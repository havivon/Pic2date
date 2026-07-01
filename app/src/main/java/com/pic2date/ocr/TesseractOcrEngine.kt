package com.pic2date.ocr

import android.content.Context
import android.graphics.Bitmap
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Offline OCR via Tesseract, configured for Hebrew + English in a single pass.
 * Trained data files (`heb.traineddata`, `eng.traineddata`) are bundled in
 * `assets/tessdata/` and copied to internal storage on first use.
 *
 * If the trained data is missing, [recognize] throws [OcrUnavailableException]
 * so callers can fall back to another engine.
 */
class TesseractOcrEngine(
    context: Context,
    private val languages: String = "heb+eng",
) : OcrEngine {

    private val appContext = context.applicationContext

    // Tesseract expects a parent dir containing a "tessdata" folder.
    private val dataDir = File(appContext.filesDir, "tesseract")
    private val tessDataDir = File(dataDir, "tessdata")

    @Volatile
    private var api: TessBaseAPI? = null

    private val requiredFiles: List<String> =
        languages.split("+").map { "$it.traineddata" }

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
        instance.pageSegMode = TessBaseAPI.PageSegMode.PSM_AUTO_OSD
        api = instance
        return instance
    }

    /** Copies any missing trained data from assets. Returns true if all present. */
    private fun copyTrainedData(): Boolean {
        if (!tessDataDir.exists()) tessDataDir.mkdirs()
        for (fileName in requiredFiles) {
            val target = File(tessDataDir, fileName)
            if (target.exists() && target.length() > 0) continue
            val copied = runCatching {
                appContext.assets.open("tessdata/$fileName").use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                }
            }.isSuccess
            if (!copied) return false
        }
        return true
    }

    override suspend fun recognize(bitmap: Bitmap): String = withContext(Dispatchers.Default) {
        val instance = ensureApi()
        try {
            instance.setImage(bitmap)
            instance.getUTF8Text().orEmpty().trim()
        } finally {
            instance.clear()
        }
    }

    @Synchronized
    override fun close() {
        api?.recycle()
        api = null
    }
}
