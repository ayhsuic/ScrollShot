package com.example.data.engine

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toArgb
import com.example.data.model.AnnotationItem
import com.example.data.model.DeviceMockupConfig
import com.example.data.model.ImageSlice
import com.example.data.model.MockupBackground
import com.example.data.model.StitchOrientation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object ImageStitchEngine {

    /**
     * Stitches multiple slices either vertically or horizontally with custom offsets and crop margins
     */
    suspend fun stitchImages(
        slices: List<ImageSlice>,
        orientation: StitchOrientation = StitchOrientation.VERTICAL
    ): Bitmap = withContext(Dispatchers.Default) {
        if (slices.isEmpty()) {
            return@withContext Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        }
        if (slices.size == 1) {
            val s = slices[0]
            return@withContext cropSlice(s.bitmap, s.topCropRatio, s.bottomCropRatio)
        }

        val processedBitmaps = slices.map { slice ->
            cropSlice(slice.bitmap, slice.topCropRatio, slice.bottomCropRatio)
        }

        if (orientation == StitchOrientation.VERTICAL) {
            // Find max width among all slices to align them
            val targetWidth = processedBitmaps.maxOf { it.width }

            // Calculate total height considering overlap offset
            var totalHeight = 0
            for (i in processedBitmaps.indices) {
                val bmp = processedBitmaps[i]
                val scaledHeight = (bmp.height.toFloat() * (targetWidth.toFloat() / bmp.width)).toInt()
                val overlap = if (i > 0) slices[i].overlapOffsetPixels else 0
                val effectiveHeight = max(1, scaledHeight - overlap)
                totalHeight += effectiveHeight
            }

            val resultBitmap = Bitmap.createBitmap(targetWidth, max(1, totalHeight), Bitmap.Config.ARGB_8888)
            val canvas = Canvas(resultBitmap)
            canvas.drawColor(Color.WHITE)

            var currentY = 0f
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

            for (i in processedBitmaps.indices) {
                val bmp = processedBitmaps[i]
                val scaledHeight = (bmp.height.toFloat() * (targetWidth.toFloat() / bmp.width)).toInt()
                val overlap = if (i > 0) slices[i].overlapOffsetPixels.toFloat() else 0f

                val destRect = RectF(0f, currentY - overlap, targetWidth.toFloat(), currentY - overlap + scaledHeight)
                canvas.drawBitmap(bmp, null, destRect, paint)

                currentY += (scaledHeight - overlap)
            }

            resultBitmap
        } else {
            // Horizontal Stitching
            val targetHeight = processedBitmaps.maxOf { it.height }
            var totalWidth = 0
            for (i in processedBitmaps.indices) {
                val bmp = processedBitmaps[i]
                val scaledWidth = (bmp.width.toFloat() * (targetHeight.toFloat() / bmp.height)).toInt()
                val overlap = if (i > 0) slices[i].overlapOffsetPixels else 0
                val effectiveWidth = max(1, scaledWidth - overlap)
                totalWidth += effectiveWidth
            }

            val resultBitmap = Bitmap.createBitmap(max(1, totalWidth), targetHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(resultBitmap)
            canvas.drawColor(Color.WHITE)

            var currentX = 0f
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

            for (i in processedBitmaps.indices) {
                val bmp = processedBitmaps[i]
                val scaledWidth = (bmp.width.toFloat() * (targetHeight.toFloat() / bmp.height)).toInt()
                val overlap = if (i > 0) slices[i].overlapOffsetPixels.toFloat() else 0f

                val destRect = RectF(currentX - overlap, 0f, currentX - overlap + scaledWidth, targetHeight.toFloat())
                canvas.drawBitmap(bmp, null, destRect, paint)

                currentX += (scaledWidth - overlap)
            }

            resultBitmap
        }
    }

    private fun cropSlice(bitmap: Bitmap, topRatio: Float, bottomRatio: Float): Bitmap {
        val top = (bitmap.height * topRatio).toInt().coerceIn(0, bitmap.height - 1)
        val bottom = (bitmap.height * (1f - bottomRatio)).toInt().coerceIn(top + 1, bitmap.height)
        val height = bottom - top
        return Bitmap.createBitmap(bitmap, 0, top, bitmap.width, height)
    }

    /**
     * Smart Overlap Detection:
     * Compares vertical scan lines near the bottom of topBitmap and top of bottomBitmap
     * to automatically identify the optimal overlapping seam offset.
     */
    suspend fun detectSmartOverlap(topBitmap: Bitmap, bottomBitmap: Bitmap): Int = withContext(Dispatchers.Default) {
        val searchHeight = min(topBitmap.height / 2, min(bottomBitmap.height / 2, 400))
        if (searchHeight <= 10) return@withContext 0

        val sampleWidth = min(topBitmap.width, bottomBitmap.width)
        val stepX = max(1, sampleWidth / 40) // sample 40 columns across width

        var bestOffset = 0
        var minDiff = Double.MAX_VALUE

        val topH = topBitmap.height
        val minOverlap = 30
        val maxOverlap = searchHeight

        for (offset in minOverlap..maxOverlap step 2) {
            var diffSum = 0.0
            var sampleCount = 0

            // Check matching rows
            for (dy in 0 until min(offset, 60) step 4) {
                val topY = topH - offset + dy
                val bottomY = dy
                if (topY < 0 || topY >= topH || bottomY >= bottomBitmap.height) continue

                for (x in 0 until sampleWidth step stepX) {
                    val pixelA = topBitmap.getPixel(x, topY)
                    val pixelB = bottomBitmap.getPixel(x, bottomY)

                    val rDiff = abs(Color.red(pixelA) - Color.red(pixelB))
                    val gDiff = abs(Color.green(pixelA) - Color.green(pixelB))
                    val bDiff = abs(Color.blue(pixelA) - Color.blue(pixelB))

                    diffSum += (rDiff + gDiff + bDiff)
                    sampleCount++
                }
            }

            if (sampleCount > 0) {
                val avgDiff = diffSum / sampleCount
                if (avgDiff < minDiff) {
                    minDiff = avgDiff
                    bestOffset = offset
                }
            }
        }

        // Only accept if confidence is good (low average color difference)
        if (minDiff < 45.0) {
            bestOffset
        } else {
            0
        }
    }

    /**
     * Applies privacy pixelation mosaic onto a region of the bitmap
     */
    fun applyMosaicToBitmap(source: Bitmap, rect: Rect, blockSize: Int = 18): Bitmap {
        val mutableBmp = if (source.isMutable) source else source.copy(Bitmap.Config.ARGB_8888, true)
        val left = rect.left.toInt().coerceIn(0, mutableBmp.width - 1)
        val top = rect.top.toInt().coerceIn(0, mutableBmp.height - 1)
        val right = rect.right.toInt().coerceIn(left + 1, mutableBmp.width)
        val bottom = rect.bottom.toInt().coerceIn(top + 1, mutableBmp.height)

        val canvas = Canvas(mutableBmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        for (y in top until bottom step blockSize) {
            for (x in left until right step blockSize) {
                val bw = min(blockSize, right - x)
                val bh = min(blockSize, bottom - y)
                val centerX = (x + bw / 2).coerceIn(0, mutableBmp.width - 1)
                val centerY = (y + bh / 2).coerceIn(0, mutableBmp.height - 1)
                val sampleColor = mutableBmp.getPixel(centerX, centerY)

                paint.color = sampleColor
                canvas.drawRect(x.toFloat(), y.toFloat(), (x + bw).toFloat(), (y + bh).toFloat(), paint)
            }
        }
        return mutableBmp
    }

    /**
     * Render all markup (Pens, Highlighters, Mosaics, Shapes, Arrows, Texts) onto final bitmap
     */
    suspend fun renderMarkupOnBitmap(
        baseBitmap: Bitmap,
        annotations: List<AnnotationItem>,
        cropRect: Rect? = null
    ): Bitmap = withContext(Dispatchers.Default) {
        val workingBmp = baseBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(workingBmp)

        // Process Mosaics first
        for (item in annotations) {
            if (item is AnnotationItem.MosaicItem) {
                applyMosaicToBitmap(workingBmp, item.rect, 20)
            }
        }

        // Process Paths, Shapes, Arrows, Texts
        for (item in annotations) {
            when (item) {
                is AnnotationItem.PathItem -> {
                    val paint = Paint().apply {
                        isAntiAlias = true
                        strokeWidth = item.strokeWidth
                        style = Paint.Style.STROKE
                        strokeCap = Paint.Cap.ROUND
                        strokeJoin = Paint.Join.ROUND
                        if (item.isHighlighter) {
                            color = item.color.copy(alpha = 0.45f).toArgb()
                            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
                        } else {
                            color = item.color.toArgb()
                        }
                    }
                    if (item.points.size > 1) {
                        val path = Path()
                        path.moveTo(item.points[0].x, item.points[0].y)
                        for (p in item.points.drop(1)) {
                            path.lineTo(p.x, p.y)
                        }
                        canvas.drawPath(path, paint)
                    }
                }
                is AnnotationItem.ShapeItem -> {
                    val paint = Paint().apply {
                        isAntiAlias = true
                        color = item.color.toArgb()
                        strokeWidth = item.strokeWidth
                        style = if (item.isFilled) Paint.Style.FILL else Paint.Style.STROKE
                        strokeCap = Paint.Cap.ROUND
                    }
                    if (item.isArrow) {
                        drawArrow(canvas, item.start, item.end, paint)
                    } else {
                        val left = min(item.start.x, item.end.x)
                        val top = min(item.start.y, item.end.y)
                        val right = max(item.start.x, item.end.x)
                        val bottom = max(item.start.y, item.end.y)
                        canvas.drawRoundRect(RectF(left, top, right, bottom), 16f, 16f, paint)
                    }
                }
                is AnnotationItem.TextItem -> {
                    val textPaint = Paint().apply {
                        isAntiAlias = true
                        color = item.color.toArgb()
                        textSize = item.fontSize
                        typeface = Typeface.DEFAULT_BOLD
                    }
                    val bgPaint = Paint().apply {
                        isAntiAlias = true
                        color = item.backgroundColor.toArgb()
                        style = Paint.Style.FILL
                    }
                    val textBounds = android.graphics.Rect()
                    textPaint.getTextBounds(item.text, 0, item.text.length, textBounds)
                    val pad = 12f
                    val bgRect = RectF(
                        item.position.x - pad,
                        item.position.y - textBounds.height() - pad,
                        item.position.x + textBounds.width() + pad,
                        item.position.y + pad
                    )
                    canvas.drawRoundRect(bgRect, 10f, 10f, bgPaint)
                    canvas.drawText(item.text, item.position.x, item.position.y, textPaint)
                }
                is AnnotationItem.MosaicItem -> {
                    // Already processed
                }
            }
        }

        // Apply crop if requested
        if (cropRect != null) {
            val left = cropRect.left.toInt().coerceIn(0, workingBmp.width - 1)
            val top = cropRect.top.toInt().coerceIn(0, workingBmp.height - 1)
            val width = cropRect.width.toInt().coerceIn(1, workingBmp.width - left)
            val height = cropRect.height.toInt().coerceIn(1, workingBmp.height - top)
            Bitmap.createBitmap(workingBmp, left, top, width, height)
        } else {
            workingBmp
        }
    }

    private fun drawArrow(canvas: Canvas, start: Offset, end: Offset, paint: Paint) {
        canvas.drawLine(start.x, start.y, end.x, end.y, paint)
        val angle = Math.atan2((end.y - start.y).toDouble(), (end.x - start.x).toDouble())
        val arrowHeadLength = max(30.0, paint.strokeWidth * 3.5)
        val arrowHeadAngle = Math.toRadians(35.0)

        val x1 = (end.x - arrowHeadLength * Math.cos(angle - arrowHeadAngle)).toFloat()
        val y1 = (end.y - arrowHeadLength * Math.sin(angle - arrowHeadAngle)).toFloat()
        val x2 = (end.x - arrowHeadLength * Math.cos(angle + arrowHeadAngle)).toFloat()
        val y2 = (end.y - arrowHeadLength * Math.sin(angle + arrowHeadAngle)).toFloat()

        val headPath = Path().apply {
            moveTo(end.x, end.y)
            lineTo(x1, y1)
            lineTo(x2, y2)
            close()
        }
        val headPaint = Paint(paint).apply {
            style = Paint.Style.FILL
        }
        canvas.drawPath(headPath, headPaint)
    }

    /**
     * Render device frame wrapper mockup
     */
    suspend fun renderDeviceMockup(
        sourceBitmap: Bitmap,
        config: DeviceMockupConfig
    ): Bitmap = withContext(Dispatchers.Default) {
        if (!config.enabled && config.background == MockupBackground.NONE) {
            return@withContext sourceBitmap
        }

        val padding = config.paddingDp * 3 // scale padding to bitmap resolution
        val outWidth = sourceBitmap.width + padding * 2
        val outHeight = sourceBitmap.height + padding * 2

        val resultBitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)

        // Draw Background
        if (config.background != MockupBackground.NONE && config.background.colors.isNotEmpty()) {
            val colorsInt = config.background.colors.map { it.toArgb() }.toIntArray()
            val gradient = if (colorsInt.size >= 2) {
                LinearGradient(
                    0f, 0f, outWidth.toFloat(), outHeight.toFloat(),
                    colorsInt, null, Shader.TileMode.CLAMP
                )
            } else {
                null
            }
            val bgPaint = Paint().apply {
                isAntiAlias = true
                if (gradient != null) {
                    shader = gradient
                } else {
                    color = colorsInt.firstOrNull() ?: android.graphics.Color.TRANSPARENT
                }
            }
            canvas.drawRect(0f, 0f, outWidth.toFloat(), outHeight.toFloat(), bgPaint)
        }

        val screenRect = RectF(
            padding.toFloat(),
            padding.toFloat(),
            (padding + sourceBitmap.width).toFloat(),
            (padding + sourceBitmap.height).toFloat()
        )

        // Draw Shadow
        val shadowPaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.argb(80, 0, 0, 0)
            maskFilter = BlurMaskFilter(config.shadowRadius * 2f, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawRoundRect(screenRect, config.cornerRadius, config.cornerRadius, shadowPaint)

        // Draw Rounded Screen Bitmap
        val roundedBmp = Bitmap.createBitmap(sourceBitmap.width, sourceBitmap.height, Bitmap.Config.ARGB_8888)
        val rCanvas = Canvas(roundedBmp)
        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE }
        val rRect = RectF(0f, 0f, sourceBitmap.width.toFloat(), sourceBitmap.height.toFloat())
        rCanvas.drawRoundRect(rRect, config.cornerRadius, config.cornerRadius, maskPaint)

        val srcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        }
        rCanvas.drawBitmap(sourceBitmap, 0f, 0f, srcPaint)

        canvas.drawBitmap(roundedBmp, padding.toFloat(), padding.toFloat(), null)

        // Draw Sleek Phone Outer Bezel if enabled
        if (config.showPhoneFrame) {
            val framePaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeWidth = 6f
                color = android.graphics.Color.argb(120, 255, 255, 255)
            }
            canvas.drawRoundRect(screenRect, config.cornerRadius, config.cornerRadius, framePaint)
        }

        resultBitmap
    }

    /**
     * Saves bitmap to local app storage and MediaStore
     */
    suspend fun saveBitmap(
        context: Context,
        bitmap: Bitmap,
        title: String
    ): Pair<String, Long> = withContext(Dispatchers.IO) {
        val fileName = "ScrollShot_${System.currentTimeMillis()}.png"
        val appDir = File(context.filesDir, "screenshots")
        if (!appDir.exists()) appDir.mkdirs()

        val localFile = File(appDir, fileName)
        FileOutputStream(localFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        // Also save to MediaStore Pictures gallery
        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ScrollShot")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { stream: OutputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    context.contentResolver.update(uri, contentValues, null, null)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        Pair(localFile.absolutePath, localFile.length())
    }
}
