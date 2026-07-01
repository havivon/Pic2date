package com.pic2date.parser

/** Small helpers for reasoning about Hebrew vs. Latin content. */
object TextLang {

    private val hebrewRange = '֐'..'׿'

    fun isHebrewChar(c: Char): Boolean = c in hebrewRange

    /** True when Hebrew letters outnumber Latin letters in [text]. */
    fun isPrimarilyHebrew(text: String): Boolean {
        var hebrew = 0
        var latin = 0
        for (c in text) {
            when {
                isHebrewChar(c) -> hebrew++
                c in 'a'..'z' || c in 'A'..'Z' -> latin++
            }
        }
        return hebrew > latin
    }
}
