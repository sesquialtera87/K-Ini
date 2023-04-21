package org.mth.kini

import java.io.*
import java.nio.charset.Charset
import java.nio.file.Path

class Ini : IniSection("##root##") {

    fun store(path: Path, charset: Charset=Charsets.UTF_8) {
        store(this, path, charset)
    }

    companion object {
        inline fun newIni(block: Ini.() -> Unit) = Ini().apply {
            block.invoke(this)
        }

        fun store(ini: Ini, path: Path, charset: Charset = Charsets.UTF_8) {
            val writer = BufferedWriter(FileWriter(path.toFile(), charset))
            var isWriterEmpty = true

            ini.sections
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

        fun load(path: Path): IniSection = load(FileReader(path.toFile()))

        fun load(inputStreamReader: InputStreamReader): Ini {
            val reader = BufferedReader(inputStreamReader)
            val fileContent = reader.readText()
            reader.close()
            return IniParser.parse(fileContent)
        }
    }

    inline fun section(name: String, block: IniSection.() -> Unit) = section(name).apply {
        block.invoke(this)
    }
}