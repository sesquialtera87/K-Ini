/**
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

package org.mth.kini

open class IniSection(var sectionName: String) {

    private val properties: MutableMap<String, String> = mutableMapOf()
    val sections: MutableCollection<IniSection> = mutableListOf()
    val propertyCount by properties::size

    @Suppress("unused")
    fun setProperty(name: String, value: Any) {
        properties[name] = value.toString()
    }

    fun properties() = properties.asIterable()

    operator fun get(name: String): String? = properties[name]

    operator fun set(name: String, value: String) {
        properties[name] = value
    }

    fun isEmpty() = properties.isEmpty()

    @Suppress("unused")
    fun hasProperty(name: String) = properties.containsKey(name)

    fun hasSection(name: String) = sections.any { it.sectionName == name }

    fun getBoolean(name: String) = properties[name].toBoolean()

    fun getInt(name: String): Int = Integer.valueOf(properties[name])

    fun getLong(name: String): Long {
        val value = properties[name] ?: throw NumberFormatException("Cannot parse null string")

        return if (value.last().lowercase() == "l") {
            java.lang.Long.valueOf(value.substring(0, value.length - 1))
        } else
            java.lang.Long.valueOf(value)
    }

    fun getShort(name: String): Short = java.lang.Short.valueOf(properties[name])

    fun getDouble(name: String): Double = java.lang.Double.valueOf(properties[name])

    fun getFloat(name: String): Float = java.lang.Float.valueOf(properties[name])

    @Suppress("unused")
    fun removeSection(name: String) = sections.removeIf { it.sectionName == name }

    @Suppress("unused")
    fun removeProperty(name: String) = properties.remove(name)

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

    override fun toString(): String {
        val builder = StringBuilder()

        for (section in sections) {
            if (builder.isNotEmpty())
                builder.append("\n\n")

            builder.append("[").append(section.sectionName).append("]")

            section.properties.forEach { (name, value) ->
                builder.append("\n\t").append(name).append("=").append(value)
            }
        }

        return builder.toString()
    }
}