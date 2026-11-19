package org.mth.kini

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.*

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
        assertFailsWith<IllegalArgumentException> { section.getBoolean("invalid") }
        assertFailsWith<IllegalArgumentException> { section.getBoolean("invalid", true) }
        assertNull(section.getBooleanOrNull("invalid"))
        assertFailsWith<NoSuchElementException> { section.getBoolean("missing") }

        for (literal in listOf("true", "TRUE", "Yes", "on", "1", " true ")) {
            section["b"] = literal
            assertTrue(section.getBoolean("b"), literal)
        }
        for (literal in listOf("false", "No", "OFF", "0")) {
            section["b"] = literal
            assertFalse(section.getBoolean("b"), literal)
        }

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
        assertFails { section.getInt("invalidInt") }

        // Long
        assertEquals(123456789L, section.getLong("longVal1"))
        assertEquals(987654321L, section.getLong("longVal2"))
        assertEquals(555L, section.getLong("longVal3"))
        assertEquals(100L, section.getLong("missing", 100L))
        assertFails { section.getLong("invalidInt") }

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
    fun testGetterFlavoursAreConsistent() {
        section["good"] = " 12 "
        section["bad"] = "12x"

        // getX(name): missing -> NoSuchElementException, malformed -> NumberFormatException
        val strict: List<(String) -> Any> = listOf(
            section::getInt, section::getLong, section::getShort, section::getDouble, section::getFloat
        )
        for (getter in strict) {
            assertEquals("12", getter("good").toString().removeSuffix(".0"))
            assertFailsWith<NoSuchElementException> { getter("missing") }
            val e = assertFailsWith<NumberFormatException> { getter("bad") }
            assertTrue(e.message!!.contains("'bad'") && e.message!!.contains("[database]"), e.message)
        }

        // getX(name, default): default only when missing
        assertEquals(7, section.getInt("missing", 7))
        assertFailsWith<NumberFormatException> { section.getInt("bad", 7) }
        assertFailsWith<NumberFormatException> { section.getLong("bad", 7L) }
        assertFailsWith<NumberFormatException> { section.getShort("bad", 7) }
        assertFailsWith<NumberFormatException> { section.getDouble("bad", 7.0) }
        assertFailsWith<NumberFormatException> { section.getFloat("bad", 7f) }

        // getXOrNull(name): null when missing or malformed
        assertEquals(12, section.getIntOrNull("good"))
        assertNull(section.getIntOrNull("bad"))
        assertNull(section.getIntOrNull("missing"))
        assertEquals(12L, section.getLongOrNull("good"))
        assertNull(section.getLongOrNull("bad"))
        assertEquals(12.toShort(), section.getShortOrNull("good"))
        assertEquals(12.0, section.getDoubleOrNull("good"))
        assertEquals(12f, section.getFloatOrNull("good"))
        assertNull(section.getDoubleOrNull("bad"))
    }

    @Test
    fun testLongSuffix() {
        section["l"] = "L"
        assertFailsWith<NumberFormatException> { section.getLong("l") }
        section["l"] = "-5l"
        assertEquals(-5L, section.getLong("l"))
    }

    enum class Level { DEBUG, INFO, WARNING }

    @Test
    fun testEnumParsing() {
        section["level"] = "warning"
        section["bad"] = "verbose"

        assertEquals(Level.WARNING, section.getEnum<Level>("level"))
        assertEquals(Level.WARNING, section.getEnum("level", Level::class.java))
        assertEquals(Level.INFO, section.getEnum("missing", Level.INFO))
        assertNull(section.getEnumOrNull<Level>("bad"))
        val e = assertFailsWith<IllegalArgumentException> { section.getEnum<Level>("bad") }
        assertTrue(e.message!!.contains("DEBUG, INFO, WARNING"), e.message)
    }

    @Test
    fun testGetRequired() {
        section["a"] = "1"
        assertEquals("1", section.getRequired("a"))
        assertFailsWith<NoSuchElementException> { section.getRequired("b") }
    }

    @Test
    fun testSetAnyAndPutAll() {
        section["int"] = 42
        section["bool"] = true
        section.putAll(mapOf("x" to 1.5, "y" to "s"))
        assertEquals("42", section["int"])
        assertEquals("true", section["bool"])
        assertEquals(1.5, section.getDouble("x"))
        assertEquals("s", section["y"])
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun testArrayParsing() {
        section["strArray"] = "[val1, val2, val3]"
        section["intArray"] = "{ 1, 2, 3 }"
        section["extendedArray"] = "val1; val2"

        var strArr = section.getArray("strArray")
        assertContentEquals(arrayOf("val1", "val2", "val3"), strArr)

        strArr = section.getArray("intArray")
        assertContentEquals(arrayOf("1", "2", "3"), strArr)

        strArr = section.getArray("extendedArray", ";")
        assertContentEquals(arrayOf("val1", "val2"), strArr)

        val intArr = section.getNumberArray("intArray", Int::class.java)
            .map { it.toInt() }.toTypedArray()
        assertContentEquals(arrayOf(1,2,3), intArr)

        assertFailsWith<NoSuchElementException> { section.getArray("missing") }
        assertFails {
            section.getNumberArray(
                "intArray",
                String::class.java as Class<Number>
            )
        }
    }

    @Test
    fun testListParsing() {
        section["empty"] = "[]"
        section["blank"] = "{  }"
        section["single"] = "solo"
        section["quoted"] = """["a, b", 'c' , "  d  ", "", e]"""
        section["escaped"] = """["x\"y", 'tab\there']"""
        section["badQuote"] = """["abc, d]"""
        section["afterQuote"] = """["a" b, c]"""
        section["numbers"] = "[1, -2, 3]"
        section["mixed"] = "[1, two]"

        assertEquals(emptyList(), section.getList("empty"))
        assertEquals(emptyList(), section.getList("blank"))
        assertEquals(listOf("solo"), section.getList("single"))
        assertEquals(listOf("a, b", "c", "  d  ", "", "e"), section.getList("quoted"))
        assertEquals(listOf("x\"y", "tab\there"), section.getList("escaped"))
        assertFailsWith<IllegalArgumentException> { section.getList("badQuote") }
        assertFailsWith<IllegalArgumentException> { section.getList("afterQuote") }

        assertContentEquals(intArrayOf(1, -2, 3), section.getIntArray("numbers"))
        assertContentEquals(longArrayOf(1, -2, 3), section.getLongArray("numbers"))
        assertContentEquals(doubleArrayOf(1.0, -2.0, 3.0), section.getDoubleArray("numbers"))
        assertFailsWith<NumberFormatException> { section.getIntArray("mixed") }
        assertFailsWith<IllegalArgumentException> { section.getList("numbers", "") }
    }

    @Test
    fun testSetListRoundTrip() {
        val elements = listOf("plain", "with, comma", "  padded ", "", "\"quoted\"", "'single'", "a;b#c", "line\nbreak", "[x]")
        section.setList("list", elements)
        assertEquals(elements, section.getList("list"))

        section.setList("nums", 1, 2, 3)
        assertEquals("[1, 2, 3]", section["nums"])

        section.setList("none", emptyList())
        assertEquals(emptyList(), section.getList("none"))

        // and through a full serialize/parse cycle
        val ini = Ini()
        ini.section("s").setList("list", elements)
        assertEquals(elements, Ini.parse(ini.serialize()).section("s").getList("list"))
    }

    @Test
    fun testKeyValidation() {
        for (bad in listOf("", " a", "a ", "\ta", "a=b", "a:b", "a\nb", "a\rb", "[a", "#a", ";a")) {
            assertFailsWith<IllegalArgumentException>("key '$bad'") { section[bad] = "v" }
            assertFailsWith<IllegalArgumentException>("key '$bad'") { section.setProperty(bad, 1) }
        }
        for (good in listOf("a b", "a.b", "a#b", "a;b", "a]b", "é", "a[0]")) {
            section[good] = "v"
            assertEquals("v", section[good])
        }
    }

    @Test
    fun testSectionNameValidation() {
        for (bad in listOf("", " a", "a ", "a]b", "a\nb")) {
            assertFailsWith<IllegalArgumentException>("section '$bad'") { IniSection(bad) }
        }
        IniSection("a b.c[d")
    }

    @Test
    fun testViewsAreReadOnly() {
        section["a"] = "1"
        @Suppress("UNCHECKED_CAST")
        assertFailsWith<UnsupportedOperationException> { (section.properties() as MutableMap<String, String>)["b"] = "2" }
        assertFailsWith<UnsupportedOperationException> { (section.keys as MutableSet<String>).clear() }
        assertFailsWith<UnsupportedOperationException> { (section.values as MutableCollection<String>).clear() }
        assertFailsWith<UnsupportedOperationException> { (section.iterator() as MutableIterator<*>).apply { next(); remove() } }
        assertEquals(1, section.propertyCount)
    }

    @Test
    fun testCopyIsIndependent() {
        section["a"] = "1"
        val copy = section.copy()
        assertEquals(section, copy)
        copy["a"] = "2"
        assertEquals("1", section["a"])
        assertNotEquals(section, copy)
    }

    @Test
    fun testInsertionOrderIsPreserved() {
        val keys = listOf("z", "a", "m", "b")
        keys.forEach { section[it] = it }
        assertEquals(keys, section.keys.toList())
        section["z"] = "changed"
        assertEquals(keys, section.keys.toList())
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
        assertEquals(listOf("host", "port"), section.getNodes("db.mysql").toList())
        assertEquals(emptyList(), section.getNodes("isolated").toList())

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

        // equality takes properties into account, order excluded
        section1["a"] = "1"; section1["b"] = "2"
        assertNotEquals(section1, section2)
        section2["b"] = "2"; section2["a"] = "1"
        assertEquals(section1, section2)
        assertEquals(section1.hashCode(), section2.hashCode())

        // an Ini is never equal to a plain section
        assertNotEquals<IniSection>(Ini(), IniSection(Ini.ROOT))
    }

    @Test
    fun testToStringAndIterator() {
        section["key"] = "value"
        val expected = "[database]\n\tkey=value"
        assertEquals(expected, section.toString())

        var count = 0
        for ((key, value) in section) {
            assertEquals("key", key)
            assertEquals("value", value)
            count++
        }
        assertEquals(1, count)
    }
}