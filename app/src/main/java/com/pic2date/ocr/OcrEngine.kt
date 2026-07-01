package com.pic2date.ocr

import android.graphics.Bitmap

/** Abstraction over an OCR backend so engines can be swapped or chained. */
interface OcrEngine {
    /** Returns recognized text (may be empty), or throws on hard failure. */
    suspend fun recognize(bitmap: Bitmap): String

    /** Release native resources. */
    fun close()
}

/** Thrown when an engine cannot run (e.g. trained data is missing). */
class OcrUnavailableException(message: String) : Exception(message)
