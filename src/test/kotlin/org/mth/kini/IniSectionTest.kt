package org.mth.kini

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class IniSectionTest {

    private lateinit var section: IniSection

    @BeforeEach
    fun setUp() {
        section = IniSection("database")
    }

    @Test
    fun testBasicPropertyManagement() {
        assertTrue(section.isEmpty())
        assertEquals(0, section.propertyCount)

        section.setProperty("host", "localhost")
        section["port"] = "5432"

        assertFalse(section.isEmpty())
        assertEquals(2, section.propertyCount)
        assertTrue(section.hasProperty("host"))
        assertEquals("localhost", section["host"])
        assertEquals("5432", section.get("port", "8080"))
        assertEquals("fallback", section.get("missing", "fallback"))

        // Test views keys e values
        assertEquals(setOf("host", "port"), section.keys)
        assertTrue(section.values.containsAll(listOf("localhost", "5432")))

        section.removeProperty("host")
        assertFalse(section.hasProperty("host"))

        section.clear()
        assertTrue(section.isEmpty())
    }

    @Test
    fun testBooleanParsing() {
        section["enabled"] = "true"
        section["disabled"] = "false"
        section["invalid"] = "not_a_boolean"

        assertTrue(section.getBoolean("enabled"))
        assertFalse(section.getBoolean("disabled"))
        assertFalse(section.getBoolean("invalid"))

        assertTrue(section.getBoolean("enabled", false))
        assertTrue(section.getBoolean("missing", true))
    }

    @Test
    fun testNumericParsing() {
        section["intVal"] = "42"
        section["invalidInt"] = "abc"
        section["longVal1"] = "123456789L"
        section["longVal2"] = "987654321l"
        section["longVal3"] = "555"
        section["doubleVal"] = "3.14"
        section["floatVal"] = "2.5"

        // Int
        assertEquals(42, section.getInt("intVal"))
        assertEquals(42, section.getInt("intVal", 10))
        assertEquals(10, section.getInt("missing", 10))
        assertThrows<NumberFormatException> { section.getInt("invalidInt") }

        // Long
        assertEquals(123456789L, section.getLong("longVal1"))
        assertEquals(987654321L, section.getLong("longVal2"))
        assertEquals(555L, section.getLong("longVal3"))
        assertEquals(100L, section.getLong("missing", 100L))
        assertThrows<NumberFormatException> { section.getLong("invalidInt") }

        // Short
        section["shortVal"] = "5"
        assertEquals(5.toShort(), section.getShort("shortVal"))
        assertEquals(5.toShort(), section.getShort("shortVal", 0.toShort()))
        assertEquals(2.toShort(), section.getShort("missing", 2.toShort()))

        // Double & Float
        assertEquals(3.14, section.getDouble("doubleVal"))
        assertEquals(3.14, section.getDouble("doubleVal", 1.0))
        assertEquals(1.0, section.getDouble("missing", 1.0))

        assertEquals(2.5f, section.getFloat("floatVal"))
        assertEquals(2.5f, section.getFloat("floatVal", 1.0f))
        assertEquals(1.0f, section.getFloat("missing", 1.0f))
    }

    @Test
    fun testArrayParsing() {
        section["strArray"] = "[val1, val2, val3]"
        section["intArray"] = "[1, 2, 3]"
        section["notAnArray"] = "val1, val2"

        val strArr = section.getArray("strArray")
        assertArrayEquals(arrayOf("val1", "val2", "val3"), strArr)

        val intArr = section.getNumberArray("intArray", Int::class.java)
        assertArrayEquals(arrayOf(1, 2, 3), intArr)

        assertThrows<IllegalArgumentException> { section.getArray("missing") }
        assertThrows<UnsupportedOperationException> { section.getArray("notAnArray") }
        assertThrows<UnsupportedOperationException> {
            section.getNumberArray(
                "intArray",
                String::class.java as Class<Number>
            )
        }
    }

    @Test
    fun testHierarchicalNavigation() {
        section["db.mysql.host"] = "localhost"
        section["db.mysql.port"] = "3306"
        section["db.postgres.host"] = "127.0.0.1"
        section["server.timeout"] = "30"
        section["isolated"] = "value"

        // Nodes
        assertEquals(listOf("db", "server", "isolated"), section.getNodes().toList())
        assertEquals(listOf("mysql", "postgres"), section.getNodes("db").toList())

        // Group
        val mysqlGroup = section.getGroup("db.mysql", stripPrefix = true)
        assertEquals(2, mysqlGroup.size)
        assertEquals("localhost", mysqlGroup["host"])
        assertEquals("3306", mysqlGroup["port"])

        // Group senza strip
        val mysqlGroupFull = section.getGroup("db.mysql", stripPrefix = false)
        assertEquals("localhost", mysqlGroupFull["db.mysql.host"])

        // GroupByRoot
        val rootGroup = section.groupByRoot()
        assertTrue(rootGroup.containsKey("db"))
        assertTrue(rootGroup.containsKey("server"))
        assertTrue(rootGroup.containsKey(""))
        assertEquals("30", rootGroup["server"]?.get("timeout"))
        assertEquals("value", rootGroup[""]?.get("isolated"))
    }

    @Test
    fun testMapLikeDsl() {
        section.apply {
            "version" to "2.0"

            "pool" {
                "size" to 10
                "timeout" to "5s"
            }
        }

        assertEquals("2.0", section["version"])
        assertEquals("10", section["pool.size"])
        assertEquals("5s", section["pool.timeout"])
    }

    @Test
    fun testEqualsAndHashCode() {
        val section1 = IniSection("network")
        val section2 = IniSection("network")
        val section3 = IniSection("security")

        assertEquals(section1, section2)
        assertNotEquals(section1, section3)
        assertEquals(section1.hashCode(), section2.hashCode())
    }

    @Test
    fun testToStringAndIterator() {
        section["key"] = "value"
        val expected = "[database]\n\tkey=value"
        assertEquals(expected, section.toString())

        var count = 0
        for (entry in section) {
            assertEquals("key", entry.key)
            assertEquals("value", entry.value)
            count++
        }
        assertEquals(1, count)
    }
}