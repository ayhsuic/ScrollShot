package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.ScrollCaptureViewModel
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenshotEditorScreen(
    viewModel: ScrollCaptureViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val workingBitmap by viewModel.workingBitmap.collectAsState()
    val activeTool by viewModel.activeMarkupTool.collectAsState()
    val activeColor by viewModel.activeBrushColor.collectAsState()
    val activeStrokeWidth by viewModel.activeStrokeWidth.collectAsState()
    val annotations by viewModel.annotations.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val mockupConfig by viewModel.deviceMockupConfig.collectAsState()

    // Temporary drawing state during touch drag
    var currentDragPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var shapeDragStart by remember { mutableStateOf<Offset?>(null) }
    var shapeDragEnd by remember { mutableStateOf<Offset?>(null) }
    var showTextInputDialog by remember { mutableStateOf(false) }
    var textInputPosition by remember { mutableStateOf(Offset.Zero) }
    var pendingText by remember { mutableStateOf("") }

    val presetColors = listOf(
        Color(0xFFEF4444), // Red
        Color(0xFF3B82F6), // Blue
        Color(0xFF10B981), // Green
        Color(0xFFF59E0B), // Yellow
        Color(0xFF8B5CF6), // Purple
        Color(0xFFEC4899), // Pink
        Color(0xFF1E293B), // Slate Black
        Color(0xFFFFFFFF)  // White
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "截屏涂鸦与编辑",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.HOME) },
                        modifier = Modifier.testTag("btn_editor_back")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // Undo
                    IconButton(
                        onClick = { viewModel.undoMarkup() },
                        enabled = canUndo,
                        modifier = Modifier.testTag("btn_undo")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Undo,
                            contentDescription = "撤销",
                            tint = if (canUndo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }

                    // Clear
                    IconButton(
                        onClick = { viewModel.clearAllMarkup() },
                        modifier = Modifier.testTag("btn_clear_markup")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteSweep,
                            contentDescription = "清除所有批注"
                        )
                    }

                    // Save / Export
                    FilledTonalButton(
                        onClick = {
                            viewModel.saveCurrentScreenshot("长截图_${System.currentTimeMillis()}") { id ->
                                viewModel.navigateTo(AppScreen.HOME)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("btn_save_screenshot")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("保存")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 12.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Secondary Options Strip based on Active Tool
                    when (activeTool) {
                        MarkupTool.PEN, MarkupTool.HIGHLIGHTER, MarkupTool.ARROW, MarkupTool.RECTANGLE, MarkupTool.TEXT -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Color palette
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    items(presetColors) { color ->
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(color)
                                                .border(
                                                    width = if (activeColor == color) 3.dp else 1.dp,
                                                    color = if (activeColor == color) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                                                    shape = CircleShape
                                                )
                                                .clickable { viewModel.setBrushColor(color) }
                                        )
                                    }
                                }

                                // Stroke width options
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { viewModel.setStrokeWidth(6f) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(modifier = Modifier.size(6.dp).background(MaterialTheme.colorScheme.onSurface, CircleShape))
                                    }
                                    IconButton(
                                        onClick = { viewModel.setStrokeWidth(14f) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(modifier = Modifier.size(12.dp).background(MaterialTheme.colorScheme.onSurface, CircleShape))
                                    }
                                    IconButton(
                                        onClick = { viewModel.setStrokeWidth(24f) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(modifier = Modifier.size(18.dp).background(MaterialTheme.colorScheme.onSurface, CircleShape))
                                    }
                                }
                            }
                        }
                        MarkupTool.MOCKUP_FRAME -> {
                            // Mockup Background and Frame Controls
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("样机背景风格：", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(MockupBackground.values()) { bg ->
                                        FilterChip(
                                            selected = (mockupConfig.background == bg),
                                            onClick = {
                                                viewModel.updateMockupConfig(mockupConfig.copy(background = bg, enabled = bg != MockupBackground.NONE))
                                            },
                                            label = { Text(bg.displayName, fontSize = 11.sp) },
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                    }
                                }
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Checkbox(
                                            checked = mockupConfig.showPhoneFrame,
                                            onCheckedChange = {
                                                viewModel.updateMockupConfig(mockupConfig.copy(showPhoneFrame = it))
                                            }
                                        )
                                        Text("手机外边框", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Checkbox(
                                            checked = mockupConfig.showCornerRadius,
                                            onCheckedChange = {
                                                viewModel.updateMockupConfig(mockupConfig.copy(showCornerRadius = it))
                                            }
                                        )
                                        Text("圆角与悬浮阴影", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                        else -> {}
                    }

                    // Main Tools Dock Tabs
                    ScrollableTabRow(
                        selectedTabIndex = activeTool.ordinal,
                        edgePadding = 0.dp,
                        divider = {},
                        indicator = {},
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ToolTabItem(
                            icon = Icons.Default.PanTool,
                            label = "浏览",
                            isSelected = (activeTool == MarkupTool.NONE),
                            onClick = { viewModel.setMarkupTool(MarkupTool.NONE) },
                            tag = "tab_browse"
                        )
                        ToolTabItem(
                            icon = Icons.Default.Draw,
                            label = "画笔",
                            isSelected = (activeTool == MarkupTool.PEN),
                            onClick = { viewModel.setMarkupTool(MarkupTool.PEN) },
                            tag = "tab_pen"
                        )
                        ToolTabItem(
                            icon = Icons.Default.BorderColor,
                            label = "荧光笔",
                            isSelected = (activeTool == MarkupTool.HIGHLIGHTER),
                            onClick = { viewModel.setMarkupTool(MarkupTool.HIGHLIGHTER) },
                            tag = "tab_highlighter"
                        )
                        ToolTabItem(
                            icon = Icons.Default.Grain,
                            label = "马赛克",
                            isSelected = (activeTool == MarkupTool.MOSAIC),
                            onClick = { viewModel.setMarkupTool(MarkupTool.MOSAIC) },
                            tag = "tab_mosaic"
                        )
                        ToolTabItem(
                            icon = Icons.Default.ArrowOutward,
                            label = "箭头/框",
                            isSelected = (activeTool == MarkupTool.ARROW || activeTool == MarkupTool.RECTANGLE),
                            onClick = {
                                viewModel.setMarkupTool(if (activeTool == MarkupTool.ARROW) MarkupTool.RECTANGLE else MarkupTool.ARROW)
                            },
                            tag = "tab_shape"
                        )
                        ToolTabItem(
                            icon = Icons.Default.TextFields,
                            label = "文字",
                            isSelected = (activeTool == MarkupTool.TEXT),
                            onClick = { viewModel.setMarkupTool(MarkupTool.TEXT) },
                            tag = "tab_text"
                        )
                        ToolTabItem(
                            icon = Icons.Default.Smartphone,
                            label = "带壳样机",
                            isSelected = (activeTool == MarkupTool.MOCKUP_FRAME),
                            onClick = {
                                viewModel.setMarkupTool(MarkupTool.MOCKUP_FRAME)
                                viewModel.updateMockupConfig(mockupConfig.copy(enabled = true))
                            },
                            tag = "tab_mockup"
                        )
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        val scrollState = rememberScrollState()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    if (mockupConfig.enabled && mockupConfig.background != MockupBackground.NONE) {
                        Brush.linearGradient(mockupConfig.background.colors)
                    } else {
                        Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF1E293B)))
                    }
                )
        ) {
            if (workingBitmap != null) {
                val bmp = workingBitmap!!
                // Vertical Scrollable viewport for high-res long screenshots
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(
                            state = scrollState,
                            enabled = (activeTool == MarkupTool.NONE || activeTool == MarkupTool.MOCKUP_FRAME)
                        )
                        .padding(if (mockupConfig.enabled) 24.dp else 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (mockupConfig.enabled && mockupConfig.showCornerRadius) {
                                    Modifier
                                        .shadow(16.dp, RoundedCornerShape(24.dp))
                                        .clip(RoundedCornerShape(24.dp))
                                        .then(
                                            if (mockupConfig.showPhoneFrame) {
                                                Modifier.border(3.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                                            } else Modifier
                                        )
                                } else {
                                    Modifier.clip(RoundedCornerShape(8.dp))
                                }
                            )
                            .pointerInput(activeTool, activeColor, activeStrokeWidth) {
                                when (activeTool) {
                                    MarkupTool.PEN, MarkupTool.HIGHLIGHTER -> {
                                        detectDragGestures(
                                            onDragStart = { offset ->
                                                currentDragPoints = listOf(offset)
                                            },
                                            onDrag = { change, _ ->
                                                change.consume()
                                                currentDragPoints = currentDragPoints + change.position
                                            },
                                            onDragEnd = {
                                                if (currentDragPoints.size > 1) {
                                                    viewModel.addAnnotation(
                                                        AnnotationItem.PathItem(
                                                            points = currentDragPoints,
                                                            color = activeColor,
                                                            strokeWidth = activeStrokeWidth,
                                                            isHighlighter = (activeTool == MarkupTool.HIGHLIGHTER)
                                                        )
                                                    )
                                                }
                                                currentDragPoints = emptyList()
                                            },
                                            onDragCancel = {
                                                currentDragPoints = emptyList()
                                            }
                                        )
                                    }
                                    MarkupTool.MOSAIC, MarkupTool.ARROW, MarkupTool.RECTANGLE -> {
                                        detectDragGestures(
                                            onDragStart = { offset ->
                                                shapeDragStart = offset
                                                shapeDragEnd = offset
                                            },
                                            onDrag = { change, _ ->
                                                change.consume()
                                                shapeDragEnd = change.position
                                            },
                                            onDragEnd = {
                                                val start = shapeDragStart
                                                val end = shapeDragEnd
                                                if (start != null && end != null) {
                                                    if (activeTool == MarkupTool.MOSAIC) {
                                                        val rect = Rect(
                                                            left = min(start.x, end.x),
                                                            top = min(start.y, end.y),
                                                            right = max(start.x, end.x),
                                                            bottom = max(start.y, end.y)
                                                        )
                                                        if (rect.width > 10 && rect.height > 10) {
                                                            viewModel.addAnnotation(AnnotationItem.MosaicItem(rect = rect))
                                                        }
                                                    } else if (activeTool == MarkupTool.ARROW) {
                                                        viewModel.addAnnotation(
                                                            AnnotationItem.ShapeItem(
                                                                start = start,
                                                                end = end,
                                                                color = activeColor,
                                                                strokeWidth = activeStrokeWidth,
                                                                isArrow = true
                                                            )
                                                        )
                                                    } else if (activeTool == MarkupTool.RECTANGLE) {
                                                        viewModel.addAnnotation(
                                                            AnnotationItem.ShapeItem(
                                                                start = start,
                                                                end = end,
                                                                color = activeColor,
                                                                strokeWidth = activeStrokeWidth,
                                                                isArrow = false
                                                            )
                                                        )
                                                    }
                                                }
                                                shapeDragStart = null
                                                shapeDragEnd = null
                                            },
                                            onDragCancel = {
                                                shapeDragStart = null
                                                shapeDragEnd = null
                                            }
                                        )
                                    }
                                    MarkupTool.TEXT -> {
                                        detectTapGestures { offset ->
                                            textInputPosition = offset
                                            pendingText = ""
                                            showTextInputDialog = true
                                        }
                                    }
                                    else -> {}
                                }
                            }
                    ) {
                        // The base stitched screenshot bitmap
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "长截图画板",
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Markup Canvas overlay
                        Canvas(modifier = Modifier.matchParentSize()) {
                            // 1. Draw existing annotations
                            for (item in annotations) {
                                when (item) {
                                    is AnnotationItem.PathItem -> {
                                        if (item.points.size > 1) {
                                            val path = Path().apply {
                                                moveTo(item.points[0].x, item.points[0].y)
                                                for (p in item.points.drop(1)) {
                                                    lineTo(p.x, p.y)
                                                }
                                            }
                                            drawPath(
                                                path = path,
                                                color = if (item.isHighlighter) item.color.copy(alpha = 0.45f) else item.color,
                                                style = Stroke(
                                                    width = item.strokeWidth,
                                                    cap = StrokeCap.Round,
                                                    join = StrokeJoin.Round
                                                )
                                            )
                                        }
                                    }
                                    is AnnotationItem.MosaicItem -> {
                                        // Mosaic visual box indicator
                                        drawRect(
                                            color = Color.Black.copy(alpha = 0.65f),
                                            topLeft = item.rect.topLeft,
                                            size = item.rect.size
                                        )
                                        drawRect(
                                            color = Color.White,
                                            topLeft = item.rect.topLeft,
                                            size = item.rect.size,
                                            style = Stroke(width = 2.dp.toPx())
                                        )
                                    }
                                    is AnnotationItem.ShapeItem -> {
                                        if (item.isArrow) {
                                            drawLine(
                                                color = item.color,
                                                start = item.start,
                                                end = item.end,
                                                strokeWidth = item.strokeWidth,
                                                cap = StrokeCap.Round
                                            )
                                        } else {
                                            val left = min(item.start.x, item.end.x)
                                            val top = min(item.start.y, item.end.y)
                                            val width = max(item.start.x, item.end.x) - left
                                            val height = max(item.start.y, item.end.y) - top
                                            drawRoundRect(
                                                color = item.color,
                                                topLeft = Offset(left, top),
                                                size = androidx.compose.ui.geometry.Size(width, height),
                                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f),
                                                style = Stroke(width = item.strokeWidth)
                                            )
                                        }
                                    }
                                    is AnnotationItem.TextItem -> {
                                        // Simple preview badge
                                    }
                                }
                            }

                            // 2. Draw active live drag feedback
                            if (currentDragPoints.size > 1) {
                                val path = Path().apply {
                                    moveTo(currentDragPoints[0].x, currentDragPoints[0].y)
                                    for (p in currentDragPoints.drop(1)) {
                                        lineTo(p.x, p.y)
                                    }
                                }
                                drawPath(
                                    path = path,
                                    color = if (activeTool == MarkupTool.HIGHLIGHTER) activeColor.copy(alpha = 0.45f) else activeColor,
                                    style = Stroke(
                                        width = activeStrokeWidth,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }

                            if (shapeDragStart != null && shapeDragEnd != null) {
                                val s = shapeDragStart!!
                                val e = shapeDragEnd!!
                                if (activeTool == MarkupTool.MOSAIC) {
                                    val left = min(s.x, e.x)
                                    val top = min(s.y, e.y)
                                    val w = max(s.x, e.x) - left
                                    val h = max(s.y, e.y) - top
                                    drawRect(
                                        color = Color.Black.copy(alpha = 0.5f),
                                        topLeft = Offset(left, top),
                                        size = androidx.compose.ui.geometry.Size(w, h)
                                    )
                                } else if (activeTool == MarkupTool.ARROW) {
                                    drawLine(
                                        color = activeColor,
                                        start = s,
                                        end = e,
                                        strokeWidth = activeStrokeWidth,
                                        cap = StrokeCap.Round
                                    )
                                } else if (activeTool == MarkupTool.RECTANGLE) {
                                    val left = min(s.x, e.x)
                                    val top = min(s.y, e.y)
                                    val w = max(s.x, e.x) - left
                                    val h = max(s.y, e.y) - top
                                    drawRoundRect(
                                        color = activeColor,
                                        topLeft = Offset(left, top),
                                        size = androidx.compose.ui.geometry.Size(w, h),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f),
                                        style = Stroke(width = activeStrokeWidth)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text("请先抓取或拼接长截图", color = Color.White)
                }
            }
        }
    }

    // Text Annotation Dialog
    if (showTextInputDialog) {
        AlertDialog(
            onDismissRequest = { showTextInputDialog = false },
            title = { Text("添加文字批注") },
            text = {
                OutlinedTextField(
                    value = pendingText,
                    onValueChange = { pendingText = it },
                    placeholder = { Text("输入批注文字...") },
                    modifier = Modifier.fillMaxWidth().testTag("input_annotation_text")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pendingText.isNotBlank()) {
                            viewModel.addAnnotation(
                                AnnotationItem.TextItem(
                                    text = pendingText,
                                    position = textInputPosition,
                                    color = activeColor,
                                    fontSize = 42f
                                )
                            )
                        }
                        showTextInputDialog = false
                    },
                    modifier = Modifier.testTag("btn_confirm_text")
                ) {
                    Text("确定添加")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTextInputDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun ToolTabItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        modifier = Modifier
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag(tag)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
