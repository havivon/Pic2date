package com.pic2date.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * On-device ML Kit text recognition (Latin script). Fast and fully offline.
 * ML Kit's on-device models do not currently cover Hebrew; a Hebrew engine can
 * be added behind [OcrEngine] without changing callers.
 */
class MlKitOcrEngine : OcrEngine {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun recognize(bitmap: Bitmap): String =
        suspendCancellableCoroutine { cont ->
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { result -> cont.resume(result.text) }
                .addOnFailureListener { e -> cont.resumeWithException(e) }
            cont.invokeOnCancellation { /* ML Kit has no per-call cancel */ }
        }

    override fun close() {
        recognizer.close()
    }
}
