package org.mth.kini

import java.io.*
import java.nio.charset.Charset
import java.nio.file.Path
import java.util.*
import kotlin.io.path.Path

/**
 * Represents an INI configuration file, managing global properties and grouped sections.
 *
 * This class extends [IniSection] to inherit management for root/global properties
 * (properties declared outside or before any explicit section).
 *
 * @author Mattia Marelli
 * @since 2026
 */
class Ini : IniSection(ROOT) {

    /**
     * Internal backing map that caches sections by their unique name for O(1) lookups.
     */
    private val sectionsMap: MutableMap<String, IniSection> = mutableMapOf()

    /**
     * A read-only collection of all the explicit sections currently contained in this INI object.
     */
    val sections: Collection<IniSection> get() = sectionsMap.values

    /**
     * The total number of global (root) properties.
     */
    val globalPropertyCount by this::propertyCount

    /**
     * Returns the total number of explicit sections contained in this INI.
     * This count does not include the global/root section.
     *
     * @return The number of explicitly declared sections.
     */
    fun sectionCount() = sectionsMap.size

    /**
     * Retrieves an existing [IniSection] by its name, or creates a new one if it does not exist.
     *
     * @param name The name of the section to retrieve or create.
     * @return The [IniSection] matching the provided name.
     */
    fun section(name: String): IniSection {
        return sectionsMap.getOrPut(name) { IniSection(name) }
    }

    /**
     * Returns a map containing all the global (root-level) properties.
     *
     * @return A map of global key-value property pairs.
     */
    fun globalProperties(): Map<String, String> = properties()

    /**
     * Checks whether an explicit section with the given name exists.
     *
     * @param name The name of the section to look for.
     * @return `true` if the section exists, `false` otherwise.
     */
    fun hasSection(name: String): Boolean = sectionsMap.containsKey(name)

    /**
     * Clears and removes all explicit sections from this INI object.
     */
    fun removeAllSections() = sectionsMap.clear()

    /**
     * Removes an explicit section by its name.
     *
     * @param name The name of the section to remove.
     * @return `true` if the section was found and successfully removed, `false` otherwise.
     */
    fun removeSection(name: String) = sectionsMap.remove(name) != null

    /**
     * Extracts a sub-collection of nested property keys from a specific section sharing a common prefix.
     *
     * @param sectionName The name of the target section.
     * @param prefix The dot-separated hierarchy prefix to look for.
     * @return A collection of sub-keys matching the prefix hierarchy.
     */
    fun getSub(sectionName: String, prefix: String): Collection<String> =
        section(sectionName).getSub(prefix)

    /**
     * Merges all global properties and explicit sections from the given [ini] object into this one.
     *
     * **WARNING:** Existing properties with the same keys or sections with the same names
     * will be overwritten by the values from the source [ini] object.
     *
     * @param ini The source [Ini] object to merge into this instance.
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

    /**
     * Converts this INI instance into a standard [Properties] object, using a custom lambda
     * resolver to format property keys originating from explicit sections.
     *
     * @param sectionResolver A functional block defining how to map an [IniSection] and its property name into a flat string key.
     * @return A [Properties] instance containing all mapped global and sectioned properties.
     */
    fun toProperties(sectionResolver: (IniSection, String) -> String): Properties = Properties().apply {
        putAll(globalProperties())
        sectionsMap.values.forEach { section ->
            section.forEach { (propName, value) ->
                setProperty(sectionResolver.invoke(section, propName), value)
            }
        }
    }

    /**
     * Converts this INI instance into a standard [Properties] object.
     * Properties belonging to an explicit section will be flattened using the default format:
     * `sectionName.propertyName`.
     *
     * @return A flat [Properties] instance representing this INI object.
     */
    fun toProperties(): Properties = toProperties { section, propertyName -> "${section.sectionName}.$propertyName" }

    /**
     * Persists the current INI structure to a file at the specified path.
     *
     * @param path The filesystem [Path] where the file will be saved.
     * @param charset The character encoding to use when writing the file. Defaults to UTF-8.
     * @throws IOException If an I/O error occurs while opening or writing to the file.
     */
    @JvmOverloads
    fun store(path: Path, charset: Charset = Charsets.UTF_8) {
        store(this, path, charset)
    }

    /**
     * Serializes this INI instance into its standard textual file format representation.
     *
     * @return A formatted string representation of the entire INI document.
     */
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
        /**
         * Internal fallback identifier for the global/root section.
         */
        const val ROOT = "###root###"

        /**
         * Writes the text-serialized content of an [Ini] instance to a file.
         *
         * @param ini The [Ini] instance to persist.
         * @param path The target [Path] of the output file.
         * @param charset The character encoding to use. Defaults to UTF-8.
         * @throws IOException If the file cannot be created, opened, or written to.
         */
        @JvmStatic
        fun store(ini: Ini, path: Path, charset: Charset = Charsets.UTF_8) {
            BufferedWriter(FileWriter(path.toFile(), charset)).use { writer ->
                writer.write(ini.toString())
                writer.flush()
            }
        }

        /**
         * Parses an INI configuration from a file path string.
         *
         * @param path The string path leading to the target INI file.
         * @return An [Ini] instance populated with the parsed configuration.
         * @throws IOException If the file does not exist or cannot be read.
         */
        @JvmStatic
        fun load(path: String): Ini = load(Path(path))

        /**
         * Parses an INI configuration from a [File] handle.
         *
         * @param file The [File] pointing to the INI data source.
         * @return An [Ini] instance populated with the parsed configuration.
         * @throws IOException If the file is invalid or unreadable.
         */
        @JvmStatic
        fun load(file: File): Ini = load(file.toPath())

        /**
         * Parses an INI configuration from a filesystem [Path].
         *
         * @param path The [Path] leading to the target INI file.
         * @return An [Ini] instance populated with the parsed configuration.
         * @throws IOException If an I/O error occurs during reading.
         */
        @JvmStatic
        fun load(path: Path): Ini = FileReader(path.toFile()).use { load(it) }

        /**
         * Low-level parsing node that processes an input character stream using an automated JFlex lexer.
         *
         * @param inputStreamReader The raw [InputStreamReader] stream containing the INI text content.
         * @return A fully populated [Ini] instance.
         */
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

        /**
         * Attempts to safely load an INI configuration from a [Path].
         * Catches any internal parser or stream errors, returning `null` instead of throwing exceptions.
         *
         * @param path The [Path] leading to the target INI file.
         * @return A populated [Ini] instance, or `null` if any exception occurred.
         */
        @JvmStatic
        fun loadOrNull(path: Path): Ini? = try {
            load(path)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Scope-oriented DSL extension enabling fluent configuration of an explicit section via a lambda block.
     *
     * Example:
     * ```
     * ini.section("database") {
     * setProperty("host", "localhost")
     * setProperty("port", "5432")
     * }
     * ```
     *
     * @param name The name of the section to configure.
     * @param block The configuration lambda executed within the context of the target [IniSection].
     * @return The configured [IniSection] instance.
     */
    inline fun section(name: String, block: IniSection.() -> Unit) = section(name).apply {
        block.invoke(this)
    }

    /**
     * Operator overload mapping the `in` keyword to check for section existence.
     *
     * Example: `if ("network" in ini) { ... }`
     *
     * @param sectionName The name of the section to verify.
     * @return `true` if the section is present, `false` otherwise.
     */
    operator fun contains(sectionName: String): Boolean = hasSection(sectionName)
}