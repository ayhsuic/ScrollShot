package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.data.db.ScreenshotEntity
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.ScrollCaptureViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ScrollCaptureViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val savedScreenshots by viewModel.savedScreenshots.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    // Multiple photo picker launcher for stitching
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(10)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.addImagesFromUris(context, uris)
            viewModel.navigateTo(AppScreen.IMAGE_STITCHER)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Screenshot,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .fillMaxSize()
                            )
                        }
                        Text(
                            text = "长截图",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        SuggestionChip(
                            onClick = {},
                            label = { Text("类原生", fontSize = 11.sp) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            ),
                            border = null,
                            modifier = Modifier.height(24.dp)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.SETTINGS) },
                        modifier = Modifier.testTag("btn_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Tune,
                            contentDescription = "截屏设置"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                icon = { Icon(Icons.Default.AddPhotoAlternate, contentDescription = null) },
                text = { Text("导入照片拼接", fontWeight = FontWeight.SemiBold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_pick_images")
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 12.dp,
                bottom = innerPadding.calculateBottomPadding() + 80.dp,
                start = 16.dp,
                end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Status Snackbar / Banner
            if (statusMessage != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = statusMessage ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            IconButton(
                                onClick = { viewModel.clearStatusMessage() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "关闭",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Hero Action Banner
            item {
                HeroCaptureBanner(
                    onStartScrollCapture = {
                        viewModel.resetScrollStudio()
                        viewModel.navigateTo(AppScreen.SCROLL_STUDIO)
                    },
                    onStitchSamples = {
                        viewModel.loadSampleSlices()
                        viewModel.navigateTo(AppScreen.IMAGE_STITCHER)
                    }
                )
            }

            // 4 Grid Function Cards
            item {
                Text(
                    text = "核心功能",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FeatureCard(
                        title = "原生滚动截屏",
                        subtitle = "实时平滑扫描",
                        icon = Icons.Default.ExpandMore,
                        accentColor = Color(0xFF3B82F6),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("card_scroll_studio"),
                        onClick = {
                            viewModel.resetScrollStudio()
                            viewModel.navigateTo(AppScreen.SCROLL_STUDIO)
                        }
                    )
                    FeatureCard(
                        title = "多图无缝拼接",
                        subtitle = "智能重叠对齐",
                        icon = Icons.Default.VerticalSplit,
                        accentColor = Color(0xFF8B5CF6),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("card_image_stitcher"),
                        onClick = {
                            viewModel.loadSampleSlices()
                            viewModel.navigateTo(AppScreen.IMAGE_STITCHER)
                        }
                    )
                }
            }

            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FeatureCard(
                        title = "涂鸦与马赛克",
                        subtitle = "隐私遮盖与标注",
                        icon = Icons.Default.Draw,
                        accentColor = Color(0xFF10B981),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("card_editor"),
                        onClick = {
                            viewModel.loadSampleSlices()
                            viewModel.navigateTo(AppScreen.SCREENSHOT_EDITOR)
                        }
                    )
                    FeatureCard(
                        title = "带壳样机包装",
                        subtitle = "渐变背景与阴影",
                        icon = Icons.Default.Smartphone,
                        accentColor = Color(0xFFF43F5E),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("card_mockup"),
                        onClick = {
                            viewModel.loadSampleSlices()
                            viewModel.updateMockupConfig(
                                viewModel.deviceMockupConfig.value.copy(enabled = true)
                            )
                            viewModel.navigateTo(AppScreen.SCREENSHOT_EDITOR)
                        }
                    )
                }
            }

            // Saved Long Screenshots Section
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text(
                        text = "截屏历史与作品 (${savedScreenshots.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (savedScreenshots.isEmpty()) {
                item {
                    EmptyScreenshotState(
                        onStartCapture = {
                            viewModel.resetScrollStudio()
                            viewModel.navigateTo(AppScreen.SCROLL_STUDIO)
                        }
                    )
                }
            } else {
                items(savedScreenshots, key = { it.id }) { item ->
                    ScreenshotHistoryCard(
                        screenshot = item,
                        onClick = {
                            viewModel.selectScreenshotForView(item)
                        },
                        onShare = {
                            shareScreenshot(context, item)
                        },
                        onDelete = {
                            viewModel.deleteScreenshot(item)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun HeroCaptureBanner(
    onStartScrollCapture: () -> Unit,
    onStitchSamples: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = modifier
            .fillMaxWidth()
            .testTag("hero_capture_banner")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF1E1B4B), Color(0xFF312E81), Color(0xFF2563EB))
                    )
                )
                .padding(20.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF38BDF8).copy(alpha = 0.2f),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier
                                .padding(5.dp)
                                .fillMaxSize()
                        )
                    }
                    Text(
                        text = "Android 15 原生 ScrollCapture 架构",
                        color = Color(0xFF93C5FD),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Text(
                    text = "一键滚动截取完整长图\n智能缝合与隐私打码",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 32.sp
                )

                Text(
                    text = "告别多次手动分屏截图，自动计算像素重叠，完美移除多余导航与状态栏。",
                    color = Color(0xFFCBD5E1),
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Button(
                        onClick = onStartScrollCapture,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF38BDF8),
                            contentColor = Color(0xFF0F172A)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("btn_hero_start_scroll")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("进入截屏工坊", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onStitchSamples,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.6f), Color.White.copy(alpha = 0.6f)))
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("btn_hero_sample_stitch")
                    ) {
                        Text("体验样例拼接")
                    }
                }
            }
        }
    }
}

@Composable
fun FeatureCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        ),
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = accentColor.copy(alpha = 0.12f),
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier
                        .padding(10.dp)
                        .fillMaxSize()
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ScreenshotHistoryCard(
    screenshot: ScreenshotEntity,
    onClick: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateStr = remember(screenshot.timestamp) {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(screenshot.timestamp))
    }
    val sizeKb = screenshot.fileSizeBytes / 1024

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .testTag("screenshot_card_${screenshot.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(12.dp)
        ) {
            // Thumbnail preview
            Box(
                modifier = Modifier
                    .size(width = 60.dp, height = 90.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AsyncImage(
                    model = File(screenshot.filePath),
                    contentDescription = screenshot.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Metadata Column
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = screenshot.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text("${screenshot.width}×${screenshot.height} px", fontSize = 10.sp) },
                        modifier = Modifier.height(22.dp)
                    )
                    Text(
                        text = "$sizeKb KB",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // Actions
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton(
                    onClick = onShare,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "分享",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyScreenshotState(
    onStartCapture: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.CropFree,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxSize()
                )
            }
            Text(
                text = "暂无已保存的长截图",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "进入「截屏工坊」体验原生滚动截屏，或导入相册照片进行智能无缝拼接",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Button(
                onClick = onStartCapture,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("开始第一次截屏")
            }
        }
    }
}

fun shareScreenshot(context: android.content.Context, entity: ScreenshotEntity) {
    try {
        val file = File(entity.filePath)
        if (!file.exists()) return
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "分享长截图"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
