package org.mth.kini

class Ini {
    var sectionName: String = "****"
    val properties: MutableMap<String, String> = mutableMapOf()
    val sections: MutableCollection<Ini> = mutableListOf()

    fun addProperty(name: String, value: String) {
        properties[name] = value
    }

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
}