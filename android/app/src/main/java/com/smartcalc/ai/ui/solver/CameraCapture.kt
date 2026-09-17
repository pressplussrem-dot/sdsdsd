package com.smartcalc.ai.ui.solver

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.smartcalc.ai.R
import java.io.File
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private const val TAG = "CameraCapture"

/**
 * CameraX preview bound to the composable lifecycle.
 * [onCaptureReady] hands back a lambda that takes the actual picture.
 */
@Composable
fun CameraCapture(
    modifier: Modifier = Modifier,
    onCaptureReady: (capture: (onSaved: (File) -> Unit, onError: (Int) -> Unit) -> Unit) -> Unit,
    onCameraError: (Int) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    val executor: Executor = remember { ContextCompat.getMainExecutor(context) }

    LaunchedEffect(lifecycleOwner) {
        try {
            val cameraProvider = context.awaitCameraProvider()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            cameraProvider.unbindAll()

            val selector = when {
                cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ->
                    CameraSelector.DEFAULT_BACK_CAMERA
                cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ->
                    CameraSelector.DEFAULT_FRONT_CAMERA
                else -> {
                    onCameraError(R.string.error_camera_unavailable)
                    return@LaunchedEffect
                }
            }

            cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)

            onCaptureReady { onSaved, onError ->
                takePicture(context, imageCapture, executor, onSaved, onError)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Camera could not be started", e)
            onCameraError(R.string.error_camera_unavailable)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching {
                ProcessCameraProvider.getInstance(context).get().unbindAll()
            }
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )
    }
}

private fun takePicture(
    context: Context,
    imageCapture: ImageCapture,
    executor: Executor,
    onSaved: (File) -> Unit,
    onError: (Int) -> Unit
) {
    val target = File(
        File(context.cacheDir, "captures").apply { mkdirs() },
        "capture_${System.currentTimeMillis()}.jpg"
    )
    val options = ImageCapture.OutputFileOptions.Builder(target).build()

    try {
        imageCapture.takePicture(
            options,
            executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    if (target.exists() && target.length() > 0) {
                        onSaved(target)
                    } else {
                        onError(R.string.error_camera_failed)
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "takePicture failed", exception)
                    onError(R.string.error_camera_failed)
                }
            }
        )
    } catch (e: Exception) {
        Log.e(TAG, "takePicture threw", e)
        onError(R.string.error_camera_failed)
    }
}

private suspend fun Context.awaitCameraProvider(): ProcessCameraProvider =
    suspendCoroutine { continuation ->
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener(
            {
                try {
                    continuation.resume(future.get())
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }
            },
            ContextCompat.getMainExecutor(this)
        )
    }
