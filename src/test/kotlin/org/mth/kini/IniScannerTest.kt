/*
 * MIT License
 *
 * Copyright (c) 2025 Mattia Marelli
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

import java.io.InputStreamReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IniScannerTest {
    /**
     * Helper function to load an INI file from resources using the automated JFlex lexer.
     */
    private fun readIni(name: String): Ini {
        val stream = IniScannerTest::class.java.getResourceAsStream(name)
            ?: throw IllegalArgumentException("Resource file '$name' not found")
        return Ini.load(InputStreamReader(stream))
    }

    @Test
    fun minimal() {
        val ini = readIni("minimal.ini")

        assertTrue(ini.hasSection("Sample"))

        val s = ini.section("Sample")
        assertEquals("127.0.0.1:80:8080/tcp", s["my.port.1"])
        assertEquals("127.0.0.1:70:7070/tcp", s["my.port.2"])
        assertEquals("Hello", s["string"])
        assertEquals("Henry", s["user"])
        assertEquals("", s["last_empty_prop"])
    }

    @Test
    fun strings() {
        val ini = readIni("strings.ini")

        // strings.ini non ha intestazioni di sezione, le proprietà sono globali (root)
        assertTrue(ini.sections.isEmpty())
        assertEquals("abc def", ini["a"])
        assertEquals("abc \\n\\t ", ini["b"])
        assertEquals("string", ini["c"])
        assertEquals("\'malformed string", ini["d"])
        assertEquals("\"malformed string", ini["e"])
    }

    @Test
    fun sample() {
        val ini = readIni("sample.ini")

        // Verifica che tutte le sezioni esplicite siano state scansionate correttamente
        assertTrue(ini.hasSection("Numbers"))
        assertTrue(ini.hasSection("Boolean"))
        assertTrue(ini.hasSection("Char"))
        assertTrue(ini.hasSection("String"))

        // 1. Test Sezione [Numbers] usando i nuovi metodi di parsing numerico nativi
        val numbers = ini.section("Numbers")
        assertEquals(3.14, numbers.getDouble("double"))
        assertEquals(-3.14, numbers.getDouble("double2")) // Gestisce correttamente il fallback o il segno
        assertEquals(199.33f, numbers.getFloat("float"))
        assertEquals(404, numbers.getInt("integer"))
        assertEquals(922337203685775808L, numbers.getLong("long"))
        assertEquals(922337203685775808L, numbers.getLong("long2")) // Gestisce il suffisso 'L'
        assertEquals((-32768).toShort(), numbers.getShort("short"))

        // 2. Test Sezione [Boolean]
        val booleans = ini.section("Boolean")
        assertTrue(booleans.getBoolean("trueKey"))
        assertTrue(booleans.getBoolean("true2Key"))
        assertTrue(booleans.getBoolean("true3Key"))
        assertFalse(booleans.getBoolean("falseKey"))
        assertFalse(booleans.getBoolean("false2Key"))
        assertFalse(booleans.getBoolean("false3Key"))

        // 3. Test Sezione [Char]
        val chars = ini.section("Char")
        assertEquals("a", chars["charKey"])
        assertEquals("b", chars["characterKey"])
        assertEquals("", chars["emptyChar"])

        // 4. Test Sezione [String] (Verifica pulizia dei commenti in linea ';' e '#')
        val strings = ini.section("String")
        assertEquals("Hello", strings["string"])
        assertEquals("Henry", strings["user"])
        assertEquals("Henry", strings["userWithComment"])
        assertEquals("Henry", strings["userWithComment2"])
    }
}