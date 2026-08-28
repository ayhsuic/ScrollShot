package com.example.data.model

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color

data class ImageSlice(
    val id: String = java.util.UUID.randomUUID().toString(),
    val bitmap: Bitmap,
    val sourceUri: Uri? = null,
    val topCropRatio: Float = 0f,
    val bottomCropRatio: Float = 0f,
    val overlapOffsetPixels: Int = 0,
    val rotationDegrees: Int = 0
)

enum class StitchOrientation {
    VERTICAL,
    HORIZONTAL
}

enum class MarkupTool {
    NONE,
    PEN,
    HIGHLIGHTER,
    MOSAIC,
    ARROW,
    RECTANGLE,
    TEXT,
    CROP,
    MOCKUP_FRAME
}

sealed class AnnotationItem {
    data class PathItem(
        val id: String = java.util.UUID.randomUUID().toString(),
        val points: List<Offset>,
        val color: Color,
        val strokeWidth: Float,
        val isHighlighter: Boolean = false
    ) : AnnotationItem()

    data class MosaicItem(
        val id: String = java.util.UUID.randomUUID().toString(),
        val rect: Rect,
        val blurRadius: Float = 16f
    ) : AnnotationItem()

    data class ShapeItem(
        val id: String = java.util.UUID.randomUUID().toString(),
        val start: Offset,
        val end: Offset,
        val color: Color,
        val strokeWidth: Float,
        val isArrow: Boolean = false,
        val isFilled: Boolean = false
    ) : AnnotationItem()

    data class TextItem(
        val id: String = java.util.UUID.randomUUID().toString(),
        val text: String,
        val position: Offset,
        val color: Color = Color.Black,
        val fontSize: Float = 18f,
        val backgroundColor: Color = Color.White
    ) : AnnotationItem()
}

enum class MockupBackground(val displayName: String, val colors: List<Color>) {
    NONE("无背景", listOf(Color.Transparent, Color.Transparent)),
    INDIGO_TWILIGHT("极光蓝紫", listOf(Color(0xFF312E81), Color(0xFF1E1B4B))),
    SLATE_DARK("深空黑曜", listOf(Color(0xFF1E293B), Color(0xFF0F172A))),
    SUNSET_WARM("落日晨霞", listOf(Color(0xFFF97316), Color(0xFFDB2777))),
    EMERALD_MINT("薄荷翠绿", listOf(Color(0xFF059669), Color(0xFF0D9488))),
    PURE_MINIMAL("纯净浅灰", listOf(Color(0xFFF1F5F9), Color(0xFFE2E8F0))),
    GRADIENT_ROSE("柔和樱粉", listOf(Color(0xFFFDA4AF), Color(0xFFF43F5E)))
}

data class DeviceMockupConfig(
    val enabled: Boolean = false,
    val showPhoneFrame: Boolean = true,
    val showCornerRadius: Boolean = true,
    val cornerRadius: Float = 28f,
    val paddingDp: Int = 24,
    val shadowRadius: Float = 24f,
    val background: MockupBackground = MockupBackground.INDIGO_TWILIGHT
)

enum class ScrollCaptureContentPreset(val title: String, val subtitle: String, val iconRes: String) {
    ARTICLE("科技深度长文", "连贯段落、代码块与信息卡片", "article"),
    CHAT("即时通讯长会话", "多人微信/原生短信对话气泡与图片", "chat"),
    SOCIAL_FEED("社交动态长瀑布流", "朋友圈/动态卡片、多图与评论互动", "social"),
    CUSTOM_WEB("网页实时长截屏", "自定义输入URL进行全页面长截屏", "web")
}
