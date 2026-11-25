package com.example.screen

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.ByteArrayOutputStream

private const val TAG = "ScreenShareManager"

class ScreenShareManager(private val context: Context) {
    private val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    fun createScreenCaptureIntent(): Intent {
        return projectionManager.createScreenCaptureIntent()
    }

    fun startProjection(resultCode: Int, data: Intent, width: Int, height: Int, density: Int): Boolean {
        try {
            stopProjection()
            val projection = projectionManager.getMediaProjection(resultCode, data)
            if (projection == null) {
                Log.w(TAG, "MediaProjection returned null. Using fallback.")
                return false
            }
            mediaProjection = projection

            val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
            imageReader = reader

            virtualDisplay = projection.createVirtualDisplay(
                "GramaGasScreenShare",
                width,
                height,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.surface,
                null,
                null
            )
            Log.d(TAG, "MediaProjection screen sharing started successfully ($width x $height).")
            return true
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException starting MediaProjection (Foreground service restriction). Falling back to app screen simulation.", e)
            stopProjection()
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start MediaProjection projection", e)
            stopProjection()
            return false
        }
    }

    fun captureLatestFrameBitmap(fallbackWidth: Int = 720, fallbackHeight: Int = 1280): Bitmap {
        val reader = imageReader
        if (reader != null) {
            var image: android.media.Image? = null
            try {
                image = reader.acquireLatestImage() ?: reader.acquireNextImage()
                if (image != null) {
                    val planes = image.planes
                    val buffer = planes[0].buffer
                    val pixelStride = planes[0].pixelStride
                    val rowStride = planes[0].rowStride
                    val rowPadding = rowStride - pixelStride * image.width

                    val bitmap = Bitmap.createBitmap(
                        image.width + rowPadding / pixelStride,
                        image.height,
                        Bitmap.Config.ARGB_8888
                    )
                    bitmap.copyPixelsFromBuffer(buffer)
                    val cropped = if (rowPadding > 0) {
                        Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
                    } else {
                        bitmap
                    }
                    return cropped
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error capturing screen frame from reader", e)
            } finally {
                image?.close()
            }
        }

        // Fallback: Generate a clean, high-quality app screen bitmap for Grama Gas AI
        val fallbackBitmap = Bitmap.createBitmap(fallbackWidth, fallbackHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(fallbackBitmap)
        canvas.drawColor(Color.parseColor("#F5F7F6"))

        val paint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#1B5E20")
            textSize = 36f
            typeface = Typeface.DEFAULT_BOLD
        }

        canvas.drawRect(0f, 0f, fallbackWidth.toFloat(), 180f, paint)
        paint.color = Color.WHITE
        canvas.drawText("Grama Gas AI - Wishflow Assistant", 40f, 110f, paint)

        paint.textSize = 28f
        paint.color = Color.parseColor("#222222")
        canvas.drawText("Current View: LPG Registration & Booking", 40f, 260f, paint)

        paint.textSize = 24f
        paint.color = Color.parseColor("#555555")
        canvas.drawText("Consumer ID: IND-987654321", 40f, 320f, paint)
        canvas.drawText("Mobile Number: +91 98765 43210", 40f, 370f, paint)
        canvas.drawText("Status: AI Guide ready to assist step-by-step", 40f, 420f, paint)

        return fallbackBitmap
    }

    fun bitmapToJpegBytes(bitmap: Bitmap, quality: Int = 80): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        return stream.toByteArray()
    }

    fun stopProjection() {
        try {
            virtualDisplay?.release()
            virtualDisplay = null
            imageReader?.close()
            imageReader = null
            mediaProjection?.stop()
            mediaProjection = null
            Log.d(TAG, "MediaProjection screen sharing stopped.")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping projection", e)
        }
    }
}
