package org.mth.kini

import org.junit.Assert.*
import org.junit.Test
import org.parboiled.errors.ErrorUtils
import org.parboiled.parser.Parboiled
import org.parboiled.parserunners.ReportingParseRunner

class IniParserTest {

    @Test
    fun emptyIniFile() {
        assertTrue(SimpleParser().parse(""))
    }

    @Test
    fun variousSections() {
        assertTrue(SimpleParser().parse("[Section 1]\n[Section 2 ]  \n\n[Section 3]"))
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
    fun sampleIni() {
        val sample = readSample("sample.ini")
        val ini = IniParser.parse(sample)

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