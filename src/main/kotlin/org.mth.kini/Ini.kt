@file:Suppress("unused")

package org.mth.kini

import java.io.*
import java.nio.charset.Charset
import java.nio.file.Path

class Ini {

    private val root = IniSection(ROOT)

    val sections by root::sections
    val globalPropertyCount by root::propertyCount

    /**
     * Return the global property value related to the key [name]
     */
    operator fun get(name: String): String? = root[name]

    /**
     * Set the property value related to the key [name] or add a new property if it doesn't exist
     */
    operator fun set(name: String, value: String) {
        root[name] = value
    }

    fun removeProperty(name: String) = root.removeProperty(name)

    fun hasProperty(name: String) = root.hasProperty(name)

    fun globalProperties(): Iterable<Map.Entry<String, String>> = root.properties()

    fun hasSection(name: String) = root.hasSection(name)

    fun section(name: String) = if (name == ROOT)
        root
    else
        root.section(name)

    fun removeSection(name: String) = root.removeSection(name)

    fun store(path: Path, charset: Charset = Charsets.UTF_8) {
        store(this, path, charset)
    }

    companion object {

        const val ROOT = "###root###"

        inline fun newIni(block: Ini.() -> Unit) = Ini().apply {
            block.invoke(this)
        }

        fun store(ini: Ini, path: Path, charset: Charset = Charsets.UTF_8) {
            val writer = BufferedWriter(FileWriter(path.toFile(), charset))
            var isWriterEmpty = true

            ini.root.sections
                .filter { !it.isEmpty() }
                .forEach { section ->
                    if (isWriterEmpty) {
                        isWriterEmpty = false
                    } else {
                        writer.newLine()
                    }

                    // section declaration
                    writer.write("[${section.sectionName}]\n")

                    section.properties().forEach {
                        writer.write("${it.key} = ${it.value}")
                        writer.newLine()
                    }
                }

            writer.flush()
            writer.close()
        }

        fun load(path: Path): Ini = load(FileReader(path.toFile()))

        fun load(inputStreamReader: InputStreamReader): Ini {
            val reader = BufferedReader(inputStreamReader)
            val fileContent = reader.readText()
            reader.close()
            return SimpleKParser().parse(fileContent)
        }
    }

    inline fun section(name: String, block: IniSection.() -> Unit) = section(name).apply {
        block.invoke(this)
    }
}