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

package org.mth.kini.model

import org.junit.Test
import java.io.InputStreamReader
import kotlin.test.*

class IniScannerTest {

    private fun readIni(name: String): Ini {
        return Ini.load(InputStreamReader(IniScannerTest::class.java.getResourceAsStream(name)!!))
    }

    @Test
    fun minimal() {
        val ini = readIni("minimal.ini")

        assertTrue { ini.hasSection("Sample") }

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

        assertTrue { ini.sections.isEmpty() }
        assertEquals("abc def", ini["a"])
        assertEquals("abc \\n\\t ", ini["b"])
        assertEquals("string", ini["c"])
        assertEquals("\'malformed string", ini["d"])
        assertEquals("\"malformed string", ini["e"])
    }

    @Test
    fun sample() {
        val ini = readIni("sample.ini")

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
    fun toPropertiesWithConverter(){
        val ini = Ini.newIni {
            set("a", "2")
            set("b", "3")

            section("booleans") {
                set("a", "true")
                set("b", "false")
            }
        }

        val props = ini.toProperties { section, property -> "${section.sectionName}:$property" }
        assertEquals(4, props.size)
        assertEquals("2", props["a"])
        assertEquals("3", props["b"])

        assertEquals("true", props["booleans:a"])
        assertEquals("false", props["booleans:b"])
    }

    @Test
    fun toProperties() {
        val ini = Ini.newIni {
            set("a", "2")
            set("b", "3")

            section("booleans") {
                set("a", "true")
                set("b", "false")
            }
        }

        val props = ini.toProperties()
        assertEquals(4, props.size)
        assertEquals("2", props["a"])
        assertEquals("3", props["b"])

        assertEquals("true", props["booleans.a"])
        assertEquals("false", props["booleans.b"])
    }

    @Test
    fun merge() {
        val ini1 = Ini.newIni {
            set("a", "2")
            set("b", "19")

            section("section") {
                set("c", "3")
            }
        }

        val ini2 = Ini.newIni {
            set("a", "52")
            set("d", "22")

            section("section") {
                set("c", "3")
            }

            section("section 2") {
                set("alpha", "33.2")
            }
        }

        ini1.merge(ini2)

        assertTrue(ini1.hasSection("section 2"))
        assertTrue(ini1.hasProperty("a"))
        assertTrue(ini1.hasProperty("b"))
        assertTrue(ini1.hasProperty("d"))
        assertEquals(22, ini1.getInt("d"))
        assertEquals(52, ini1.getInt("a"))
    }

    @Test
    fun sectionsFiltering() {
        val ini = Ini.newIni {
            for (i in 0 until 4)
                this["test.id.$i"] = i.toString()

            section("section 1") {
                for (i in 0 until 2)
                    this["a.b.string.$i"] = i.toString()
            }
        }

        assertContentEquals(listOf("id"), ini.getSub("test"))
        assertContentEquals(listOf("0", "1", "2", "3"), ini.getSub("test.id"))
        assertTrue(ini.getSub("test.child").isEmpty())

        assertContentEquals(listOf("b"), ini.getSub("section 1", "a"))
        assertContentEquals(listOf("0", "1"), ini.getSub("section 1", "a.b.string"))
    }
}