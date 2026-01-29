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

package org.mth.kini.model

import org.mth.kini.core.IniScanner
import java.io.*
import java.nio.charset.Charset
import java.nio.file.Path
import java.util.*
import kotlin.io.path.Path

class Ini : IniSection(ROOT) {

    val sections: MutableSet<IniSection> = mutableSetOf()
    val globalPropertyCount by this::propertyCount

    /**
     * Get the number of the sections contained in the INI. The number does not include the global section, so
     * the number counts exactly the explicitly declared sections.
     */
    fun sectionCount() = sections.size

    fun section(name: String): IniSection {
        val iterator = sections.iterator()

        while (iterator.hasNext()) {
            val section = iterator.next()

            if (name == section.sectionName)
                return section
        }

        val newSection = IniSection(name)
        sections.add(newSection)

        return newSection
    }


    fun globalProperties(): Map<String, String> = properties()

    fun hasSection(name: String): Boolean = sections.count { it.sectionName == name } != 0

    fun removeAllSections() = sections.clear()

    fun removeSection(name: String) = sections.removeIf { it.sectionName == name }

    fun getSub(sectionName: String, prefix: String): Collection<String> =
        section(sectionName).getSub(prefix)

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

    fun toProperties(sectionResolver: (IniSection, String) -> String): Properties = Properties().apply {
        this.putAll(globalProperties())

        sections.forEach { section ->
            section.properties().forEach { this.setProperty(sectionResolver.invoke(section, it.key), it.value) }
        }
    }

    /**
     * Convert the INI into a [Properties] object.
     * Properties in sections will be transformed in **sectionName.propertyName**
     */
    fun toProperties(): Properties = toProperties { section, propertyName -> "${section.sectionName}.$propertyName" }

    @JvmOverloads
    fun store(path: Path, charset: Charset = Charsets.UTF_8) {
        store(this, path, charset)
    }

    override fun toString(): String {
        val b = StringBuilder()

        properties.forEach {
            b.append("${it.key} = ${it.value}")
            b.append('\n')
        }

        // add one empty line between global properties and the sections
        if (b.isNotEmpty()) {
            b.append('\n')
        }

        sections.filter { !it.isEmpty() }
            .forEach { section ->
                if (b.isNotEmpty()) {
                    b.append('\n')
                }

                // section declaration
                b.append("[%s]\n".format(section.sectionName))

                section.properties().forEach {
                    b.append("${it.key} = ${it.value}")
                    b.append('\n')
                }
            }

        return b.toString().trim()
    }

    companion object {
        const val ROOT = "###root###"

        @JvmStatic
        inline fun newIni(block: Ini.() -> Unit) = Ini().apply {
            block.invoke(this)
        }

        @JvmStatic
        fun store(ini: Ini, path: Path, charset: Charset = Charsets.UTF_8) {
            BufferedWriter(FileWriter(path.toFile(), charset)).run {
                write(ini.toString())
                flush()
                close()
            }
        }

        @JvmStatic
        fun load(path: String): Ini = load(Path(path))

        @JvmStatic
        fun load(file: File): Ini = load(file.toPath())

        @JvmStatic
        fun load(path: Path): Ini = load(FileReader(path.toFile()))

        @JvmStatic
        fun load(inputStreamReader: InputStreamReader): Ini {
            val ini = Ini()
            val reader = BufferedReader(inputStreamReader)
            val lexer = IniScanner(reader)
            lexer.yylex()
            lexer.ini.forEach { (section, properties) ->
                if (section == IniScanner.DEFAULT_SECTION)
                    properties.forEach { ini[it[0]] = it[1] }
                else {
                    val sec = ini.section(section)
                    properties.forEach { sec[it[0]] = it[1] }
                }
            }
            return ini
        }
    }

    inline fun section(name: String, block: IniSection.() -> Unit) = section(name).apply {
        block.invoke(this)
    }
}