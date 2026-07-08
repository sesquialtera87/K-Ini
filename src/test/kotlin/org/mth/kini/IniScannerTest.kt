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

import org.junit.jupiter.api.assertThrows
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

//    @Test
//    fun testEdgeCaseSpecialCharactersInKeys() {
//        val iniContent = """
//            [Special]
//            my-custom_key/data = verified
//            🚀_mode = active
//            path.with.dots = true
//        """.trimIndent()
//
//        val ini = Ini.load(InputStreamReader(iniContent.byteInputStream()))
//        val s = ini.section("Special")
//
//        assertEquals("verified", s["my-custom_key/data"])
//        assertEquals("active", s["🚀_mode"])
//        assertEquals("true", s["path.with.dots"])
//    }

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

        // 1. Array vuoto
        val emptyArr = s.getArray("empty")
        assertTrue(emptyArr.isEmpty() || (emptyArr.size == 1 && emptyArr[0].isEmpty()))

        // 2. Strutture malformate devono lanciare l'eccezione corretta impostata in IniSection
        assertThrows<UnsupportedOperationException> { s.getArray("unclosed") }
        assertThrows<UnsupportedOperationException> { s.getArray("no_brackets") }
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
        assertThrows<NumberFormatException> { s.getInt("overflow_int") }
        assertEquals(2147483648L, s.getLong("overflow_int"))

        // Se il tuo scanner gestisce solo stringhe numeriche pure, questi lanceranno NumberFormatException.
        // È utile testarli per capire se vuoi supportare i formati esadecimali/scientifici in futuro.
        assertEquals("0x1A", s["hex_val"])
        assertEquals("1e3", s["scientific"])
    }
}