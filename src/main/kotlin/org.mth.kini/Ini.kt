package org.mth.kini

import java.io.BufferedReader
import java.io.FileReader
import java.io.InputStreamReader
import java.nio.file.Path

class Ini {
    var sectionName: String = "****"
    val properties: MutableMap<String, String> = mutableMapOf()
    val sections: MutableCollection<Ini> = mutableListOf()

    fun setProperty(name: String, value: Any) {
        properties[name] = value.toString()
    }

    operator fun get(name: String): String? = properties[name]

    operator fun set(name: String, value: String) {
        properties[name] = value
    }

    fun hasProperty(name: String) = properties.containsKey(name)

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


    fun section(name: String): Ini {
        val iterator = sections.iterator()

        while (iterator.hasNext()) {
            val section = iterator.next()

            if (name == section.sectionName)
                return section
        }

        val newSection = Ini().apply { sectionName = name }
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

    companion object {
        fun load(path: Path): Ini = load(FileReader(path.toFile()))

        fun load(inputStreamReader: InputStreamReader): Ini {
            val reader = BufferedReader(inputStreamReader)
            val fileContent = reader.readText()
            reader.close()

            return IniParser.parse(fileContent)
        }
    }
}