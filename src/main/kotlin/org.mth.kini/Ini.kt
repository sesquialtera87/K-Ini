/*
 * MIT License
 *
 * Copyright (c) 2023 Mattia Marelli
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

@file:Suppress("unused")

package org.mth.kini

import java.io.*
import java.nio.charset.Charset
import java.nio.file.Path
import kotlin.io.path.Path

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

    /**
     * Get the number of the sections contained in the INI. The number does not include the global section, so
     * the number counts exactly the explicitly declared sections.
     */
    fun sectionCount() = sections.size

    fun removeProperty(name: String) = root.removeProperty(name)

    fun removeAllProperties() = root.clear()

    fun hasProperty(name: String) = root.hasProperty(name)

    fun globalProperties(): Iterable<Map.Entry<String, String>> = root.properties()

    fun hasSection(name: String) = root.hasSection(name)

    fun section(name: String) = if (name == ROOT)
        root
    else
        root.section(name)

    fun removeAllSections() = root.clear()

    fun removeSection(name: String) = root.removeSection(name)

    fun getBoolean(name: String) = root.getBoolean(name)

    fun getInt(name: String): Int = root.getInt(name)

    fun getLong(name: String): Long = root.getLong(name)

    fun getShort(name: String): Short = root.getShort(name)

    fun getDouble(name: String): Double = root.getDouble(name)

    fun getFloat(name: String): Float = root.getFloat(name)

    fun get(name: String, defaultValue: String): String = if (root.hasProperty(name)) get(name)!! else defaultValue

    fun getBoolean(name: String, defaultValue: Boolean): Boolean =
        if (root.hasProperty(name)) getBoolean(name) else defaultValue

    fun getInt(name: String, defaultValue: Int): Int = if (root.hasProperty(name)) getInt(name) else defaultValue

    fun getLong(name: String, defaultValue: Long): Long = if (root.hasProperty(name)) getLong(name) else defaultValue

    fun getShort(name: String, defaultValue: Short): Short =
        if (root.hasProperty(name)) getShort(name) else defaultValue

    fun getDouble(name: String, defaultValue: Double): Double =
        if (root.hasProperty(name)) getDouble(name) else defaultValue

    fun getFloat(name: String, defaultValue: Float): Float =
        if (root.hasProperty(name)) getFloat(name) else defaultValue

    /**
     * Copy all properties and sections of the [ini] object into this object.
     *
     * **WARNING:** Existing values will be overwritten
     */
    fun merge(ini: Ini) {
        // put all the properties into this INI object
        ini.globalProperties().forEach { this[it.key] = it.value }

        // copy all sections
        ini.sections.forEach { section ->
            if (section.isEmpty().not()) {
                this.section(section.sectionName) {
                    section.properties().forEach { section[it.key] = it.value }
                }
            }
        }
    }

    @JvmOverloads
    fun store(path: Path, charset: Charset = Charsets.UTF_8) {
        store(this, path, charset)
    }

    override fun toString(): String {
        val b = StringBuilder()

        root.properties().forEach {
            b.append("${it.key} = ${it.value}")
            b.append('\n')
        }

        // add one empty line between global properties and the sections
        if (b.isNotEmpty())
            b.append('\n')

        root.sections
            .filter { !it.isEmpty() }
            .forEach { section ->
                if (b.isNotEmpty()) {
                    b.append('\n')
                }

                // section declaration
                b.append("[${section.sectionName}]\n")

                section.properties().forEach {
                    b.append("${it.key} = ${it.value}")
                    b.append('\n')
                }
            }

        return b.toString()
    }

    companion object {

        const val ROOT = "###root###"

        @JvmStatic
        inline fun newIni(block: Ini.() -> Unit) = Ini().apply {
            block.invoke(this)
        }

        @JvmStatic
        fun store(ini: Ini, path: Path, charset: Charset = Charsets.UTF_8) {
            val writer = BufferedWriter(FileWriter(path.toFile(), charset))
            var isWriterEmpty = true

            // write the global properties
            ini.root.properties().forEach {
                writer.write("${it.key} = ${it.value}")
                writer.newLine()
            }

            // write the properties of each section
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

        @JvmStatic
        fun load(path: String): Ini = load(Path(path))

        @JvmStatic
        fun load(file: File): Ini = load(file.toPath())

        @JvmStatic
        fun load(path: Path): Ini = load(FileReader(path.toFile()))

        @JvmStatic
        fun load(inputStreamReader: InputStreamReader): Ini {
            val reader = BufferedReader(inputStreamReader)
            val lexer = IniScanner(reader)
            lexer.yylex()
            return lexer.ini
//            return Ini()
        }
    }

    inline fun section(name: String, block: IniSection.() -> Unit) = section(name).apply {
        block.invoke(this)
    }
}