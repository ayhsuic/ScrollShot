package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.engine.MockCaptureProvider
import com.example.data.model.ScrollCaptureContentPreset
import com.example.ui.components.NativeScreenshotHud
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.ScrollCaptureViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScrollCaptureStudioScreen(
    viewModel: ScrollCaptureViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val selectedPreset by viewModel.selectedPreset.collectAsState()
    val isAutoScrolling by viewModel.isAutoScrolling.collectAsState()
    val scrollProgress by viewModel.scrollProgress.collectAsState()
    val nativeHudVisible by viewModel.nativeHudVisible.collectAsState()
    val workingBitmap by viewModel.workingBitmap.collectAsState()

    var fullPresetBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Load full preset image for simulation
    LaunchedEffect(selectedPreset) {
        fullPresetBitmap = MockCaptureProvider.generatePresetBitmap(selectedPreset)
    }

    // Glowing animation for active scanner line
    val infiniteTransition = rememberInfiniteTransition(label = "scanner")
    val scannerGlow by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scannerGlow"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "原生滚动截屏工坊",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "模拟系统级 ScrollCaptureCallback",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.HOME) },
                        modifier = Modifier.testTag("btn_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.resetScrollStudio() },
                        modifier = Modifier.testTag("btn_reset_studio")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "重置")
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
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Status & Height readout
                    val currentPixelHeight = ((fullPresetBitmap?.height ?: 4000) * scrollProgress).toInt()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isAutoScrolling) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(8.dp)
                            ) {}
                            Text(
                                text = if (isAutoScrolling) "正在平滑自动向下抓取..." else "就绪 / 已捕获长图",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "当前高度: $currentPixelHeight px (${String.format("%.1f", scrollProgress * 4.2)} 屏)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Linear progress indicator
                    LinearProgressIndicator(
                        progress = { scrollProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    // Control Buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (!isAutoScrolling) {
                            Button(
                                onClick = {
                                    viewModel.startAutoScrollCapture()
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("btn_start_scroll_capture")
                            ) {
                                Icon(Icons.Default.KeyboardDoubleArrowDown, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("开始滚动长截屏", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = {
                                    viewModel.stopAutoScrollCapture()
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("btn_stop_scroll_capture")
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("完成并停止截屏", fontWeight = FontWeight.Bold)
                            }
                        }

                        // Go to Editor Button
                        FilledTonalButton(
                            onClick = {
                                if (workingBitmap == null && fullPresetBitmap != null) {
                                    viewModel.stopAutoScrollCapture()
                                }
                                viewModel.navigateTo(AppScreen.SCREENSHOT_EDITOR)
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("btn_open_editor")
                        ) {
                            Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("编辑长图")
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Preset Scenario Selector Chips
                Text(
                    text = "选择截屏场景预设：",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(ScrollCaptureContentPreset.values()) { preset ->
                        FilterChip(
                            selected = (selectedPreset == preset),
                            onClick = { viewModel.selectPreset(preset) },
                            label = { Text(preset.title) },
                            leadingIcon = {
                                Icon(
                                    imageVector = when (preset) {
                                        ScrollCaptureContentPreset.ARTICLE -> Icons.Default.Article
                                        ScrollCaptureContentPreset.CHAT -> Icons.Default.ChatBubble
                                        ScrollCaptureContentPreset.SOCIAL_FEED -> Icons.Default.DynamicFeed
                                        ScrollCaptureContentPreset.CUSTOM_WEB -> Icons.Default.Language
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // Phone Screen Viewport Canvas
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(3.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
                ) {
                    if (fullPresetBitmap != null) {
                        // The long content image
                        Image(
                            bitmap = fullPresetBitmap!!.asImageBitmap(),
                            contentDescription = "滚动长截屏实时预览",
                            contentScale = ContentScale.FillWidth,
                            alignment = Alignment.TopCenter,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Top Mask: Already captured portion has a subtle highlight indicator
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(scrollProgress)
                                .background(Color(0xFF38BDF8).copy(alpha = 0.08f))
                        )

                        // The Glowing Animated Scanner / Stitch Seam Line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(scrollProgress)
                        ) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color.Transparent,
                                                Color(0xFF38BDF8).copy(alpha = scannerGlow),
                                                Color(0xFF818CF8).copy(alpha = scannerGlow),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )
                        }

                        // Floating Height Tag on the right edge
                        Surface(
                            shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp),
                            color = Color(0xFF0F172A).copy(alpha = 0.85f),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = (scrollProgress * 280).dp.coerceAtLeast(16.dp))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Height,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "${((fullPresetBitmap?.height ?: 4000) * scrollProgress).toInt()} px",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Android Native Bottom-Left Floating Screenshot HUD
            NativeScreenshotHud(
                visible = nativeHudVisible,
                thumbnailBitmap = workingBitmap ?: fullPresetBitmap,
                onCaptureMoreClick = {
                    viewModel.startAutoScrollCapture()
                },
                onEditClick = {
                    viewModel.dismissNativeHud()
                    viewModel.navigateTo(AppScreen.SCREENSHOT_EDITOR)
                },
                onShareClick = {
                    viewModel.saveCurrentScreenshot("长截屏_${System.currentTimeMillis()}") { id ->
                        viewModel.savedScreenshots.value.find { it.id == id }?.let { entity ->
                            shareScreenshot(context, entity)
                        }
                    }
                },
                onDismiss = {
                    viewModel.dismissNativeHud()
                },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 12.dp)
            )
        }
    }
}
