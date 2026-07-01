package com.pic2date.ocr

import android.content.Context
import android.net.Uri
import com.pic2date.util.ImageLoader

/**
 * Coordinates image decoding and OCR. Prefers the offline Tesseract engine
 * (Hebrew + English); if its trained data is unavailable or yields nothing, it
 * falls back to ML Kit (Latin). Fully on-device — no network required.
 */
class OcrService(context: Context) {

    private val appContext = context.applicationContext
    private val tesseract by lazy { TesseractOcrEngine(appContext) }
    private val mlKit by lazy { MlKitOcrEngine() }

    sealed interface Result {
        data class Success(val text: String) : Result
        data object NoText : Result
        data class Error(val cause: Throwable) : Result
    }

    suspend fun scan(uri: Uri, mimeType: String?): Result {
        val bitmap = try {
            ImageLoader.load(appContext, uri, mimeType)
        } catch (e: Exception) {
            return Result.Error(e)
        } ?: return Result.Error(IllegalStateException("Could not decode image"))

        // 1. Tesseract (covers Hebrew + English offline).
        val tessText = runCatching { tesseract.recognize(bitmap) }.getOrNull()
        if (!tessText.isNullOrBlank()) return Result.Success(tessText)

        // 2. Fallback to ML Kit (Latin only).
        val mlText = runCatching { mlKit.recognize(bitmap) }.getOrNull()
        return when {
            !mlText.isNullOrBlank() -> Result.Success(mlText)
            tessText != null || mlText != null -> Result.NoText
            else -> Result.Error(IllegalStateException("All OCR engines failed"))
        }
    }

    fun release() {
        runCatching { tesseract.close() }
        runCatching { mlKit.close() }
    }
}
