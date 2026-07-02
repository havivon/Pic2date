package com.pic2date.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Offline OCR via Tesseract, configured for Hebrew + English in a single pass.
 * Trained data files (`heb.traineddata`, `eng.traineddata`) are bundled in
 * `assets/tessdata/` and copied to internal storage on first use.
 *
 * Recognition uses PSM_AUTO (per-word language choice between heb and eng).
 * PSM_AUTO_OSD is deliberately NOT used: its page-level script detection can
 * decide the whole page is Latin and read Hebrew glyphs as Latin shapes.
 * Sideways photos are handled instead by a rotation vote: when the first pass
 * yields little real text, the engine retries 90/180/270 degrees on a smaller
 * copy, picks the best-scoring angle, and re-runs at full resolution.
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

    private val requiredFiles: List<String> = languages.split("+").map { "$it.traineddata" }

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
        // Android Bitmaps carry no print resolution, so Tesseract guesses one —
        // and its page-layout analysis is very sensitive to that guess (a wrong
        // DPI collapsed a full invitation to four lines). 150 measured best on
        // real invitation photos; see docs/ocr-lab.md for the methodology.
        instance.setVariable("user_defined_dpi", "150")
        api = instance
        return instance
    }

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
        val prepared = preprocess(bitmap)
        try {
            var text = runPass(instance, prepared)

            // Weak result? The photo may be sideways — vote between rotations
            // on a downscaled copy, then redo the best angle at full size.
            if (textScore(text) < MIN_ACCEPT_SCORE) {
                val probe = downscale(prepared, PROBE_DIMENSION)
                var bestAngle = 0
                var bestScore = textScore(text)
                for (angle in intArrayOf(90, 180, 270)) {
                    val rotatedProbe = rotate(probe, angle)
                    val score = textScore(runPass(instance, rotatedProbe))
                    rotatedProbe.recycle()
                    if (score > bestScore) {
                        bestScore = score
                        bestAngle = angle
                    }
                }
                if (probe != prepared) probe.recycle()

                if (bestAngle != 0) {
                    val rotated = rotate(prepared, bestAngle)
                    text = runPass(instance, rotated)
                    rotated.recycle()
                }
            }
            text
        } finally {
            instance.clear()
            if (prepared != bitmap) prepared.recycle()
        }
    }

    private fun runPass(instance: TessBaseAPI, bitmap: Bitmap): String {
        instance.pageSegMode = TessBaseAPI.PageSegMode.PSM_AUTO
        instance.setImage(bitmap)
        val text = instance.getUTF8Text().orEmpty().trim()
        instance.clear()
        return text
    }

    /** Rough "real text" score: Hebrew letters count double, then Latin/digits. */
    private fun textScore(text: String): Int {
        var score = 0
        for (c in text) {
            when {
                c in 'א'..'ת' -> score += 2
                c.isLetterOrDigit() -> score += 1
            }
        }
        return score
    }

    private fun rotate(src: Bitmap, degrees: Int): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
    }

    private fun downscale(src: Bitmap, maxDim: Int): Bitmap {
        val longest = maxOf(src.width, src.height)
        if (longest <= maxDim) return src
        val scale = maxDim.toFloat() / longest
        return Bitmap.createScaledBitmap(
            src,
            (src.width * scale).toInt().coerceAtLeast(1),
            (src.height * scale).toInt().coerceAtLeast(1),
            true,
        )
    }

    /**
     * Grayscale only — deliberately NO resizing. Measured on a real invitation:
     * even a mild bilinear upscale (1748 -> 1800) blurred thin strokes enough to
     * collapse recognition from a full page to four lines, while the unresized
     * grayscale image read fine.
     */
    private fun preprocess(src: Bitmap): Bitmap {
        val gray = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        Canvas(gray).drawBitmap(
            src,
            0f,
            0f,
            Paint().apply { colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) }) },
        )
        return gray
    }

    @Synchronized
    override fun close() {
        api?.recycle()
        api = null
    }

    private companion object {
        /** Below this score the first pass is considered failed and rotations are tried. */
        const val MIN_ACCEPT_SCORE = 60

        /** Rotation probes run on a copy this size for speed. */
        const val PROBE_DIMENSION = 1400
    }
}
