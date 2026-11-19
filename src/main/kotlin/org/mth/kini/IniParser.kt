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
 * Line-oriented INI parser.
 *
 * Grammar (one construct per line; lines end with `\n`, `\r\n` or `\r`):
 *
 * - blank line                      → ignored
 * - `#…` or `;…` (after optional blanks) → comment
 * - `[name]` optionally followed by a comment → section header (name is trimmed, must be non-empty)
 * - `key = value` or `key : value`  → property; the delimiter is the first `=` or `:` of the line
 *
 * A value is either:
 * - *quoted* with `"` or `'`: escapes are processed (see [unescape]) and the closing quote may be followed
 *   only by blanks and/or a comment;
 * - *unquoted*: the raw text up to an inline comment, trimmed. An inline comment starts at a `#` or `;`
 *   that is the first character of the value or is preceded by a blank, so that `pwd = a#b` keeps `a#b`.
 *
 * For backward compatibility, a value opening a quote that is never closed on the same line is kept verbatim
 * (opening quote included, no escape processing, no comment stripping).
 */
internal object IniParser {

    fun parse(text: CharSequence, target: Ini) {
        var current: IniSection = target
        var lineNumber = 0
        val length = text.length
        // Skip a byte order mark, if present
        var start = if (length > 0 && text[0] == '﻿') 1 else 0

        while (start < length) {
            var end = start
            while (end < length && text[end] != '\n' && text[end] != '\r') end++
            lineNumber++
            current = parseLine(text.substring(start, end), lineNumber, target, current)
            // Consume the line terminator: "\r\n", "\r" or "\n"
            start = if (end + 1 < length && text[end] == '\r' && text[end + 1] == '\n') end + 2 else end + 1
        }
    }

    private fun parseLine(raw: String, lineNumber: Int, ini: Ini, current: IniSection): IniSection {
        val line = raw.trim(::isBlank)
        if (line.isEmpty() || isCommentStart(line[0])) return current

        if (line[0] == '[') {
            val close = line.indexOf(']')
            if (close < 0) throw IniParseException("Unclosed section header, expected ']'", lineNumber)
            val name = line.substring(1, close).trim(::isBlank)
            if (name.isEmpty()) throw IniParseException("Empty section name", lineNumber)
            val rest = line.substring(close + 1).trimStart(::isBlank)
            if (rest.isNotEmpty() && !isCommentStart(rest[0])) {
                throw IniParseException("Unexpected text after section header: '$rest'", lineNumber)
            }
            return try {
                ini.section(name)
            } catch (e: IllegalArgumentException) {
                throw IniParseException(e.message ?: "Invalid section name", lineNumber)
            }
        }

        val delimiter = line.indexOfFirst { it == '=' || it == ':' }
        if (delimiter < 0) throw IniParseException("Expected '=' or ':' after property name", lineNumber)
        val key = line.substring(0, delimiter).trim(::isBlank)
        if (key.isEmpty()) throw IniParseException("Empty property name", lineNumber)

        val value = parseValue(line.substring(delimiter + 1).trimStart(::isBlank), lineNumber)
        try {
            current[key] = value
        } catch (e: IllegalArgumentException) {
            throw IniParseException(e.message ?: "Invalid property name", lineNumber)
        }
        return current
    }

    private fun parseValue(text: String, lineNumber: Int): String {
        if (text.isEmpty()) return ""
        val first = text[0]

        if (first == '"' || first == '\'') {
            val closing = findClosingQuote(text, first)
            if (closing < 0) {
                // Unterminated quote: lenient fallback, keep the raw text
                return text.trimEnd(::isBlank)
            }
            val rest = text.substring(closing + 1).trimStart(::isBlank)
            if (rest.isNotEmpty() && !isCommentStart(rest[0])) {
                throw IniParseException("Unexpected text after quoted value: '$rest'", lineNumber)
            }
            return unescape(text, 1, closing)
        }

        if (isCommentStart(first)) return ""
        for (i in 1 until text.length) {
            if (isCommentStart(text[i]) && isBlank(text[i - 1])) {
                return text.substring(0, i).trimEnd(::isBlank)
            }
        }
        return text.trimEnd(::isBlank)
    }

    /** Index of the quote closing the string opened at index 0, honouring backslash escapes; -1 if none. */
    private fun findClosingQuote(text: String, quote: Char): Int {
        var i = 1
        while (i < text.length) {
            when (text[i]) {
                '\\' -> i += 2
                quote -> return i
                else -> i++
            }
        }
        return -1
    }

    /**
     * Resolves escape sequences in `text[from, to)`.
     *
     * Supported: `\t \n \r \f \0 \\ \" \' \; \#` and `\uXXXX`. Any other backslash sequence is kept verbatim.
     */
    fun unescape(text: String, from: Int = 0, to: Int = text.length): String {
        val sb = StringBuilder(to - from)
        var i = from
        while (i < to) {
            val c = text[i]
            if (c != '\\' || i + 1 >= to) {
                sb.append(c)
                i++
                continue
            }
            val next = text[i + 1]
            when (next) {
                't' -> sb.append('\t')
                'n' -> sb.append('\n')
                'r' -> sb.append('\r')
                'f' -> sb.append('\u000C')
                '0' -> sb.append('\u0000')
                '\\', '"', '\'', ';', '#' -> sb.append(next)
                'u' -> {
                    val hex = if (i + 6 <= to) text.substring(i + 2, i + 6) else ""
                    if (hex.length == 4 && hex.all { it.isHexDigit() }) {
                        sb.append(hex.toInt(16).toChar())
                        i += 6
                        continue
                    }
                    sb.append(c).append(next)
                }
                else -> sb.append(c).append(next)
            }
            i += 2
        }
        return sb.toString()
    }

    private fun Char.isHexDigit() = this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'

    fun isCommentStart(c: Char) = c == '#' || c == ';'

    fun isBlank(c: Char) = c == ' ' || c == '\t'
}
