package com.example.bodycamai

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.core.ImageAnalysis
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.video.AudioConfig
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.bodycamai.core.AiFrameResult
import com.example.bodycamai.core.SensorFrame
import com.example.bodycamai.core.SensorFusionPipeline
import com.example.bodycamai.core.SensorSource
import com.example.bodycamai.p2p.LiveStreamBridge
import com.example.bodycamai.sensors.adapters.ExternalCameraController
import com.example.bodycamai.recording.RecordingRecovery
import com.example.bodycamai.recording.RecordingRetentionManager

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    onRecordingChanged: (Boolean) -> Unit,
    onError: (Throwable) -> Unit,
    onAiResult: (AiFrameResult) -> Unit,
    onCaptureIdChanged: (String?) -> Unit = {},
    frontCamera: Boolean = false,
    externalCamera: Boolean = false,
    fusionPipeline: SensorFusionPipeline? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val aiEngine = remember { OfflineAiEngine() }
    val externalController = remember { ExternalCameraController(context) }
    val recordingRecovery = remember { RecordingRecovery(context) }
    val retentionManager = remember { RecordingRetentionManager(context.contentResolver) }
    val controller = remember {
        LifecycleCameraController(context).apply {
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            setEnabledUseCases(
                CameraController.IMAGE_CAPTURE or
                    CameraController.VIDEO_CAPTURE or
                    CameraController.IMAGE_ANALYSIS
            )
        }
    }

    DisposableEffect(lifecycleOwner, controller, aiEngine) {
        val executor = ContextCompat.getMainExecutor(context)
        controller.setImageAnalysisAnalyzer(executor, ImageAnalysis.Analyzer { imageProxy ->
            val mediaImage = imageProxy.image
            fusionPipeline?.offer(
                SensorFrame(
                    source = SensorSource.PHONE_CAMERA,
                    timestampNs = imageProxy.imageInfo.timestamp,
                    width = imageProxy.width,
                    height = imageProxy.height,
                    rotationDegrees = imageProxy.imageInfo.rotationDegrees
                )
            )
            if (mediaImage == null) {
                imageProxy.close()
                return@Analyzer
            }
            // Live View gets the same authorized local frame. When no Live session is attached,
            // LiveStreamBridge simply closes/returns and AI continues normally below.
            LiveStreamBridge.offer(imageProxy)
            // ImageProxy ownership is consumed by the live bridge when attached, so AI receives
            // its own analysis image only when Live View is inactive.
            if (LiveStreamBridge.isAttached()) return@Analyzer
            aiEngine.analyze(mediaImage, imageProxy.imageInfo.rotationDegrees) { result ->
                onAiResult(result)
                imageProxy.close()
            }
        })
        controller.bindToLifecycle(lifecycleOwner)
        onDispose {
            controller.clearImageAnalysisAnalyzer()
            controller.unbind()
            aiEngine.close()
            RecordingBridge.clear()
        }
    }

    LaunchedEffect(frontCamera, externalCamera) {
        if (externalCamera) {
            runCatching {
                val future = ProcessCameraProvider.getInstance(context)
                future.addListener({
                    val provider = future.get()
                    val selector = externalController.selector(provider)
                    if (selector != null) controller.cameraSelector = selector
                }, ContextCompat.getMainExecutor(context))
            }.onFailure(onError)
        } else {
            controller.cameraSelector = if (frontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
        }
    }

    AndroidView(
        modifier = modifier,
        factory = {
            PreviewView(context).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                this.controller = controller
            }
        }
    )

    RecordingBridge.register(
        start = {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                onError(IllegalStateException("Нет разрешения на камеру"))
                return@register
            }
            try {
                val captureId = System.currentTimeMillis().toString()
                val name = "BODYCAM_${captureId}.mp4"
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, name)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    if (Build.VERSION.SDK_INT >= 29) {
                        put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/BodyCamAI")
                    }
                }
                val output = MediaStoreOutputOptions.Builder(
                    context.contentResolver,
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                ).setContentValues(values).build()

                val pending = controller.startRecording(
                    output,
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        AudioConfig.create(true)
                    } else {
                        AudioConfig.create(false)
                    },
                    ContextCompat.getMainExecutor(context)
                ) { event ->
                    when (event) {
                        is VideoRecordEvent.Start -> {
                            onCaptureIdChanged(captureId)
                            recordingRecovery.markStarted(captureId)
                            onRecordingChanged(true)
                        }
                        is VideoRecordEvent.Finalize -> {
                            onRecordingChanged(false)
                            recordingRecovery.markFinished()
                            onCaptureIdChanged(null)
                            if (event.error == VideoRecordEvent.Finalize.ERROR_NONE) {
                                runCatching { retentionManager.trim(50) }
                            } else {
                                onError(IllegalStateException("Ошибка сохранения видео: ${event.error}"))
                            }
                        }
                    }
                }
                RecordingBridge.recording = pending
            } catch (t: Throwable) {
                onCaptureIdChanged(null)
                onRecordingChanged(false)
                onError(t)
            }
        },
        stop = {
            RecordingBridge.recording?.stop()
            RecordingBridge.recording = null
        },
        photo = {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                onError(IllegalStateException("Нет разрешения на камеру"))
                return@register
            }
            val name = "BODYCAM_${System.currentTimeMillis()}.jpg"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= 29) put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/BodyCamAI")
            }
            val output = androidx.camera.core.ImageCapture.OutputFileOptions.Builder(
                context.contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            ).build()
            controller.takePicture(
                output,
                ContextCompat.getMainExecutor(context),
                object : androidx.camera.core.ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(result: androidx.camera.core.ImageCapture.OutputFileResults) = Unit
                    override fun onError(exception: androidx.camera.core.ImageCaptureException) = onError(exception)
                }
            )
        }
    )
}

object RecordingBridge {
    var recording: Recording? = null
    private var startAction: (() -> Unit)? = null
    private var stopAction: (() -> Unit)? = null
    private var photoAction: (() -> Unit)? = null

    fun register(start: () -> Unit, stop: () -> Unit, photo: () -> Unit) {
        startAction = start
        stopAction = stop
        photoAction = photo
    }

    fun start() = startAction?.invoke()
    fun stop() = stopAction?.invoke()
    fun photo() = photoAction?.invoke()
    fun clear() {
        startAction = null
        stopAction = null
        photoAction = null
        recording = null
    }
}
