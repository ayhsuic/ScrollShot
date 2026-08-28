package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.db.ScreenshotEntity
import com.example.data.engine.ImageStitchEngine
import com.example.data.engine.MockCaptureProvider
import com.example.data.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.InputStream

enum class AppScreen {
    HOME,
    SCROLL_STUDIO,
    IMAGE_STITCHER,
    SCREENSHOT_EDITOR,
    GALLERY_DETAIL,
    SETTINGS
}

class ScrollCaptureViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val screenshotDao = db.screenshotDao()

    // Navigation state
    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Saved screenshots in database
    val savedScreenshots: StateFlow<List<ScreenshotEntity>> = screenshotDao.getAllScreenshots()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active selected screenshot for detail viewing / editing
    private val _selectedScreenshot = MutableStateFlow<ScreenshotEntity?>(null)
    val selectedScreenshot: StateFlow<ScreenshotEntity?> = _selectedScreenshot.asStateFlow()

    // Active slices for multi-image stitcher
    private val _activeSlices = MutableStateFlow<List<ImageSlice>>(emptyList())
    val activeSlices: StateFlow<List<ImageSlice>> = _activeSlices.asStateFlow()

    private val _stitchOrientation = MutableStateFlow(StitchOrientation.VERTICAL)
    val stitchOrientation: StateFlow<StitchOrientation> = _stitchOrientation.asStateFlow()

    // Stitched / Working Bitmap for Editor
    private val _workingBitmap = MutableStateFlow<Bitmap?>(null)
    val workingBitmap: StateFlow<Bitmap?> = _workingBitmap.asStateFlow()

    // Editor Markup state
    private val _activeMarkupTool = MutableStateFlow(MarkupTool.NONE)
    val activeMarkupTool: StateFlow<MarkupTool> = _activeMarkupTool.asStateFlow()

    private val _activeBrushColor = MutableStateFlow(Color(0xFFEF4444)) // Red default
    val activeBrushColor: StateFlow<Color> = _activeBrushColor.asStateFlow()

    private val _activeStrokeWidth = MutableStateFlow(12f)
    val activeStrokeWidth: StateFlow<Float> = _activeStrokeWidth.asStateFlow()

    private val _annotations = MutableStateFlow<List<AnnotationItem>>(emptyList())
    val annotations: StateFlow<List<AnnotationItem>> = _annotations.asStateFlow()

    private val _undoStack = MutableStateFlow<List<List<AnnotationItem>>>(emptyList())
    val canUndo: StateFlow<Boolean> = _undoStack.map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _deviceMockupConfig = MutableStateFlow(DeviceMockupConfig())
    val deviceMockupConfig: StateFlow<DeviceMockupConfig> = _deviceMockupConfig.asStateFlow()

    // Scroll Capture Simulator state
    private val _selectedPreset = MutableStateFlow(ScrollCaptureContentPreset.ARTICLE)
    val selectedPreset: StateFlow<ScrollCaptureContentPreset> = _selectedPreset.asStateFlow()

    private val _isAutoScrolling = MutableStateFlow(false)
    val isAutoScrolling: StateFlow<Boolean> = _isAutoScrolling.asStateFlow()

    private val _scrollProgress = MutableStateFlow(0f)
    val scrollProgress: StateFlow<Float> = _scrollProgress.asStateFlow()

    private val _nativeHudVisible = MutableStateFlow(false)
    val nativeHudVisible: StateFlow<Boolean> = _nativeHudVisible.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private var autoScrollJob: Job? = null
    private var cachedPresetBitmap: Bitmap? = null

    init {
        loadDefaultPreset()
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun selectPreset(preset: ScrollCaptureContentPreset) {
        _selectedPreset.value = preset
        loadDefaultPreset()
    }

    private fun loadDefaultPreset() {
        viewModelScope.launch {
            val bmp = MockCaptureProvider.generatePresetBitmap(_selectedPreset.value)
            cachedPresetBitmap = bmp
            _scrollProgress.value = 0.25f // initial viewport position
        }
    }

    /**
     * Start the native auto-scroll capture scanner animation
     */
    fun startAutoScrollCapture(onComplete: () -> Unit = {}) {
        if (_isAutoScrolling.value) return
        _isAutoScrolling.value = true
        _nativeHudVisible.value = false

        autoScrollJob?.cancel()
        autoScrollJob = viewModelScope.launch {
            val initial = _scrollProgress.value
            val target = 1.0f
            val durationMs = 2800L
            val steps = 70
            val stepDelay = durationMs / steps
            val increment = (target - initial) / steps

            for (i in 0 until steps) {
                delay(stepDelay)
                if (!_isAutoScrolling.value) break
                _scrollProgress.value = (_scrollProgress.value + increment).coerceIn(0f, 1f)
            }

            _isAutoScrolling.value = false
            _nativeHudVisible.value = true
            _statusMessage.value = "长截图抓取完成！已自动拼接全部内容"

            // Set working bitmap for editor
            cachedPresetBitmap?.let { bmp ->
                val captureHeight = (bmp.height * _scrollProgress.value).toInt().coerceAtLeast(800)
                val fullCapture = Bitmap.createBitmap(bmp, 0, 0, bmp.width, captureHeight)
                _workingBitmap.value = fullCapture
            }
            onComplete()
        }
    }

    fun stopAutoScrollCapture() {
        _isAutoScrolling.value = false
        autoScrollJob?.cancel()
        _nativeHudVisible.value = true
        _statusMessage.value = "已停止滚动截屏"
        cachedPresetBitmap?.let { bmp ->
            val captureHeight = (bmp.height * _scrollProgress.value).toInt().coerceAtLeast(800)
            val fullCapture = Bitmap.createBitmap(bmp, 0, 0, bmp.width, captureHeight)
            _workingBitmap.value = fullCapture
        }
    }

    fun dismissNativeHud() {
        _nativeHudVisible.value = false
    }

    fun resetScrollStudio() {
        _scrollProgress.value = 0.25f
        _isAutoScrolling.value = false
        _nativeHudVisible.value = false
        loadDefaultPreset()
    }

    /**
     * Slices & Multi-Image Stitcher actions
     */
    fun loadSampleSlices() {
        viewModelScope.launch {
            val longBmp = MockCaptureProvider.generatePresetBitmap(ScrollCaptureContentPreset.ARTICLE)
            val slices = MockCaptureProvider.createSampleSlices(longBmp, screenHeight = 1600, overlap = 220)
            _activeSlices.value = slices
            refreshStitchedResult()
        }
    }

    fun addImagesFromUris(context: Context, uris: List<Uri>) {
        viewModelScope.launch {
            val newSlices = mutableListOf<ImageSlice>()
            for (uri in uris) {
                try {
                    val stream: InputStream? = context.contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(stream)
                    stream?.close()
                    if (bitmap != null) {
                        newSlices.add(ImageSlice(bitmap = bitmap, sourceUri = uri))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            _activeSlices.value = _activeSlices.value + newSlices
            runAutoOverlapDetection()
            refreshStitchedResult()
        }
    }

    fun removeSlice(sliceId: String) {
        _activeSlices.value = _activeSlices.value.filter { it.id != sliceId }
        refreshStitchedResult()
    }

    fun moveSlice(fromIndex: Int, toIndex: Int) {
        val list = _activeSlices.value.toMutableList()
        if (fromIndex in list.indices && toIndex in list.indices) {
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            _activeSlices.value = list
            refreshStitchedResult()
        }
    }

    fun updateSliceOverlap(sliceId: String, offsetPixels: Int) {
        _activeSlices.value = _activeSlices.value.map {
            if (it.id == sliceId) it.copy(overlapOffsetPixels = offsetPixels) else it
        }
        refreshStitchedResult()
    }

    fun updateSliceCrop(sliceId: String, topRatio: Float, bottomRatio: Float) {
        _activeSlices.value = _activeSlices.value.map {
            if (it.id == sliceId) it.copy(topCropRatio = topRatio, bottomCropRatio = bottomRatio) else it
        }
        refreshStitchedResult()
    }

    fun toggleStitchOrientation() {
        _stitchOrientation.value = if (_stitchOrientation.value == StitchOrientation.VERTICAL) {
            StitchOrientation.HORIZONTAL
        } else {
            StitchOrientation.VERTICAL
        }
        refreshStitchedResult()
    }

    fun runAutoOverlapDetection() {
        viewModelScope.launch {
            val slices = _activeSlices.value
            if (slices.size < 2) return@launch
            val updated = mutableListOf<ImageSlice>()
            updated.add(slices[0])

            for (i in 1 until slices.size) {
                val prev = updated[i - 1]
                val curr = slices[i]
                val detected = ImageStitchEngine.detectSmartOverlap(prev.bitmap, curr.bitmap)
                updated.add(curr.copy(overlapOffsetPixels = if (detected > 0) detected else 180))
            }
            _activeSlices.value = updated
            refreshStitchedResult()
            _statusMessage.value = "已自动完成智能重叠分析与对齐"
        }
    }

    private fun refreshStitchedResult() {
        viewModelScope.launch {
            val slices = _activeSlices.value
            if (slices.isEmpty()) {
                _workingBitmap.value = null
                return@launch
            }
            val stitched = ImageStitchEngine.stitchImages(slices, _stitchOrientation.value)
            _workingBitmap.value = stitched
        }
    }

    /**
     * Editor Markup Actions
     */
    fun setMarkupTool(tool: MarkupTool) {
        _activeMarkupTool.value = tool
    }

    fun setBrushColor(color: Color) {
        _activeBrushColor.value = color
    }

    fun setStrokeWidth(width: Float) {
        _activeStrokeWidth.value = width
    }

    fun addAnnotation(item: AnnotationItem) {
        _undoStack.value = _undoStack.value + listOf(_annotations.value)
        _annotations.value = _annotations.value + item
    }

    fun undoMarkup() {
        val stack = _undoStack.value
        if (stack.isNotEmpty()) {
            _annotations.value = stack.last()
            _undoStack.value = stack.dropLast(1)
        }
    }

    fun clearAllMarkup() {
        if (_annotations.value.isNotEmpty()) {
            _undoStack.value = _undoStack.value + listOf(_annotations.value)
            _annotations.value = emptyList()
        }
    }

    fun updateMockupConfig(config: DeviceMockupConfig) {
        _deviceMockupConfig.value = config
    }

    fun setWorkingBitmap(bitmap: Bitmap) {
        _workingBitmap.value = bitmap
        _annotations.value = emptyList()
        _undoStack.value = emptyList()
    }

    fun selectScreenshotForView(screenshot: ScreenshotEntity) {
        _selectedScreenshot.value = screenshot
        try {
            val bmp = BitmapFactory.decodeFile(screenshot.filePath)
            if (bmp != null) {
                _workingBitmap.value = bmp
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _currentScreen.value = AppScreen.GALLERY_DETAIL
    }

    /**
     * Export / Save long screenshot to Gallery and Room DB
     */
    fun saveCurrentScreenshot(
        title: String = "长截图",
        captureType: String = "SCROLL_CAPTURE",
        onSuccess: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            val base = _workingBitmap.value ?: return@launch
            val context = getApplication<Application>()

            // 1. Render all annotations
            val annotatedBmp = ImageStitchEngine.renderMarkupOnBitmap(base, _annotations.value)

            // 2. Render Mockup frame if enabled
            val finalBmp = if (_deviceMockupConfig.value.enabled || _deviceMockupConfig.value.background != MockupBackground.NONE) {
                ImageStitchEngine.renderDeviceMockup(annotatedBmp, _deviceMockupConfig.value)
            } else {
                annotatedBmp
            }

            // 3. Save to storage
            val (filePath, sizeBytes) = ImageStitchEngine.saveBitmap(context, finalBmp, title)

            // 4. Insert into Room DB
            val entity = ScreenshotEntity(
                title = title,
                filePath = filePath,
                width = finalBmp.width,
                height = finalBmp.height,
                sliceCount = _activeSlices.value.size.coerceAtLeast(1),
                fileSizeBytes = sizeBytes,
                captureType = captureType,
                timestamp = System.currentTimeMillis()
            )
            val id = screenshotDao.insertScreenshot(entity)
            _selectedScreenshot.value = entity.copy(id = id)
            _statusMessage.value = "长截图已成功保存到相册与历史归档"
            onSuccess(id)
        }
    }

    fun deleteScreenshot(screenshot: ScreenshotEntity) {
        viewModelScope.launch {
            screenshotDao.deleteScreenshot(screenshot)
            try {
                val f = java.io.File(screenshot.filePath)
                if (f.exists()) f.delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (_selectedScreenshot.value?.id == screenshot.id) {
                _selectedScreenshot.value = null
                _currentScreen.value = AppScreen.HOME
            }
            _statusMessage.value = "已删除截图记录"
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}
