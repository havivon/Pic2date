package com.pic2date.parser

/** Extracts a phone number, tolerating Israeli and international formats. */
object ContactExtractor {

    // +972-50-123-4567, 050-1234567, 03-1234567, (03) 123-4567, 052 123 4567
    private val phone = Regex(
        """(?<![\d/.\-])(\+?\d[\d\-\s().]{6,15}\d)(?![\d/.])""",
    )

    fun extractPhone(text: String): String? {
        for (m in phone.findAll(text)) {
            val raw = m.value.trim()
            val digits = raw.count { it.isDigit() }
            // Israeli mobile/landline & international numbers land in this range.
            if (digits in 9..13) return normalize(raw)
        }
        return null
    }

    private fun normalize(raw: String): String =
        raw.replace(Regex("""\s{2,}"""), " ").trim()
}
