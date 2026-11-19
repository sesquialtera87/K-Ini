package org.mth.kini

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayOutputStream
import java.io.StringWriter
import java.nio.charset.MalformedInputException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readBytes
import kotlin.io.path.readText
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText
import kotlin.random.Random
import kotlin.test.*

class IniTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun testGlobalPropertiesManagement() {
        val ini = Ini()

        ini["version"] = "1.0.2"
        ini["author"] = "Mattia"

        assertEquals("1.0.2", ini["version"])
        assertEquals("Mattia", ini["author"])
        assertEquals(2, ini.globalPropertyCount)

        val globals = ini.globalProperties()
        assertTrue(globals.containsKey("version"))
        assertTrue(globals.containsKey("author"))
    }

    @Test
    fun testSectionsManagement() {
        val ini = Ini()

        assertFalse(ini.hasSection("database"))
        assertEquals(0, ini.sectionCount())

        val dbSection = ini.section("database")
        dbSection["host"] = "localhost"

        assertTrue(ini.hasSection("database"))
        assertEquals(1, ini.sectionCount())
        assertEquals("localhost", ini.section("database")["host"])

        assertTrue("database" in ini)
        assertFalse("network" in ini)

        val removed = ini.removeSection("database")
        assertTrue(removed)
        assertEquals(0, ini.sectionCount())
        assertFalse("database" in ini)
    }

    @Test
    fun testDslSectionConfiguration() {
        val ini = Ini()

        ini.section("server") {
            this["port"] = "8080"
            this["protocol"] = "https"
        }

        assertTrue(ini.hasSection("server"))
        assertEquals("8080", ini.section("server")["port"])
        assertEquals("https", ini.section("server")["protocol"])
    }

    @Test
    fun testGetSectionNodes() {
        val ini = ini {
            section("db.mysql") { this["user"] = "root" }
            section("db.postgres") { this["user"] = "postgres" }
            section("server.api.v1") { this["timeout"] = "10" }
            section("standalone") { this["key"] = "value" }
        }

        // Test senza prefisso: deve estrarre i primi token assoluti delle sezioni
        val rootNodes = ini.getSectionNodes()
        assertEquals(3, rootNodes.size)
        assertTrue(rootNodes.containsAll(listOf("db", "server", "standalone")))

        // Test con prefisso "db": deve isolare i nodi figli immediati
        val dbNodes = ini.getSectionNodes("db")
        assertEquals(2, dbNodes.size)
        assertTrue(dbNodes.containsAll(listOf("mysql", "postgres")))

        // Test con prefisso "server": deve isolare il nodo intermedio "api"
        val serverNodes = ini.getSectionNodes("server")
        assertEquals(1, serverNodes.size)
        assertEquals("api", serverNodes.first())

        // Test con prefisso inesistente: deve ritornare una lista vuota
        val emptyNodes = ini.getSectionNodes("ghost")
        assertTrue(emptyNodes.isEmpty())
    }

    @Test
    fun testGetSectionGroup() {
        val ini = ini {
            section("modules.auth") { this["enabled"] = "true" }
            section("modules.payment") { this["gateway"] = "stripe" }
            section("network") { this["ip"] = "127.0.0.1" }
        }

        // Isola il gruppo "modules" rimuovendo il prefisso dai nomi delle sezioni risultanti
        val modulesGroup = ini.getSectionGroup("modules")
        assertEquals(2, modulesGroup.size)
        assertTrue(modulesGroup.containsKey("auth"))
        assertTrue(modulesGroup.containsKey("payment"))
        assertFalse(modulesGroup.containsKey("network"))

        assertEquals("true", modulesGroup["auth"]?.get("enabled"))
        assertEquals("stripe", modulesGroup["payment"]?.get("gateway"))
    }

    @Test
    fun testGroupBySectionRoot() {
        val ini = ini {
            section("db.mysql") { this["port"] = "3306" }
            section("db.oracle") { this["port"] = "1521" }
            section("server.http") { this["port"] = "80" }
            section("isolated") { this["key"] = "val" }
        }

        val grouped = ini.groupBySectionRoot()

        // Verifica la presenza dei raggruppamenti principali delle radici
        assertTrue(grouped.containsKey("db"))
        assertTrue(grouped.containsKey("server"))
        assertTrue(grouped.containsKey("")) // Le sezioni senza punti finiscono sotto ""

        // Controlla il sotto-ramo "db"
        val dbGroup = grouped["db"]!!
        assertEquals(2, dbGroup.size)
        assertEquals("3306", dbGroup["mysql"]?.get("port"))
        assertEquals("1521", dbGroup["oracle"]?.get("port"))

        // Controlla il sotto-ramo "server"
        val serverGroup = grouped["server"]!!
        assertEquals("80", serverGroup["http"]?.get("port"))

        // Controlla la radice vuota ""
        val standaloneGroup = grouped[""]!!
        assertEquals("val", standaloneGroup["isolated"]?.get("key"))
    }

    @Test
    fun testMerge() {
        val ini1 = ini {
            this["global_key"] = "value1"
            section("common") { this["keyA"] = "1" }
        }

        val ini2 = ini {
            this["global_key"] = "overwritten_value"
            this["new_global"] = "value2"
            section("common") { this["keyB"] = "2" }
            section("unique") { this["keyC"] = "3" }
        }

        ini1.merge(ini2)

        assertEquals("overwritten_value", ini1["global_key"])
        assertEquals("value2", ini1["new_global"])
        assertEquals("1", ini1.section("common")["keyA"])
        assertEquals("2", ini1.section("common")["keyB"])
        assertEquals("3", ini1.section("unique")["keyC"])
    }

    @Test
    fun testToPropertiesConversion() {
        val ini = ini {
            this["debug"] = "true"
            section("database") {
                this["user"] = "root"
            }
        }

        val props = ini.toProperties()
        assertEquals("true", props.getProperty("debug"))
        assertEquals("root", props.getProperty("database.user"))

        val customProps = ini.toProperties { section, key -> "${section.sectionName}_$key" }
        assertEquals("root", customProps.getProperty("database_user"))
    }

    @Test
    fun testStoreAndLoad() {
        val ini = ini {
            this["app_name"] = "K-Ini-Test"
            section("ui") {
                this["theme"] = "dark"
                this["font_size"] = "14"
            }
        }

        val fileTarget = tempDir.resolve("config.ini")

        ini.store(fileTarget)

        val fileContent = fileTarget.readText()
        assertTrue(fileContent.contains("app_name = K-Ini-Test"))
        assertTrue(fileContent.contains("[ui]"))
        assertTrue(fileContent.contains("theme = dark"))

        val loadedIni = Ini.load(fileTarget)

        assertEquals("K-Ini-Test", loadedIni["app_name"])
        assertTrue(loadedIni.hasSection("ui"))
        assertEquals("dark", loadedIni.section("ui")["theme"])
        assertEquals("14", loadedIni.section("ui")["font_size"])
    }

    @Test
    fun testLoadOrNullWithInvalidFile() {
        val nonExistentPath = tempDir.resolve("ghost_file_404.ini")

        val result = Ini.loadOrNull(nonExistentPath)
        assertNull(result)
    }

    @Test
    fun testRemoveAllSections() {
        val ini = ini {
            section("sec1") { this["k"] = "v" }
            section("sec2") { this["k"] = "v" }
        }

        assertEquals(2, ini.sectionCount())
        ini.removeAllSections()
        assertEquals(0, ini.sectionCount())
        assertTrue(ini.sections.isEmpty())
    }

    @Test
    fun testLoadOrNullWithMalformedFile() {
        val file = tempDir.resolve("bad.ini")
        file.writeText("[unclosed")
        assertNull(Ini.loadOrNull(file))
        assertFailsWith<IniParseException> { Ini.load(file) }
    }

    @Test
    fun testCharsets() {
        val text = "nome = perché €\n"
        val utf8 = tempDir.resolve("utf8.ini").apply { writeText(text, Charsets.UTF_8) }
        val latin1 = tempDir.resolve("latin1.ini").apply { writeBytes("nome = perché\n".toByteArray(Charsets.ISO_8859_1)) }

        assertEquals("perché €", Ini.load(utf8)["nome"])
        assertEquals("perché", Ini.load(latin1, Charsets.ISO_8859_1)["nome"])
        // Latin-1 bytes are not valid UTF-8: fail loudly instead of returning mangled text
        assertFailsWith<MalformedInputException> { Ini.load(latin1) }

        val ini = Ini().apply { this["k"] = "è" }
        val out = tempDir.resolve("out.ini")
        ini.store(out, Charsets.ISO_8859_1)
        assertContentEquals("k = è\n".toByteArray(Charsets.ISO_8859_1), out.readBytes())
    }

    @Test
    fun testStoreEndsWithNewlineAndToWriterAndStream() {
        val ini = ini {
            this["a"] = "1"
            section("s") { this["b"] = "2" }
        }
        val expected = "a = 1\n\n[s]\nb = 2\n"

        val file = tempDir.resolve("x.ini")
        ini.store(file)
        assertEquals(expected, file.readText())

        val writer = StringWriter()
        ini.store(writer)
        assertEquals(expected, writer.toString())

        val stream = ByteArrayOutputStream()
        ini.store(stream)
        assertEquals(expected, stream.toString(Charsets.UTF_8))
        assertEquals(ini, Ini.load(stream.toByteArray().inputStream()))
    }

    @Test
    fun testSerializeLayout() {
        assertEquals("", Ini().serialize())
        assertEquals("[s]\nk = v", ini { section("s") { this["k"] = "v" } }.serialize())
        assertEquals("g = 1", ini { this["g"] = "1"; section("empty") {} }.serialize())
        assertEquals(
            "a = \"1\"\n\n[s]\nk = \"v\"",
            ini { this["a"] = 1; section("s") { this["k"] = "v" } }.serialize(IniQuoteMode.ALWAYS)
        )
        assertEquals("a = ''", ini { this["a"] = "" }.serialize(IniQuoteMode.ALWAYS).replace('"', '\''))
    }

    @Test
    fun testSerializeQuotesOnlyWhenNeeded() {
        val values = mapOf(
            "plain" to "plain",
            "inner space" to "inner space",
            "C:\\dir" to "C:\\dir",
            " lead" to "\" lead\"",
            "trail " to "\"trail \"",
            "a#b" to "\"a#b\"",
            "a;b" to "\"a;b\"",
            "\"q" to "'\"q'",
            "'q" to "\"'q\"",
            "\"'" to "\"\\\"'\"",
            "x\ny" to "\"x\\ny\"",
            "\\ #" to "\"\\\\ #\"",
            "\u0001" to "\"\\u0001\"",
        )
        for ((value, expected) in values) {
            assertEquals("k = $expected", ini { this["k"] = value }.serialize(), value)
        }
    }

    @Test
    fun testSectionOrNullDoesNotCreate() {
        val ini = Ini()
        assertNull(ini.sectionOrNull("s"))
        assertFalse(ini.hasSection("s"))
        ini.section("s")
        assertNotNull(ini.sectionOrNull("s"))
    }

    @Test
    fun testSectionsViewIsReadOnly() {
        val ini = ini { section("s") { this["k"] = "v" } }
        assertFailsWith<UnsupportedOperationException> { (ini.sections as MutableCollection<IniSection>).clear() }
        assertEquals(1, ini.sectionCount())
    }

    @Test
    fun testCopyIsDeep() {
        val ini = ini {
            this["g"] = "1"
            section("s") { this["k"] = "v" }
        }
        val copy = ini.copy()
        assertEquals(ini, copy)
        assertEquals(ini.hashCode(), copy.hashCode())

        copy.section("s")["k"] = "changed"
        copy["g"] = "2"
        copy.section("new")["x"] = "y"
        assertEquals("v", ini.section("s")["k"])
        assertEquals("1", ini["g"])
        assertFalse(ini.hasSection("new"))
        assertNotEquals(ini, copy)
    }

    @Test
    fun testEquality() {
        assertEquals(Ini(), Ini())
        assertNotEquals(Ini(), ini { this["a"] = "1" })
        assertNotEquals(Ini(), ini { section("s") { this["a"] = "1" } })
        // different order, same content
        val a = ini { section("x") { this["k"] = "1" }; section("y") { this["k"] = "2" } }
        val b = ini { section("y") { this["k"] = "2" }; section("x") { this["k"] = "1" } }
        assertEquals(a, b)
    }

    @Test
    fun testSectionOrderIsPreserved() {
        val names = listOf("zeta", "alpha", "mid")
        val ini = Ini()
        names.forEach { ini.section(it)["k"] = it }
        assertEquals(names, Ini.parse(ini.serialize()).sections.map { it.sectionName })
    }

    @Test
    fun testParsedFileWithGlobalsAndSections() {
        val ini = Ini.parse(
            """
            ; global settings
            name = demo
            debug: yes

            [server]
            host = 0.0.0.0
            port = 8080
            ports = [80, 443]

            [server.tls]
            enabled = on
            """.trimIndent()
        )
        assertEquals("demo", ini["name"])
        assertTrue(ini.getBoolean("debug"))
        assertEquals(8080, ini.section("server").getInt("port"))
        assertContentEquals(intArrayOf(80, 443), ini.section("server").getIntArray("ports"))
        assertTrue(ini.section("server.tls").getBoolean("enabled"))
        assertEquals(listOf("tls"), ini.getSectionNodes("server"))
    }

    // ------------------------------------------------------------------------------------------------------
    // Property: parse(serialize(ini)) == ini, for any representable content
    // ------------------------------------------------------------------------------------------------------

    // Single code points (the emoji is a surrogate pair: lone surrogates cannot be encoded in UTF-8)
    private val tricky = "  \t\"'\\#;=:[]{}\n\r\u0000\u0001\u000C\u007F\u00A0\uFEFFaZ09è€.,"
        .map { it.toString() } + "\uD83D\uDE80"

    private fun Random.randomString(maxLength: Int): String {
        val length = nextInt(maxLength + 1)
        return buildString {
            repeat(length) {
                when (nextInt(4)) {
                    0 -> append(tricky.random(this@randomString))
                    1 -> append(nextInt(0x20, 0x7F).toChar())
                    2 -> append(nextInt(0, 0xD800).toChar())
                    else -> append(('a'..'z').random(this@randomString))
                }
            }
        }
    }

    private fun Random.randomKey(): String {
        while (true) {
            val candidate = randomString(8)
            if (runCatching { IniSection("x")[candidate] = "" }.isSuccess) return candidate
        }
    }

    private fun Random.randomSectionName(): String {
        while (true) {
            val candidate = randomString(8)
            if (runCatching { IniSection(candidate) }.isSuccess) return candidate
        }
    }

    private fun Random.randomIni(): Ini = Ini().apply {
        repeat(nextInt(4)) { this[randomKey()] = randomString(12) }
        repeat(nextInt(4)) {
            val section = section(randomSectionName())
            repeat(1 + nextInt(4)) { section[randomKey()] = randomString(12) }
        }
    }

    @Test
    fun testRandomRoundTrip() {
        val random = Random(20261119)
        repeat(5_000) { iteration ->
            val ini = random.randomIni()
            for (mode in IniQuoteMode.values()) {
                val text = ini.serialize(mode)
                val parsed = try {
                    Ini.parse(text)
                } catch (e: IniParseException) {
                    fail("iteration $iteration, mode $mode: ${e.message}\n$text")
                }
                assertEquals(ini, parsed, "iteration $iteration, mode $mode\n$text")
            }
        }
    }

    @Test
    fun testRandomListRoundTrip() {
        val random = Random(42)
        val section = IniSection("s")
        repeat(5_000) {
            val elements = List(random.nextInt(5)) { random.randomString(6) }
            section.setList("l", elements)
            assertEquals(elements, section.getList("l"), section["l"])
        }
    }

    @Test
    fun testStoreLoadRoundTripOnDisk() {
        val random = Random(7)
        repeat(50) {
            val ini = random.randomIni()
            val file = Files.createTempFile(tempDir, "rt", ".ini")
            ini.store(file)
            assertEquals(ini, Ini.load(file))
        }
    }
}