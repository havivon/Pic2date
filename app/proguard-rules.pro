# Keep any JNI entry points.
-keepclasseswithmembernames class * {
    native <methods>;
}

# ML Kit ships its own consumer rules, but keep the vision entry points to be safe.
-keep class com.google.mlkit.vision.text.** { *; }

# Keep the pure-Kotlin parser models (harmless; keeps stack traces readable).
-keep class com.pic2date.parser.model.** { *; }
