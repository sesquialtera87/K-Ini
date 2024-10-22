/*
 * MIT License
 *
 * Copyright (c) 2024 Mattia Marelli
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

import org.junit.Test
import java.io.StringReader
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IniScannerTest {

    private fun readIni(name: String): String {
        val b = StringBuilder()
        val input = Scanner(IniScannerTest::class.java.getResourceAsStream(name)!!)

        while (input.hasNext()) {
            b.append(input.nextLine()).append('\n')
        }

        return b.toString()
    }

    @Test
    fun minimal() {
        val text = readIni("minimal.ini")
        val lexer = IniScanner(StringReader(text))
        lexer.yylex()
        val ini = lexer.ini

        assertTrue { ini.hasSection("Sample") }

        val s = ini.section("Sample")
        assertEquals("127.0.0.1:80:8080/tcp", s["my.port.1"])
        assertEquals("127.0.0.1:70:7070/tcp", s["my.port.2"])
        assertEquals("Hello", s["string"])
        assertEquals("Henry", s["user"])
    }

    @Test
    fun strings() {
        val text = readIni("strings.ini")
        val lexer = IniScanner(StringReader(text))
        lexer.yylex()
        val ini = lexer.ini

        assertTrue { ini.sections.isEmpty() }
        assertEquals("abc def", ini["a"])
        assertEquals("abc \\n\\t ", ini["b"])
        assertEquals("string", ini["c"])
        assertEquals("\'malformed string", ini["d"])
        assertEquals("\"malformed string", ini["e"])
    }

    @Test
    fun sample() {
        val text = readIni("sample.ini")
        val lexer = IniScanner(StringReader(text))
        lexer.yylex()
        val ini = lexer.ini

        assertTrue { ini.hasSection("Numbers") }
        assertTrue { ini.hasSection("Boolean") }
        assertTrue { ini.hasSection("Char") }
        assertTrue { ini.hasSection("String") }

        var s = ini.section("Boolean")
        assertTrue { s.getBoolean("trueKey") }
        assertTrue { s.getBoolean("true2Key") }
        assertTrue { s.getBoolean("true3Key") }
        assertFalse { s.getBoolean("falseKey") }
        assertFalse { s.getBoolean("false2Key") }
        assertFalse { s.getBoolean("false3Key") }

        s = ini.section("Char")
        assertEquals("a", s["charKey"])
        assertEquals("b", s["characterKey"])
        assertEquals("", s["emptyChar"])

        s = ini.section("String")
        assertEquals("Henry", s["userWithComment"])
        assertEquals("Henry", s["userWithComment2"])
        assertEquals("Henry", s["user"])
        assertEquals("Hello", s["string"])
    }

    @Test
    fun merge() {
        val ini1 = Ini.newIni {
            set("a","2")
            set("b","19")

            section("section"){
                set("c","3")
            }
        }

        val ini2 = Ini.newIni {
            set("a","52")
            set("d","22")

            section("section"){
                set("c","3")
            }

            section("section 2"){
                set("alpha","33.2")
            }
        }

        ini1.merge(ini2)

        assertTrue (ini1.hasSection("section 2"))
        assertTrue (ini1.hasProperty("a"))
        assertTrue (ini1.hasProperty("b"))
        assertTrue (ini1.hasProperty("d"))
        assertEquals(22,ini1.getInt("d"))
        assertEquals(52,ini1.getInt("a"))
    }
}