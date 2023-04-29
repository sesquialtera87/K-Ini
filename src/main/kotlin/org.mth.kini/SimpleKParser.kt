package org.mth.kini

class SimpleKParser {
    private var input: String = ""
    private var globalOffset = 0
    private var lineCount = 0
    private var ch = 0.toChar()
    private var currentSection: IniSection? = null
    lateinit var ini: Ini

    private fun consumeWhitespaces() {
        while (!EOF()) {
            ch = peek()
            if (ch == WHITESPACE || ch == '\t') consume() else break
        }
    }

    private fun peek(): Char = input[globalOffset]

    private fun consume() {
        globalOffset++
    }

    @Suppress("FunctionName")
    private fun EOF(): Boolean = globalOffset >= input.length

    private fun readNext(): Char = input[globalOffset++]

    private fun startComment() = if (!EOF()) {
        ch = peek()
        ch == SEMICOLON || ch == HASHTAG
    } else false

    @Throws(IniParseException::class)
    fun parse(content: String): Boolean {
        input = content
        globalOffset = 0
        lineCount = 1
        ini = Ini()
        currentSection = ini.section(Ini.ROOT)

        while (!EOF()) {
            ch = peek()
            if (ch == '[') {
                consume()
                if (!section()) throw IniParseException("Section", globalOffset, lineCount)
            } else if (startComment()) {
                consume()
                comment()
            } else if (Character.isWhitespace(ch)) {
                emptyLine()
            } else {
                assignment()
            }
        }
        return true
    }

    @Suppress("FunctionName")
    private fun EOL(consume: Boolean = true): Boolean {
        if (EOF()) return true

        if (peek() == '\n' || peek() == '\r') {
            if (consume) {
                consume()
                lineCount++
            }
            return true
        }
        return false
    }

    private fun emptyLine() {
        ch = peek()

        while (!EOL(false)) {
            if (!Character.isWhitespace(ch)) return
            consume()
        }

        consume()
        lineCount++
    }

    private fun comment() {
        val b = StringBuilder()

        while (!EOF()) {
            ch = peek()

            if (EOL()) {
                println("Comment -> $b")
                return
            } else {
                b.append(ch)
                consume()
            }
        }
    }

    fun section(): Boolean {
        val b = StringBuilder()

        while (!EOF()) {
            ch = peek()

            if (ch == ']') {
                consume()
                consumeWhitespaces()

                return if (EOL()) {
                    println("Section: $b")
                    currentSection = ini.section(b.toString())
                    true
                } else false
            } else if (EOL()) {
                return false
            } else {
                b.append(ch)
                consume()
            }
        }
        return true
    }

    @Throws(IniParseException::class)
    fun key(): String {
        val b = StringBuilder()

        while (!EOF()) {
            ch = peek()

            if (EOL()) {
                if (b.isEmpty()) {
                    throw RuntimeException("Strange...")
                } else throw IniParseException("Invalid assignment", globalOffset, lineCount)
            } else if (Character.isWhitespace(ch)) {
                consumeWhitespaces()
                if (peek() != EQUAL) throw IniParseException("Spaces not allowed in keys", globalOffset, lineCount)
            } else if (ch != EQUAL) {
                b.append(ch)
                consume()
            } else {
                println("key [$b]")
                break
            }
        }

        return b.toString()
    }

    /**
     * Check if the [Char] located at [globalOffset] equals the passed character [ch].
     * if match succeeded, the character is consumed and the pointer [globalOffset] incremented.
     */
    private fun match(ch: Char) = if (peek() == ch) {
        consume()
        true
    } else false

    private fun checkEscape(): Char? {
        if (globalOffset + 1 >= input.length)
            return null

        var ch: Char? = input[globalOffset + 1]

        when (ch) {
            'n' -> ch = '\n'
            'r' -> ch = '\r'
            't' -> ch = '\t'
            'f' -> ch = '\u000c'
            'b' -> ch = '\b'
            '0' -> ch = '\u0000'
            SEMICOLON, COLON, HASHTAG, '=', '\\', SINGLE_QUOTE, DOUBLE_QUOTES -> {}
            else -> ch = null
        }

        if (ch != null) {
            consume()   // consume the \ char
            consume()   // consume the char next to the \
        }

        return ch
    }

    @Throws(IniParseException::class)
    fun quotedValue(quotationChar: Char): String? {
        var stringClosed = false
        val b = StringBuilder()

        if (!match(quotationChar)) return null

        while (!EOF()) {
            ch = peek()

            if (EOL(false) && !stringClosed) {
                throw IniParseException("Unclosed string value", globalOffset, lineCount)
            } else if (ch != quotationChar) {
                if (ch == '\\') {
                    val escape = checkEscape()
                    if (escape == null) {
                        b.append(ch)
                        consume()
                    } else b.append(escape)
                } else {
                    b.append(ch)
                    consume()
                }
            } else if (match(quotationChar)) {
                stringClosed = true
                consumeWhitespaces()

                if (EOL(true)) {
                    println("Quoted Value [$b]")
                    break
                } else throw IniParseException("Invalid character after escaped string", globalOffset, lineCount)
            }
        }
        return b.toString()
    }

    @Throws(IniParseException::class)
    fun value(): String {
        val b = StringBuilder()

        while (!EOF()) {
            ch = peek()

            if (ch == DOUBLE_QUOTES || ch == SINGLE_QUOTE) {
                b.append(quotedValue(ch))
                break
            }
            if (ch == '\\') {
                val escape = checkEscape()

                if (escape == null) b.append(readNext())
                else b.append(escape)
            } else if (ch == WHITESPACE) {
                consumeWhitespaces()

                if (startComment()) {
                    consume()
                    comment()
                } else if (EOL(true))
                    break
                else
                    throw IniParseException("Whitespace in non escaped value", globalOffset, lineCount)
            } else if (startComment()) {
                println("Quoted Value [$b]")
                consume()
                comment()
            } else if (!EOL(false))
                b.append(readNext())
            else if (EOL(true)) {
                println("Value [$b]")
                break
            }
        }
        return b.toString()
    }

    @Throws(IniParseException::class)
    fun assignment() {
        val key = key()

        if (!match(EQUAL)) throw RuntimeException("Strange...")

        consumeWhitespaces()

        val value = value()

        currentSection!![key] = value
    }

    companion object {

        var DOUBLE_QUOTES = '"'
        var SINGLE_QUOTE = '\''
        var WHITESPACE = ' '
        var EQUAL = '='
        var SEMICOLON = ';'
        var COLON = ':'
        var HASHTAG = '#'

        @JvmStatic
        fun main(args: Array<String>) {
//        String content = readSample("startsWithExample.ini");
            val content = readSample("sampleWithQuotes.ini")
            val millis = System.currentTimeMillis()
            val parser = SimpleKParser()
            println(parser.parse(content))
            println(System.currentTimeMillis() - millis)
            println(parser.ini.section(Ini.ROOT))
        }
    }
}
