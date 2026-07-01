package com.pic2date.ocr

import android.content.Context
import android.net.Uri
import com.pic2date.util.ImageLoader

/**
 * Coordinates image decoding and OCR. Uses the on-device ML Kit engine (no
 * network required). The [OcrEngine] abstraction lets additional engines (e.g. a
 * Tesseract-based Hebrew engine) be plugged in without touching callers.
 */
class OcrService(context: Context) {

    private val appContext = context.applicationContext
    private val engine: OcrEngine = MlKitOcrEngine()

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

        return try {
            val text = engine.recognize(bitmap)
            if (text.isNotBlank()) Result.Success(text) else Result.NoText
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    fun release() {
        runCatching { engine.close() }
    }
}
