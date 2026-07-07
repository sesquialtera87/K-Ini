/*
 * MIT License
 *
 * Copyright (c) 2026 Mattia Marelli
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

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.readText

class IniTest {

    // JUnit 5 inietta automaticamente una cartella temporanea sicura per i test di I/O
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun testGlobalPropertiesManagement() {
        val ini = Ini()

        // Verifica assegnazione e recupero proprietà globali (ereditate da IniSection)
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

        // Crea o recupera una sezione
        val dbSection = ini.section("database")
        dbSection["host"] = "localhost"

        assertTrue(ini.hasSection("database"))
        assertEquals(1, ini.sectionCount())
        assertEquals("localhost", ini.section("database")["host"])

        // Verifica l'operatore 'contains' (in)
        assertTrue("database" in ini)
        assertFalse("network" in ini)

        // Rimozione sezione
        val removed = ini.removeSection("database")
        assertTrue(removed)
        assertEquals(0, ini.sectionCount())
        assertFalse("database" in ini)
    }

    @Test
    fun testDslSectionConfiguration() {
        val ini = Ini()

        // Test della funzione scope inline con lambda block
        ini.section("server") {
            this["port"] = "8080"
            this["protocol"] = "https"
        }

        assertTrue(ini.hasSection("server"))
        assertEquals("8080", ini.section("server")["port"])
        assertEquals("https", ini.section("server")["protocol"])
    }

    @Test
    fun testMerge() {
        val ini1 = ini {
            this["global_key"] = "value1"
            section("common") { this["keyA"] = "1" }
        }

        val ini2 = ini {
            this["global_key"] = "overwritten_value" // Sovrascriverà ini1
            this["new_global"] = "value2"
            section("common") { this["keyB"] = "2" }
            section("unique") { this["keyC"] = "3" }
        }

        ini1.merge(ini2)

        // Verifica proprietà globali dopo il merge
        assertEquals("overwritten_value", ini1["global_key"])
        assertEquals("value2", ini1["new_global"])

        // Verifica proprietà delle sezioni unite
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

        // Conversione standard (sectionName.propertyName)
        val props = ini.toProperties()
        assertEquals("true", props.getProperty("debug"))
        assertEquals("root", props.getProperty("database.user"))

        // Conversione con resolver personalizzato (es. usando l'underscore)
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

        // Test di salvataggio (store)
        ini.store(fileTarget)

        // Verifica veloce del formato testuale scritto sul disco
        val fileContent = fileTarget.readText()
        assertTrue(fileContent.contains("app_name = K-Ini-Test"))
        assertTrue(fileContent.contains("[ui]"))
        assertTrue(fileContent.contains("theme = dark"))

        // Test di caricamento (load) dal file appena generato
        val loadedIni = Ini.load(fileTarget)

        assertEquals("K-Ini-Test", loadedIni["app_name"])
        assertTrue(loadedIni.hasSection("ui"))
        assertEquals("dark", loadedIni.section("ui")["theme"])
        assertEquals("14", loadedIni.section("ui")["font_size"])
    }

    @Test
    fun testLoadOrNullWithInvalidFile() {
        val nonExistentPath = tempDir.resolve("ghost_file_404.ini")

        // Non deve lanciare eccezioni ma restituire null
        val result = Ini.loadOrNull(nonExistentPath)
        assertNull(result)
    }

    @Test
    fun testRemoveAllSections() {
        val ini = ini {
            section("sec1") {
                this["k"] = "v"
            }
            section("sec2") {
                this["k"] = "v"
            }
        }

        assertEquals(2, ini.sectionCount())
        ini.removeAllSections()
        assertEquals(0, ini.sectionCount())
        assertTrue(ini.sections.isEmpty())
    }
}