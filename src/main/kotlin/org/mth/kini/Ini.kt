package org.mth.kini

import java.io.*
import java.nio.charset.Charset
import java.nio.file.Path
import java.util.*
import kotlin.io.path.Path

class Ini : IniSection(ROOT) {

    private val sectionsMap: MutableMap<String, IniSection> = mutableMapOf()

    val sections: Collection<IniSection> get() = sectionsMap.values
    val globalPropertyCount by this::propertyCount

    fun sectionCount() = sectionsMap.size

    fun section(name: String): IniSection {
        return sectionsMap.getOrPut(name) { IniSection(name) }
    }


    fun globalProperties(): Map<String, String> = properties()

    fun hasSection(name: String): Boolean = sectionsMap.containsKey(name)

    fun removeAllSections() = sectionsMap.clear()

    fun removeSection(name: String) = sectionsMap.remove(name) != null

    fun getSub(sectionName: String, prefix: String): Collection<String> =
        section(sectionName).getSub(prefix)

    /**
     * Copy all properties and sections of the [ini] object into this object.
     * **WARNING:** Existing values will be overwritten
     */
    fun merge(ini: Ini) {
        ini.globalProperties().forEach { (k, v) -> this[k] = v }

        ini.sectionsMap.forEach { (name, srcSection) ->
            if (!srcSection.isEmpty()) {
                val destSection = this.section(name)
                srcSection.forEach { (k, v) -> destSection[k] = v }
            }
        }
    }

    fun toProperties(sectionResolver: (IniSection, String) -> String): Properties = Properties().apply {
        putAll(globalProperties())
        sectionsMap.values.forEach { section ->
            section.forEach { (propName, value) ->
                setProperty(sectionResolver.invoke(section, propName), value)
            }
        }
    }

    fun toProperties(): Properties = toProperties { section, propertyName -> "${section.sectionName}.$propertyName" }

    @JvmOverloads
    fun store(path: Path, charset: Charset = Charsets.UTF_8) {
        store(this, path, charset)
    }

    override fun toString(): String {
        val b = StringBuilder()

        properties.forEach { (k, v) ->
            b.append("$k = $v\n")
        }

        if (b.isNotEmpty()) {
            b.append('\n')
        }

        sectionsMap.values.filter { !it.isEmpty() }
            .forEach { section ->
                if (b.isNotEmpty()) b.append('\n')
                b.append("[${section.sectionName}]\n")
                section.forEach { (k, v) ->
                    b.append("$k = $v\n")
                }
            }

        return b.toString().trim()
    }

    companion object {
        const val ROOT = "###root###"

        @JvmStatic
        fun store(ini: Ini, path: Path, charset: Charset = Charsets.UTF_8) {
            // L'uso di .use garantisce la chiusura dello stream anche in caso di crash
            BufferedWriter(FileWriter(path.toFile(), charset)).use { writer ->
                writer.write(ini.toString())
                writer.flush()
            }
        }

        @JvmStatic
        fun load(path: String): Ini = load(Path(path))

        @JvmStatic
        fun load(file: File): Ini = load(file.toPath())

        @JvmStatic
        fun load(path: Path): Ini = FileReader(path.toFile()).use { load(it) }

        @JvmStatic
        fun load(inputStreamReader: InputStreamReader): Ini {
            return BufferedReader(inputStreamReader).use { reader ->
                val ini = Ini()
                val lexer = IniScanner(reader)
                lexer.yylex()
                lexer.ini.forEach { (section, properties) ->
                    if (section == IniScanner.DEFAULT_SECTION) {
                        properties.forEach { ini[it[0]] = it[1] }
                    } else {
                        val sec = ini.section(section)
                        properties.forEach { sec[it[0]] = it[1] }
                    }
                }
                ini
            }
        }

        @JvmStatic
        fun loadOrNull(path: Path): Ini? = try {
            load(path)
        } catch (e: Exception) {
            null
        }
    }

    inline fun section(name: String, block: IniSection.() -> Unit) = section(name).apply {
        block.invoke(this)
    }

    operator fun contains(sectionName: String): Boolean = hasSection(sectionName)
}