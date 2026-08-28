package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ImageSlice
import com.example.data.model.StitchOrientation
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.ScrollCaptureViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageStitcherScreen(
    viewModel: ScrollCaptureViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeSlices by viewModel.activeSlices.collectAsState()
    val stitchOrientation by viewModel.stitchOrientation.collectAsState()
    val workingBitmap by viewModel.workingBitmap.collectAsState()

    var selectedSliceIndex by remember { mutableStateOf(0) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(10)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.addImagesFromUris(context, uris)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "多图无缝拼接",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "已选 ${activeSlices.size} 张截图 • ${if (stitchOrientation == StitchOrientation.VERTICAL) "纵向缝合" else "横向缝合"}",
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // Toggle Vertical / Horizontal
                    IconButton(
                        onClick = { viewModel.toggleStitchOrientation() },
                        modifier = Modifier.testTag("btn_toggle_orientation")
                    ) {
                        Icon(
                            imageVector = if (stitchOrientation == StitchOrientation.VERTICAL) Icons.Default.VerticalSplit else Icons.Default.HorizontalSplit,
                            contentDescription = "切换拼接方向"
                        )
                    }
                    // Auto overlap detector
                    IconButton(
                        onClick = { viewModel.runAutoOverlapDetection() },
                        modifier = Modifier.testTag("btn_auto_overlap")
                    ) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = "智能重叠分析", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.runAutoOverlapDetection()
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("智能对齐")
                    }

                    Button(
                        onClick = {
                            viewModel.navigateTo(AppScreen.SCREENSHOT_EDITOR)
                        },
                        enabled = workingBitmap != null,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.weight(1.2f).height(48.dp).testTag("btn_proceed_editor")
                    ) {
                        Text("进入画板编辑", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Slices management list
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "截图片段序列 (点击单张可微调)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("添加截图", fontSize = 13.sp)
                    }
                }
            }

            // Slices Horizontal list
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(activeSlices) { index, slice ->
                        SliceThumbnailCard(
                            slice = slice,
                            index = index,
                            isSelected = (selectedSliceIndex == index),
                            onSelect = { selectedSliceIndex = index },
                            onDelete = { viewModel.removeSlice(slice.id) },
                            onMoveLeft = if (index > 0) { { viewModel.moveSlice(index, index - 1) } } else null,
                            onMoveRight = if (index < activeSlices.size - 1) { { viewModel.moveSlice(index, index + 1) } } else null
                        )
                    }
                }
            }

            // Slice Seam & Crop Adjustment Panel
            if (activeSlices.isNotEmpty() && selectedSliceIndex in activeSlices.indices) {
                val currentSlice = activeSlices[selectedSliceIndex]
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "片段 #${selectedSliceIndex + 1} 接缝与去边微调",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text("重叠 ${currentSlice.overlapOffsetPixels} px", fontSize = 11.sp) },
                                    modifier = Modifier.height(24.dp)
                                )
                            }

                            // Overlap offset slider
                            if (selectedSliceIndex > 0) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("与上一张重叠距离", style = MaterialTheme.typography.bodySmall)
                                        Text("${currentSlice.overlapOffsetPixels} 像素", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = currentSlice.overlapOffsetPixels.toFloat(),
                                        onValueChange = {
                                            viewModel.updateSliceOverlap(currentSlice.id, it.toInt())
                                        },
                                        valueRange = 0f..500f,
                                        modifier = Modifier.testTag("slider_overlap")
                                    )
                                }
                            }

                            // Top Crop Slider (Status bar remover)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("顶部状态栏裁剪", style = MaterialTheme.typography.bodySmall)
                                    Text("${(currentSlice.topCropRatio * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = currentSlice.topCropRatio,
                                    onValueChange = {
                                        viewModel.updateSliceCrop(currentSlice.id, it, currentSlice.bottomCropRatio)
                                    },
                                    valueRange = 0f..0.2f
                                )
                            }

                            // Bottom Crop Slider (Nav bar remover)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("底部导航栏裁剪", style = MaterialTheme.typography.bodySmall)
                                    Text("${(currentSlice.bottomCropRatio * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = currentSlice.bottomCropRatio,
                                    onValueChange = {
                                        viewModel.updateSliceCrop(currentSlice.id, currentSlice.topCropRatio, it)
                                    },
                                    valueRange = 0f..0.2f
                                )
                            }
                        }
                    }
                }
            }

            // Real-time Stitched Result Preview
            item {
                Text(
                    text = "实时拼接预览",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 280.dp, max = 520.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        if (workingBitmap != null) {
                            Image(
                                bitmap = workingBitmap!!.asImageBitmap(),
                                contentDescription = "拼接预览",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        } else {
                            Text(
                                text = "暂无拼接内容，请添加截图",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SliceThumbnailCard(
    slice: ImageSlice,
    index: Int,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    onMoveLeft: (() -> Unit)?,
    onMoveRight: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        ),
        border = if (isSelected) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)) else null,
        modifier = modifier
            .width(100.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onSelect() }
            .testTag("slice_thumb_$index")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(6.dp)
        ) {
            // Header with slice number and delete
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${index + 1}",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "移除",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Thumbnail image
            Box(
                modifier = Modifier
                    .size(width = 88.dp, height = 110.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black)
            ) {
                Image(
                    bitmap = slice.bitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Reorder buttons
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (onMoveLeft != null) {
                    IconButton(onClick = onMoveLeft, modifier = Modifier.size(22.dp)) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "左移", modifier = Modifier.size(16.dp))
                    }
                }
                if (onMoveRight != null) {
                    IconButton(onClick = onMoveRight, modifier = Modifier.size(22.dp)) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "右移", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
