package com.pic2date.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Offline OCR via Tesseract, configured for Hebrew + English in a single pass.
 * Trained data files (`heb`, `eng`, and `osd` for orientation detection) are
 * bundled in `assets/tessdata/` and copied to internal storage on first use.
 *
 * The engine grayscales/upscales the image, then uses OSD to auto-rotate photos
 * that were taken sideways before recognizing text.
 *
 * If the trained data is missing, [recognize] throws [OcrUnavailableException]
 * so callers can fall back to another engine.
 */
class TesseractOcrEngine(
    context: Context,
    private val languages: String = "heb+eng",
) : OcrEngine {

    private val appContext = context.applicationContext

    private val dataDir = File(appContext.filesDir, "tesseract")
    private val tessDataDir = File(dataDir, "tessdata")

    @Volatile
    private var api: TessBaseAPI? = null

    // Language data is required; osd enables orientation detection (best-effort).
    private val requiredFiles: List<String> = languages.split("+").map { "$it.traineddata" }
    private val optionalFiles: List<String> = listOf("osd.traineddata")

    @Synchronized
    private fun ensureApi(): TessBaseAPI {
        api?.let { return it }
        if (!copyTrainedData()) {
            throw OcrUnavailableException("Tesseract trained data for '$languages' is missing")
        }
        val instance = TessBaseAPI()
        if (!instance.init(dataDir.absolutePath, languages)) {
            instance.recycle()
            throw OcrUnavailableException("Tesseract failed to initialize for '$languages'")
        }
        api = instance
        return instance
    }

    private fun copyTrainedData(): Boolean {
        if (!tessDataDir.exists()) tessDataDir.mkdirs()
        for (fileName in requiredFiles) {
            if (!copyAsset(fileName, required = true)) return false
        }
        for (fileName in optionalFiles) {
            copyAsset(fileName, required = false)
        }
        return true
    }

    private fun copyAsset(fileName: String, required: Boolean): Boolean {
        val target = File(tessDataDir, fileName)
        if (target.exists() && target.length() > 0) return true
        return runCatching {
            appContext.assets.open("tessdata/$fileName").use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
        }.isSuccess
    }

    override suspend fun recognize(bitmap: Bitmap): String = withContext(Dispatchers.Default) {
        val instance = ensureApi()
        val prepared = preprocess(bitmap)
        try {
            // PSM_AUTO_OSD runs orientation+script detection (via osd.traineddata)
            // and rotates sideways photos internally before recognizing.
            instance.pageSegMode = TessBaseAPI.PageSegMode.PSM_AUTO_OSD
            instance.setImage(prepared)
            instance.getUTF8Text().orEmpty().trim()
        } finally {
            instance.clear()
            if (prepared != bitmap) prepared.recycle()
        }
    }

    /** Grayscale + upscale small images for more reliable OCR. */
    private fun preprocess(src: Bitmap): Bitmap {
        val longest = maxOf(src.width, src.height)
        val target = 1800
        val scale = if (longest < target) target.toFloat() / longest else 1f
        val scaled = if (scale != 1f) {
            Bitmap.createScaledBitmap(src, (src.width * scale).toInt(), (src.height * scale).toInt(), true)
        } else {
            src
        }

        val gray = Bitmap.createBitmap(scaled.width, scaled.height, Bitmap.Config.ARGB_8888)
        Canvas(gray).drawBitmap(
            scaled,
            0f,
            0f,
            Paint().apply { colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) }) },
        )
        if (scaled != src) scaled.recycle()
        return gray
    }

    @Synchronized
    override fun close() {
        api?.recycle()
        api = null
    }
}
