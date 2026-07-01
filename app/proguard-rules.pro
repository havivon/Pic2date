# Tesseract4Android / Leptonica use JNI; keep their classes and native methods.
-keep class com.googlecode.tesseract.android.** { *; }
-keep class com.googlecode.leptonica.android.** { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}

# ML Kit ships its own consumer rules, but keep the vision entry points to be safe.
-keep class com.google.mlkit.vision.text.** { *; }

# Keep the pure-Kotlin parser models (used via data classes, no reflection needed,
# but harmless and keeps stack traces readable).
-keep class com.pic2date.parser.model.** { *; }
