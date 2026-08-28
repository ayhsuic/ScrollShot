package com.example.data.engine

import android.graphics.*
import com.example.data.model.ImageSlice
import com.example.data.model.ScrollCaptureContentPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MockCaptureProvider {

    /**
     * Generates a realistic high-definition long content bitmap for scroll capture demonstration
     */
    suspend fun generatePresetBitmap(preset: ScrollCaptureContentPreset, width: Int = 1080): Bitmap = withContext(Dispatchers.Default) {
        when (preset) {
            ScrollCaptureContentPreset.ARTICLE -> generateArticleBitmap(width)
            ScrollCaptureContentPreset.CHAT -> generateChatBitmap(width)
            ScrollCaptureContentPreset.SOCIAL_FEED -> generateSocialFeedBitmap(width)
            ScrollCaptureContentPreset.CUSTOM_WEB -> generateWebpageBitmap(width, "https://developer.android.com/design", "Android 15 原生设计规范与滚动截屏框架")
        }
    }

    /**
     * Slices a long bitmap into multiple overlapping screen-sized slices (mimicking multiple manual screenshots)
     */
    fun createSampleSlices(longBitmap: Bitmap, screenHeight: Int = 1920, overlap: Int = 300): List<ImageSlice> {
        val slices = mutableListOf<ImageSlice>()
        var y = 0
        var index = 0

        while (y < longBitmap.height) {
            val h = kotlin.math.min(screenHeight, longBitmap.height - y)
            val sliceBmp = Bitmap.createBitmap(longBitmap, 0, y, longBitmap.width, h)
            slices.add(
                ImageSlice(
                    bitmap = sliceBmp,
                    overlapOffsetPixels = if (index > 0) overlap else 0
                )
            )
            y += (h - overlap)
            index++
            if (y + overlap >= longBitmap.height) break
        }
        return slices
    }

    private fun generateArticleBitmap(width: Int): Bitmap {
        val height = 4800
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.parseColor("#F8FAFC"))

        // Status Bar
        drawStatusBar(canvas, width, isDark = false)

        var curY = 160f
        val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#0F172A") }

        // Category Tag
        val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#3B82F6") }
        canvas.drawRoundRect(RectF(60f, curY, 260f, curY + 60f), 16f, 16f, tagPaint)
        val tagTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText("移动开发前沿", 85f, curY + 42f, tagTextPaint)
        curY += 100f

        // Article Title
        paintText.textSize = 62f
        paintText.typeface = Typeface.DEFAULT_BOLD
        curY = drawWrappedText(canvas, "深入解析 Android 原生 Scroll Capture 滚动截屏与图像缝合算法", 60f, curY, width - 120, paintText, 80f)
        curY += 40f

        // Author bar
        val avatarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#6366F1") }
        canvas.drawCircle(100f, curY + 40f, 40f, avatarPaint)
        val authorText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#475569")
            textSize = 32f
        }
        canvas.drawText("Google 官方工程团队 • 2026年8月 • 阅读时长 8 分钟", 160f, curY + 50f, authorText)
        curY += 120f

        // Hero Graphic Card
        val heroRect = RectF(60f, curY, (width - 60).toFloat(), curY + 500f)
        val heroPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(heroRect.left, heroRect.top, heroRect.right, heroRect.bottom,
                intArrayOf(Color.parseColor("#1E1B4B"), Color.parseColor("#3B82F6")), null, Shader.TileMode.CLAMP)
        }
        canvas.drawRoundRect(heroRect, 32f, 32f, heroPaint)
        val heroTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 48f
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText("SCROLL CAPTURE 2.0", 120f, curY + 240f, heroTextPaint)
        val heroSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#93C5FD")
            textSize = 30f
        }
        canvas.drawText("无缝长截图 API 与像素级重叠检测实现", 120f, curY + 310f, heroSubPaint)
        curY += 560f

        // Body Paragraph 1
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#334155")
            textSize = 38f
            typeface = Typeface.DEFAULT
        }
        val p1 = "滚动截屏（Scroll Capture）是现代智能手机中最具实用价值的系统级交互功能之一。从 Android 12 开始，Google 引入了标准化的 ScrollCaptureCallback 体系，允许应用无缝将多屏幕可滚动视图渲染为完整的高清长图。"
        curY = drawWrappedText(canvas, p1, 60f, curY, width - 120, bodyPaint, 64f)
        curY += 50f

        // Highlight Callout Box
        val boxRect = RectF(60f, curY, (width - 60).toFloat(), curY + 260f)
        val boxBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#EFF6FF") }
        canvas.drawRoundRect(boxRect, 24f, 24f, boxBg)
        val boxBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#3B82F6")
            strokeWidth = 10f
            style = Paint.Style.STROKE
        }
        canvas.drawLine(60f, curY + 12f, 60f, curY + 248f, boxBorder)
        val calloutPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E40AF")
            textSize = 34f
            typeface = Typeface.DEFAULT_BOLD
        }
        drawWrappedText(canvas, "核心亮点：无需 Root 权限，自动跳过固定浮动导航栏与状态栏，实现零损耗智能缝合与隐私打码。", 100f, curY + 60f, width - 200, calloutPaint, 56f)
        curY += 320f

        // Heading 2
        paintText.textSize = 50f
        canvas.drawText("1. 像素重叠与边缘对齐原理", 60f, curY, paintText)
        curY += 70f

        val p2 = "在多张连续截图中，算法通过比较第一张图片的底部与下一张图片的顶部像素行，通过均方差（MSE）或特征梯度检测出精确重叠高度（Overlap Offset）。当检测重合度高于阈值时，自动剪裁多余重叠部分并合并为单张大图。"
        curY = drawWrappedText(canvas, p2, 60f, curY, width - 120, bodyPaint, 64f)
        curY += 60f

        // Code block card
        val codeRect = RectF(60f, curY, (width - 60).toFloat(), curY + 420f)
        val codeBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#0F172A") }
        canvas.drawRoundRect(codeRect, 24f, 24f, codeBg)
        val codeText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#38BDF8")
            textSize = 32f
            typeface = Typeface.MONOSPACE
        }
        canvas.drawText("// Kotlin 图像缝合核心示例", 100f, curY + 80f, codeText)
        codeText.color = Color.parseColor("#F1F5F9")
        canvas.drawText("val stitched = ImageStitchEngine.stitchImages(", 100f, curY + 150f, codeText)
        canvas.drawText("    slices = listOf(slice1, slice2, slice3),", 100f, curY + 210f, codeText)
        canvas.drawText("    orientation = StitchOrientation.VERTICAL", 100f, curY + 270f, codeText)
        canvas.drawText(")", 100f, curY + 330f, codeText)
        curY += 480f

        // Heading 3
        canvas.drawText("2. 隐私打码与画板涂鸦体系", 60f, curY, paintText)
        curY += 70f
        val p3 = "截图生成后，原生编辑器提供像素化马赛克（Pixelate Mosaic）、高斯模糊与荧光笔批注功能。用户可一键涂抹隐藏敏感人名、电话号码或支付账单金额，确保分享安全。"
        curY = drawWrappedText(canvas, p3, 60f, curY, width - 120, bodyPaint, 64f)
        curY += 80f

        // Interactive Footer Bar
        val footerRect = RectF(60f, curY, (width - 60).toFloat(), curY + 160f)
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E2E8F0") }
        canvas.drawRoundRect(footerRect, 24f, 24f, footerPaint)
        val ftText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 32f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("—— 已阅读至文章末尾 • 支持分享与本地保存 ——", width / 2f, curY + 95f, ftText)

        return bitmap
    }

    private fun generateChatBitmap(width: Int): Bitmap {
        val height = 4400
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.parseColor("#EDEDED"))

        // Top App Bar
        val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F5F5F5") }
        canvas.drawRect(0f, 0f, width.toFloat(), 180f, barPaint)
        drawStatusBar(canvas, width, isDark = false)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#111827")
            textSize = 42f
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText("产品技术核心研发组 (8)", 120f, 130f, titlePaint)

        var curY = 240f

        // Time stamp badge
        val timeBadgeBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#D1D5DB") }
        val timeRect = RectF(width / 2f - 140f, curY, width / 2f + 140f, curY + 50f)
        canvas.drawRoundRect(timeRect, 25f, 25f, timeBadgeBg)
        val timeText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 26f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("今天 上午 10:28", width / 2f, curY + 35f, timeText)
        curY += 100f

        // Message 1 (Left - Leader)
        curY = drawChatBubble(canvas, width, curY, isMe = false, name = "Alex (项目主理人)",
            text = "大家早上好！今天我们需要把全新的长截图体验上线测试，重点测试多图缝合与马赛克隐私打码功能。", avatarColor = "#3B82F6")

        // Message 2 (Right - Me)
        curY = drawChatBubble(canvas, width, curY, isMe = true, name = "我",
            text = "收到！核心图像缝合算法已集成，支持均方差智能对齐，拼接几乎察觉不到接缝。", avatarColor = "#10B981")

        // Message 3 (Left - Designer)
        curY = drawChatBubble(canvas, width, curY, isMe = false, name = "Emma (视觉设计)",
            text = "UI 方面我们遵循 Material 3 与原生 Pixel 体验，添加了精致的设备外框包装（Mockup Frame）和渐变背景导出！", avatarColor = "#EC4899")

        // Message 4 (Right - Me with Image Preview)
        curY = drawChatBubbleWithCard(canvas, width, curY, isMe = true, name = "我",
            text = "太棒了，这是刚刚导出的样张效果：", cardTitle = "长截图样张预览 (1080 x 4800)", avatarColor = "#10B981")

        // Message 5 (Left - QA)
        curY = drawChatBubble(canvas, width, curY, isMe = false, name = "David (质量保障)",
            text = "自动化测试和长图多倍缩放已经跑通，内存占用非常平稳，没有 OOM 风险。", avatarColor = "#8B5CF6")

        // Message 6 (Left - Leader)
        curY = drawChatBubble(canvas, width, curY, isMe = false, name = "Alex (项目主理人)",
            text = "完美的进度！大家今天就可以安排灰度发布了 🚀", avatarColor = "#3B82F6")

        return bitmap
    }

    private fun generateSocialFeedBitmap(width: Int): Bitmap {
        val height = 4500
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.parseColor("#F1F5F9"))

        drawStatusBar(canvas, width, isDark = false)
        var curY = 160f

        // Feed Header
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 48f
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText("朋友圈动态与探索", 60f, curY, headerPaint)
        curY += 80f

        // Feed Card 1
        curY = drawSocialCard(canvas, width, curY,
            userName = "极客科技视界",
            time = "10 分钟前 • 来自 Pixel 9 Pro",
            content = "终于用上了原生长截图功能！无论是长微博、群聊记录还是技术文档，一键顺畅滚动捕获，再也不用手动拼接十几张屏幕截图了，体验极其丝滑！",
            likes = 328,
            comments = 64,
            accentColor = "#3B82F6")

        // Feed Card 2
        curY = drawSocialCard(canvas, width, curY,
            userName = "设计美学日记",
            time = "1 小时前",
            content = "分享一个长截图导出小技巧：开启「带壳截图」和「极光蓝紫」渐变背景，导出的长图瞬间变成高级设计作品展示，发到社交平台质感拉满 ✨",
            likes = 892,
            comments = 112,
            accentColor = "#F43F5E")

        // Feed Card 3
        curY = drawSocialCard(canvas, width, curY,
            userName = "Android 开发工坊",
            time = "3 小时前",
            content = "长截图的打码功能对于隐私保护太关键了，一键框选马赛克，聊天记录里的银行卡号和敏感信息秒级隐藏，安全感满满。",
            likes = 512,
            comments = 45,
            accentColor = "#10B981")

        return bitmap
    }

    private fun generateWebpageBitmap(width: Int, url: String, title: String): Bitmap {
        val height = 4200
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        // Web Browser Address Bar
        val barBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F8FAFC") }
        canvas.drawRect(0f, 0f, width.toFloat(), 180f, barBg)
        drawStatusBar(canvas, width, isDark = false)

        val urlBox = RectF(60f, 90f, (width - 60).toFloat(), 160f)
        val urlBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E2E8F0") }
        canvas.drawRoundRect(urlBox, 35f, 35f, urlBoxPaint)
        val urlText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#475569")
            textSize = 28f
        }
        canvas.drawText("🔒 $url", 100f, 136f, urlText)

        var curY = 260f
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 54f
            typeface = Typeface.DEFAULT_BOLD
        }
        curY = drawWrappedText(canvas, title, 60f, curY, width - 120, titlePaint, 72f)
        curY += 40f

        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#334155")
            textSize = 36f
        }
        val text1 = "网页滚动长截图允许用户一次性抓取完整的网页文章，包含所有排版、插图与表格，无需分屏多次截取。配合内置的平滑滚动扫描仪，生成超清长图。"
        curY = drawWrappedText(canvas, text1, 60f, curY, width - 120, bodyPaint, 60f)
        curY += 60f

        // Web Visual Card
        val cardRect = RectF(60f, curY, (width - 60).toFloat(), curY + 450f)
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(cardRect.left, cardRect.top, cardRect.right, cardRect.bottom,
                intArrayOf(Color.parseColor("#065F46"), Color.parseColor("#0D9488")), null, Shader.TileMode.CLAMP)
        }
        canvas.drawRoundRect(cardRect, 28f, 28f, cardPaint)
        val cardText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 44f
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText("全功能网页无损抓取引擎", 120f, curY + 220f, cardText)
        curY += 520f

        val text2 = "高保真色彩呈现，支持导出为 PNG 或 PDF 格式，方便随时离线归档或发送给同事客户审阅。"
        curY = drawWrappedText(canvas, text2, 60f, curY, width - 120, bodyPaint, 60f)

        return bitmap
    }

    private fun drawStatusBar(canvas: Canvas, width: Int, isDark: Boolean) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDark) Color.WHITE else Color.parseColor("#1E293B")
            textSize = 30f
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText("09:41", 60f, 60f, paint)
        canvas.drawText("5G  100%", width - 180f, 60f, paint)
    }

    private fun drawChatBubble(canvas: Canvas, width: Int, startY: Float, isMe: Boolean, name: String, text: String, avatarColor: String): Float {
        var y = startY
        val avatarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor(avatarColor) }
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 26f
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isMe) Color.parseColor("#0F172A") else Color.parseColor("#0F172A")
            textSize = 36f
        }

        val maxBubbleWidth = width * 0.68f
        val lines = splitTextToLines(text, textPaint, maxBubbleWidth - 60f)
        val bubbleHeight = lines.size * 52f + 50f

        if (!isMe) {
            // Left Avatar
            canvas.drawCircle(90f, y + 50f, 40f, avatarPaint)
            val initial = name.take(1)
            val initialPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 34f
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(initial, 90f, y + 62f, initialPaint)
            canvas.drawText(name, 150f, y + 25f, namePaint)

            // Bubble
            val bubbleRect = RectF(150f, y + 40f, 150f + maxBubbleWidth.coerceAtMost(calculateMaxLineWidth(lines, textPaint) + 60f), y + 40f + bubbleHeight)
            val bubbleBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
            canvas.drawRoundRect(bubbleRect, 24f, 24f, bubbleBg)

            var lineY = y + 92f
            for (line in lines) {
                canvas.drawText(line, 180f, lineY, textPaint)
                lineY += 52f
            }
            y += (bubbleHeight + 70f)
        } else {
            // Right Avatar
            canvas.drawCircle(width - 90f, y + 50f, 40f, avatarPaint)
            val initialPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 34f
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("我", width - 90f, y + 62f, initialPaint)

            val calculatedW = calculateMaxLineWidth(lines, textPaint) + 60f
            val bubbleLeft = (width - 150f - calculatedW).coerceAtLeast(width * 0.25f)
            val bubbleRect = RectF(bubbleLeft, y + 30f, width - 150f, y + 30f + bubbleHeight)
            val bubbleBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#95EC69") } // WeChat green
            canvas.drawRoundRect(bubbleRect, 24f, 24f, bubbleBg)

            var lineY = y + 82f
            for (line in lines) {
                canvas.drawText(line, bubbleLeft + 30f, lineY, textPaint)
                lineY += 52f
            }
            y += (bubbleHeight + 60f)
        }

        return y
    }

    private fun drawChatBubbleWithCard(canvas: Canvas, width: Int, startY: Float, isMe: Boolean, name: String, text: String, cardTitle: String, avatarColor: String): Float {
        var y = drawChatBubble(canvas, width, startY, isMe, name, text, avatarColor)
        // Draw image card
        val cardW = 500f
        val cardH = 260f
        val left = width - 150f - cardW
        val rect = RectF(left, y - 40f, width - 150f, y - 40f + cardH)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(rect.left, rect.top, rect.right, rect.bottom,
                intArrayOf(Color.parseColor("#3B82F6"), Color.parseColor("#6366F1")), null, Shader.TileMode.CLAMP)
        }
        canvas.drawRoundRect(rect, 20f, 20f, bgPaint)
        val txtPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText("🖼️ $cardTitle", left + 30f, y + 80f, txtPaint)
        return y + cardH + 40f
    }

    private fun drawSocialCard(canvas: Canvas, width: Int, startY: Float, userName: String, time: String, content: String, likes: Int, comments: Int, accentColor: String): Float {
        var curY = startY
        val cardRect = RectF(40f, curY, (width - 40).toFloat(), curY + 600f)
        val cardBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        canvas.drawRoundRect(cardRect, 32f, 32f, cardBg)

        // Avatar
        val avatarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor(accentColor) }
        canvas.drawCircle(100f, curY + 70f, 40f, avatarPaint)
        val initialPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 34f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(userName.take(1), 100f, curY + 82f, initialPaint)

        // User & Time
        val uPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 36f
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText(userName, 160f, curY + 65f, uPaint)
        val tPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textSize = 26f
        }
        canvas.drawText(time, 160f, curY + 105f, tPaint)

        // Content
        val cPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#334155")
            textSize = 34f
        }
        curY = drawWrappedText(canvas, content, 70f, curY + 160f, width - 140, cPaint, 52f)
        curY += 40f

        // Stats Row
        val statPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 28f
        }
        canvas.drawText("❤️ $likes 次点赞    💬 $comments 条评论    ↗️ 分享", 70f, curY + 20f, statPaint)

        return curY + 80f
    }

    private fun drawWrappedText(canvas: Canvas, text: String, x: Float, y: Float, maxWidth: Int, paint: Paint, lineHeight: Float): Float {
        val lines = splitTextToLines(text, paint, maxWidth.toFloat())
        var currentY = y
        for (line in lines) {
            canvas.drawText(line, x, currentY, paint)
            currentY += lineHeight
        }
        return currentY
    }

    private fun splitTextToLines(text: String, paint: Paint, maxWidth: Float): List<String> {
        val lines = mutableListOf<String>()
        var start = 0
        while (start < text.length) {
            val count = paint.breakText(text, start, text.length, true, maxWidth, null)
            lines.add(text.substring(start, start + count))
            start += count
        }
        return lines
    }

    private fun calculateMaxLineWidth(lines: List<String>, paint: Paint): Float {
        var maxW = 0f
        for (line in lines) {
            val w = paint.measureText(line)
            if (w > maxW) maxW = w
        }
        return maxW
    }
}
