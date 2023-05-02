package org.mth.kini

import java.io.BufferedReader
import java.io.Reader
import java.io.StringReader

class CustomParser(private val reader: Reader) {

    private fun match(char: Char): Boolean {
        reader.mark(1)
        val ch = reader.read()

        return if (ch.toChar() == char) {
            true
        } else {
            reader.reset()
            false
        }
    }

    private fun consumeWhitespaces() {
        reader.mark(1000)
        var ws = 0
        var ch = reader.read()

        while (ch.toChar() == ' ') {
            ch = reader.read()
            ws++
        }

        println("Consumed $ws whitespaces")
        reader.reset()
        reader.skip(ws.toLong())
    }

    fun section(): Boolean {
        if (match('[')) {
            val builder = StringBuilder("[")
            var ch = reader.read()

            while (ch != -1 && ch.toChar() != ']') {
                builder.append(ch.toChar())
                ch = reader.read()
            }

            builder.append(ch.toChar())
            println(builder)

            return if (ch != -1) {
                consumeWhitespaces()
                match('\n') || match('\r')
            } else false
        } else return false
    }

    fun assignment(): Boolean {
        val builder = StringBuilder()
        BufferedReader(StringReader("")).apply {

        }
        while (!match('=') && !match('\n')) {
            builder.append(reader.read().toChar())
        }

        println("key=$builder")

        if (!match('=')) return false

        consumeWhitespaces()

        builder.clear()

        while (!match('\n')) {
            builder.append(reader.read().toChar())
        }

        println("value=$builder")

        return true
    }
}

fun main() {
    CustomParser(StringReader(readSample("startsWithExample.ini"))).apply {
        println(assignment())
    }

}