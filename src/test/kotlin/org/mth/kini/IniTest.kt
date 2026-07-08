package org.mth.kini

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.readText

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
}