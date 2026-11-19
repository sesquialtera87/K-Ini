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
 * Returns `true` if [value] cannot be written verbatim (unquoted) without changing its meaning when read back:
 * leading/trailing blanks, comment markers, line breaks or other control characters, or a leading quote.
 */
internal fun needsQuoting(value: String): Boolean {
    if (value.isEmpty()) return false
    val first = value.first()
    val last = value.last()
    if (first.isWhitespace() || last.isWhitespace() || first == '"' || first == '\'') return true
    return value.any { it == '#' || it == ';' || it.isISOControl() }
}

/**
 * Quotes a string value based on the specified [IniQuoteMode] and its content.
 *
 * When quoting, double quotes are preferred; single quotes are used when the value contains double quotes
 * but no single quotes. Backslashes, the chosen quote character and control characters are escaped, so that
 * parsing the result always yields back the original [value].
 *
 * @param value The raw string value to process.
 * @param mode The [IniQuoteMode] strategy controlling when quotes should be applied.
 * @return The original [value] or a quote-enclosed, escaped representation.
 */
internal fun quoteValue(value: String, mode: IniQuoteMode): String {
    if (mode == IniQuoteMode.WHEN_NEEDED && !needsQuoting(value)) {
        return value
    }
    val quote = if ('"' in value && '\'' !in value) '\'' else '"'
    val sb = StringBuilder(value.length + 2).append(quote)
    for (c in value) {
        when {
            c == '\\' -> sb.append("\\\\")
            c == quote -> sb.append('\\').append(c)
            c == '\n' -> sb.append("\\n")
            c == '\r' -> sb.append("\\r")
            c == '\t' -> sb.append("\\t")
            c == '\u000C' -> sb.append("\\f")
            c == '\u0000' -> sb.append("\\0")
            c.isISOControl() -> sb.append("\\u").append(String.format("%04x", c.code))
            else -> sb.append(c)
        }
    }
    return sb.append(quote).toString()
}

/**
 * Ensures that [name] can be written as a property key and read back unchanged.
 *
 * @throws IllegalArgumentException if the key is empty, has leading/trailing blanks (space or tab), contains a delimiter
 * (`=`, `:`) or a line break, or starts with a character that the parser would interpret differently.
 */
internal fun requireValidKey(name: String) {
    require(name.isNotEmpty()) { "Property name must not be empty" }
    require(name.trim(IniParser::isBlank) == name) { "Property name must not have leading or trailing whitespace: '$name'" }
    require(name.none { it == '=' || it == ':' || it == '\n' || it == '\r' }) {
        "Property name must not contain '=', ':' or line breaks: '$name'"
    }
    require(name[0] != '[' && name[0] != '#' && name[0] != ';' && name[0] != '\uFEFF') {
        "Property name must not start with '[', '#' or ';': '$name'"
    }
}

/**
 * Ensures that [name] can be written as a section header and read back unchanged.
 *
 * @throws IllegalArgumentException if the name is empty, has leading/trailing blanks, or contains `]` or a line break.
 */
internal fun requireValidSectionName(name: String) {
    require(name.isNotEmpty()) { "Section name must not be empty" }
    require(name.trim(IniParser::isBlank) == name) { "Section name must not have leading or trailing whitespace: '$name'" }
    require(name.none { it == ']' || it == '\n' || it == '\r' }) {
        "Section name must not contain ']' or line breaks: '$name'"
    }
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