package com.smartcalc.ai.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * Decoding, down-scaling, rotation and encoding of the captured / picked image.
 * Every method fails softly (null) instead of throwing.
 */
class ImageProcessor(private val context: Context) {

    companion object {
        private const val MAX_DIMENSION = 1600
        private const val JPEG_QUALITY = 85
        private const val BLUR_THRESHOLD = 55.0
    }

    /** Copies a gallery Uri into our own cache directory so we fully control the file. */
    fun copyUriToCache(uri: Uri, fileName: String = "picked_${System.currentTimeMillis()}.jpg"): File? {
        return try {
            val target = File(cacheDir(), fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(target).use { output -> input.copyTo(output) }
            } ?: return null
            if (target.length() == 0L || decodeBounds(target) == null) {
                target.delete()
                null
            } else {
                target
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Moves an image from cache into permanent app storage for the history. */
    fun persistForHistory(source: File): String? = try {
        val dir = File(context.filesDir, "solved").apply { mkdirs() }
        val target = File(dir, "solved_${System.currentTimeMillis()}.jpg")
        source.copyTo(target, overwrite = true)
        target.absolutePath
    } catch (e: Exception) {
        null
    }

    fun cacheDir(): File = File(context.cacheDir, "captures").apply { mkdirs() }

    fun newCaptureFile(): File = File(cacheDir(), "capture_${System.currentTimeMillis()}.jpg")

    /** Deletes older capture files so the cache does not grow forever. */
    fun clearOldCaptures(keep: File? = null) {
        runCatching {
            cacheDir().listFiles()?.forEach { file ->
                if (file.absolutePath != keep?.absolutePath) file.delete()
            }
        }
    }

    /** Down-scales, rotates by EXIF and returns Base64 JPEG, or null if unreadable. */
    fun toBase64Jpeg(file: File): String? {
        return try {
            val bitmap = loadScaledBitmap(file) ?: return null
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)
            bitmap.recycle()
            Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
        } catch (e: Throwable) {
            null
        }
    }

    /** Rough sharpness check so obviously blurry photos are rejected before upload. */
    fun looksTooBlurry(file: File): Boolean {
        return try {
            val bitmap = loadScaledBitmap(file, maxDimension = 512) ?: return false
            val variance = laplacianVariance(bitmap)
            bitmap.recycle()
            variance < BLUR_THRESHOLD
        } catch (e: Throwable) {
            false // never block the user because the heuristic failed
        }
    }

    private fun decodeBounds(file: File): BitmapFactory.Options? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        return if (options.outWidth > 0 && options.outHeight > 0) options else null
    }

    private fun loadScaledBitmap(file: File, maxDimension: Int = MAX_DIMENSION): Bitmap? {
        val bounds = decodeBounds(file) ?: return null
        var sample = 1
        while (bounds.outWidth / sample > maxDimension || bounds.outHeight / sample > maxDimension) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = BitmapFactory.decodeFile(file.absolutePath, options) ?: return null
        return applyExifRotation(file, decoded)
    }

    private fun applyExifRotation(file: File, bitmap: Bitmap): Bitmap = try {
        val exif = ExifInterface(file.absolutePath)
        val degrees = when (
            exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        ) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (degrees == 0f) {
            bitmap
        } else {
            val matrix = Matrix().apply { postRotate(degrees) }
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated != bitmap) bitmap.recycle()
            rotated
        }
    } catch (e: Exception) {
        bitmap
    }

    /** Variance of a 3x3 Laplacian over the grayscale image. Low variance == blurry. */
    private fun laplacianVariance(bitmap: Bitmap): Double {
        val width = bitmap.width
        val height = bitmap.height
        if (width < 8 || height < 8) return Double.MAX_VALUE

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val gray = DoubleArray(width * height)
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            gray[i] = 0.299 * r + 0.587 * g + 0.114 * b
        }

        var sum = 0.0
        var sumSq = 0.0
        var count = 0
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val idx = y * width + x
                val value = -4 * gray[idx] +
                    gray[idx - 1] + gray[idx + 1] +
                    gray[idx - width] + gray[idx + width]
                sum += value
                sumSq += value * value
                count++
            }
        }
        if (count == 0) return Double.MAX_VALUE
        val mean = sum / count
        return sumSq / count - mean * mean
    }
}
