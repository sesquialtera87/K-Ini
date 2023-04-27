package org.mth.kini

import org.junit.Assert.*
import org.junit.Test
import kotlin.test.assertFails

class IniParserTest {

    private val parser = SimpleParser()

    private fun parse(string: String) = parser.parse(string)

    @Test
    fun emptyIniFile() {
        assertTrue(parse(""))
    }

    @Test
    fun variousSections() {
        assertTrue(parse("[Section 1]\n[Section 2 ]  \n\n[Section 3]"))
    }

    @Test
    fun comments() {
        assertTrue(parse("; gdgjlsjjadcf  \n\n#fxsaxs\n;#fx \r\n"))
    }

    @Test
    fun assignment() {
        assertTrue(parse("key1=198.9"))
        assertEquals("198.9", parser.ini["key1"])
        assertEquals(198.9, parser.ini.section(Ini.ROOT).getDouble("key1"), 0.0002)

        assertTrue(parse("key2  =False"))
        assertEquals("False", parser.ini["key2"])
        assertEquals(false, parser.ini.section(Ini.ROOT).getBoolean("key2"))

        assertTrue(parse("key3 = 0.004  "))
        assertEquals("0.004", parser.ini["key3"])
        assertEquals(0.004, parser.ini.section(Ini.ROOT).getDouble("key3"), 0.0002)

        // space in non quoted string
        assertFails { parse("key4 = Hello World!") }

        // space in key definition
        assertFails { parse("key 5 = 9") }

    }

    @Test
    fun sections() {
        val sample = readSample("sample.ini")
        val ini = IniParser.parse(sample)

        assertEquals(ini.sections.size, 4)

        val sectionNames = ini.sections.map { it.sectionName }.toTypedArray()
        assertArrayEquals(sectionNames, arrayOf("Numbers", "String", "Boolean", "Char"))
    }

    @Test
    fun startsWithSampleIni() {
        val sample = readSample("startsWithExample.ini")
        parse(sample)
        val ini = parser.ini

        val section = ini.section("Sample")

        assertEquals("127.0.0.1:80:8080/tcp", section["my.port.1"])
        assertEquals("127.0.0.1:70:7070/tcp", section["my.port.2"])
        assertEquals("Hello", section["string"])
        assertEquals("Henry", section["user"])
    }

    @Test
    fun sampleIni() {
        val sample = readSample("sample.ini")
        parse(sample)
        val ini = parser.ini

        val numbers = ini.section("Numbers")

        assertEquals(numbers.getDouble("double"), 3.14, 0.00001)
        assertEquals(numbers.getDouble("double2"), -3.14, 0.00001)
        assertEquals(numbers.getFloat("float"), 199.33F, 0.00001F)
        assertEquals(numbers.getInt("integer"), 404)
        assertEquals(numbers.getLong("long"), 922337203685775808)
        assertEquals(numbers.getLong("long2"), 922337203685775808L)
        assertEquals(numbers.getShort("short"), (-32768).toShort())

        val booleans = ini.section("Boolean")

        assertTrue(booleans.getBoolean("trueKey"))
        assertTrue(booleans.getBoolean("true2Key"))
        assertTrue(booleans.getBoolean("true3Key"))
        assertFalse(booleans.getBoolean("falseKey"))
        assertFalse(booleans.getBoolean("false2Key"))
        assertFalse(booleans.getBoolean("false3Key"))
    }
}