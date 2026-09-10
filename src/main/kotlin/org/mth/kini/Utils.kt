/*
 * MIT License
 *
 * Copyright (c) 2026 Mattia Marelli
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.mth.kini

/**
 * Specifies the quoting strategy for values in an INI file.
 */
enum class IniQuoteMode {
    /**
     * Quotes a value only if it contains characters that require escaping
     * (e.g., comments, whitespace at boundaries, or existing quotes).
     */
    WHEN_NEEDED,

    /**
     * Encloses every value in quotes regardless of its content.
     */
    ALWAYS
}

/**
 * Regular expression matching values that require quoting in INI files.
 *
 * Matches values containing comments (`#`, `;`), line breaks (`\r`, `\n`),
 * leading/trailing whitespace, or starting with quote characters (`"`, `'`).
 */
private val VALUE_NEEDS_QUOTING = Regex("[#;\\r\\n]|^\\s|\\s$|^[\"']")

/**
 * Quotes a string value based on the specified [IniQuoteMode] and its content.
 *
 * Determines whether quoting is necessary and dynamically selects single (`'`)
 * or double (`"`) quotes to prevent premature string termination when quotes are present in [value].
 *
 * @param value The raw string value to process.
 * @param mode The [IniQuoteMode] strategy controlling when quotes should be applied.
 * @return The original [value] or a quote-enclosed representation.
 */
internal fun quoteValue(value: String, mode: IniQuoteMode): String {
    if (mode == IniQuoteMode.WHEN_NEEDED && !VALUE_NEEDS_QUOTING.containsMatchIn(value)) {
        return value
    }
    // If the value already contains double quotes but no single quotes, use single quotes
    val quote = if ('"' in value && '\'' !in value) '\'' else '"'
    return "$quote$value$quote"
}

/**
 * Constructs and configures a new [Ini] instance using a type-safe builder DSL.
 *
 * @param block A lambda with receiver allowing configuration of the newly created [Ini] instance.
 * @return The configured [Ini] object.
 */
inline fun ini(block: Ini.() -> Unit): Ini = Ini().apply {
    block.invoke(this)
}