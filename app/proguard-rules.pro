# Keep any JNI entry points.
-keepclasseswithmembernames class * {
    native <methods>;
}

# Tesseract4Android / Leptonica use JNI; keep their classes.
-keep class com.googlecode.tesseract.android.** { *; }
-keep class com.googlecode.leptonica.android.** { *; }

# ML Kit ships its own consumer rules, but keep the vision entry points to be safe.
-keep class com.google.mlkit.vision.text.** { *; }

# Keep the pure-Kotlin parser models (harmless; keeps stack traces readable).
-keep class com.pic2date.parser.model.** { *; }
