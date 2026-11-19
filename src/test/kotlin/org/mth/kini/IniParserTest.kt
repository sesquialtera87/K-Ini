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
import kotlin.test.*

class IniParserTest {
    /**
     * Helper function to load an INI file from resources.
     */
    private fun readIni(name: String): Ini {
        val stream = IniParserTest::class.java.getResourceAsStream(name)
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
        assertEquals("abc \n\t ", ini["b"])
        assertEquals("string", ini["c"])
        assertEquals("\'malformed string", ini["d"])
        assertEquals("\"malformed string", ini["e"])
        assertEquals("\"", ini["quoted1"])
        assertEquals("\"", ini["quoted2"])
        assertEquals("'", ini["quoted3"])
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

    @Test
    fun testEdgeCaseAggressiveWhitespace() {
        val iniContent = """
            
               [   Sezione Spaziata   ]   
            
               chiave spaziata   =      valore pulito      
            
        """.trimIndent()

        val ini = Ini.load(InputStreamReader(iniContent.byteInputStream()))

        assertTrue(ini.hasSection("Sezione Spaziata"))
        // Il parser o la sezione dovrebbero applicare il trim automatico
        assertEquals("valore pulito", ini.section("Sezione Spaziata")["chiave spaziata"])
    }

    @Test
    fun testEdgeCaseDuplicateKeysAndSections() {
        val iniContent = """
            [App]
            theme = light
            timeout = 30
            
            [App]          ; Riapertura della stessa sezione
            theme = dark   ; Sovrascrittura del valore precedente
            port = 8080    ; Nuova proprietà aggiunta alla sezione esistente
        """.trimIndent()

        val ini = Ini.load(InputStreamReader(iniContent.byteInputStream()))

        assertEquals(1, ini.sectionCount()) // La sezione deve essere considerata una sola
        val app = ini.section("App")
        assertEquals("dark", app["theme"])  // Il secondo valore ha sovrascritto il primo
        assertEquals("30", app["timeout"])  // Il valore originale è stato preservato
        assertEquals(8080, app.getInt("port"))
    }

    @Test
    fun testEdgeCaseSpecialCharactersInKeys() {
        val iniContent = """
            [Special]
            my-custom_key/data = verified
            🚀_mode = active
            path.with.dots = true
        """.trimIndent()

        val ini = Ini.load(InputStreamReader(iniContent.byteInputStream()))
        val s = ini.section("Special")

        assertEquals("verified", s["my-custom_key/data"])
        assertEquals("active", s["🚀_mode"])
        assertEquals("true", s["path.with.dots"])
    }

    @Test
    fun testEdgeCaseMalformedAndEmptyArrays() {
        val iniContent = """
            [Arrays]
            empty = []
            spaces = [   ,   ]
            unclosed = [1, 2, 3
            no_brackets = 1, 2, 3
        """.trimIndent()

        val ini = Ini.load(InputStreamReader(iniContent.byteInputStream()))
        val s = ini.section("Arrays")

        assertTrue(s.getArray("empty").isEmpty())
        assertEquals(listOf("", ""), s.getList("spaces"))
        assertEquals(listOf("[1", "2", "3"), s.getList("unclosed"))
        assertContentEquals(intArrayOf(1, 2, 3), s.getIntArray("no_brackets"))
    }

    @Test
    fun testEdgeCaseNumericOverflowAndFormats() {
        val iniContent = """
            [Numbers]
            overflow_int = 2147483648
            hex_val = 0x1A
            scientific = 1e3
        """.trimIndent()

        val ini = Ini.load(InputStreamReader(iniContent.byteInputStream()))
        val s = ini.section("Numbers")

        // 2147483648 è Int.MAX_VALUE + 1. getInt deve fallire per overflow, ma getLong deve leggerlo
        assertFails { s.getInt("overflow_int") }
        assertEquals(2147483648L, s.getLong("overflow_int"))

        // Se il tuo scanner gestisce solo stringhe numeriche pure, questi lanceranno NumberFormatException.
        // È utile testarli per capire se vuoi supportare i formati esadecimali/scientifici in futuro.
        assertEquals("0x1A", s["hex_val"])
        assertEquals("1e3", s["scientific"])
    }

    // ----------------------------------------------------------------------------------------------------------
    // Grammar
    // ----------------------------------------------------------------------------------------------------------

    @Test
    fun lineTerminators() {
        for (eol in listOf("\n", "\r\n", "\r")) {
            val ini = Ini.parse(listOf("a = 1", "[s]", "b = 2", "", "c = 3").joinToString(eol))
            assertEquals("1", ini["a"], "eol=${eol.toByteArray().toList()}")
            assertEquals("2", ini.section("s")["b"])
            assertEquals("3", ini.section("s")["c"])
        }
    }

    @Test
    fun byteOrderMarkIsSkipped() {
        val ini = Ini.parse("\uFEFFkey = value")
        assertEquals(setOf("key"), ini.globalProperties().keys)
    }

    @Test
    fun blankAndWhitespaceOnlyLines() {
        val ini = Ini.parse("a = 1\n   \n\t\n[s]\n  \nb=2\n   ")
        assertEquals("1", ini["a"])
        assertEquals("2", ini.section("s")["b"])
    }

    @Test
    fun emptyInput() {
        val ini = Ini.parse("")
        assertTrue(ini.isEmpty())
        assertEquals(0, ini.sectionCount())
    }

    @Test
    fun delimiters() {
        val ini = Ini.parse("a = x=y\nb : c=d\nc= a:b\nd:\ne=")
        assertEquals("x=y", ini["a"])  // only the first delimiter splits
        assertEquals("c=d", ini["b"])
        assertEquals("a:b", ini["c"])
        assertEquals("", ini["d"])
        assertEquals("", ini["e"])
    }

    @Test
    fun inlineCommentsRequireAPrecedingBlank() {
        val ini = Ini.parse(
            """
            url = http://host/path#anchor
            pwd = abc;def
            a = value # comment
            b = value ; comment
            c = value	;tab before comment
            d = ; only a comment
            e = #
            """.trimIndent()
        )
        assertEquals("http://host/path#anchor", ini["url"])
        assertEquals("abc;def", ini["pwd"])
        assertEquals("value", ini["a"])
        assertEquals("value", ini["b"])
        assertEquals("value", ini["c"])
        assertEquals("", ini["d"])
        assertEquals("", ini["e"])
    }

    @Test
    fun commentsAfterSectionAndQuotedValues() {
        val ini = Ini.parse("[s] ; comment\na = \"x # y\" # real comment\nb='z';c")
        assertEquals("x # y", ini.section("s")["a"])
        assertEquals("z", ini.section("s")["b"])
    }

    @Test
    fun indentedLines() {
        val ini = Ini.parse("  [s]\n\t a = 1\n    # comment\n    ; comment")
        assertEquals("1", ini.section("s")["a"])
        assertEquals(1, ini.section("s").propertyCount)
    }

    @Test
    fun quotedValuesPreserveWhitespace() {
        val ini = Ini.parse("a = \"  x  \"\nb = '\t'\nc = \"\"")
        assertEquals("  x  ", ini["a"])
        assertEquals("\t", ini["b"])
        assertEquals("", ini["c"])
    }

    @Test
    fun escapeSequences() {
        val ini = Ini.parse(
            """a = "t\tn\nr\rf\f0\0b\\q\"s\'c\;h\#"
               |u = "\u00e8\u20AC"
               |bad = "\x \u12 \uZZZZ"
               |raw = C:\new\table""".trimMargin()
        )
        assertEquals("t\tn\nr\rf\u000C0\u0000b\\q\"s'c;h#", ini["a"])
        assertEquals("è€", ini["u"])
        assertEquals("\\x \\u12 \\uZZZZ", ini["bad"])
        assertEquals("C:\\new\\table", ini["raw"])  // no escape processing outside quotes
    }

    @Test
    fun unterminatedQuoteIsKeptVerbatim() {
        val ini = Ini.parse("a = \"abc ; not a comment\nb = '\nc = \"x\\\"")
        assertEquals("\"abc ; not a comment", ini["a"])
        assertEquals("'", ini["b"])
        assertEquals("\"x\\\"", ini["c"])
    }

    @Test
    fun sectionsAreNotConfusedWithRoot() {
        val ini = Ini.parse("g = 0\n[${Ini.ROOT}]\nk = 1")
        assertEquals(mapOf("g" to "0"), ini.globalProperties())
        assertEquals("1", ini.section(Ini.ROOT)["k"])
    }

    @Test
    fun sectionNameMayContainOpeningBracket() {
        val ini = Ini.parse("[[a b]\nk=v")
        assertEquals("v", ini.section("[a b")["k"])
    }

    @Test
    fun malformedInputReportsLineNumber() {
        val cases = mapOf(
            "a = 1\njust a line\n" to 2,
            "a = 1\n\n= value" to 3,
            "[unclosed\nk=v" to 1,
            "k = v\n[]" to 2,
            "[s] trailing\n" to 1,
            "a = \"x\" y" to 1,
            "a = 1\r\nb = 2\r\noops" to 3,
        )
        for ((text, line) in cases) {
            val e = assertFailsWith<IniParseException>(text) { Ini.parse(text) }
            assertEquals(line, e.lineNumber, text)
        }
    }

    @Test
    fun loadFromReaderDoesNotCloseIt() {
        var closed = false
        val reader = object : java.io.StringReader("a = 1") {
            override fun close() {
                closed = true
                super.close()
            }
        }
        assertEquals("1", Ini.load(reader)["a"])
        assertFalse(closed)
    }
}