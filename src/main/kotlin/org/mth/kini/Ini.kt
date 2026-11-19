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

import java.io.*
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.io.path.Path

/**
 * Represents an INI configuration file, managing global properties and grouped sections.
 *
 * This class extends [IniSection] to inherit management for root/global properties
 * (properties declared outside or before any explicit section).
 *
 * Sections and properties keep their insertion order. Instances are **not** thread-safe.
 *
 * @author Mattia Marelli
 * @since 2026
 */
class Ini : IniSection(ROOT) {

    /**
     * Internal backing map that caches sections by their unique name for O(1) lookups.
     */
    private val sectionsMap: MutableMap<String, IniSection> = LinkedHashMap()

    /**
     * A read-only, live collection of all the explicit sections currently contained in this INI object.
     */
    val sections: Collection<IniSection> get() = Collections.unmodifiableCollection(sectionsMap.values)

    /**
     * The total number of global (root) properties.
     */
    val globalPropertyCount: Int get() = propertyCount

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
     * @throws IllegalArgumentException if [name] is not a valid section name.
     * @see sectionOrNull
     */
    fun section(name: String): IniSection {
        return sectionsMap.getOrPut(name) { IniSection(name) }
    }

    /**
     * Retrieves an existing [IniSection] by its name, without creating it.
     *
     * @return The section, or `null` if there is no section with that name.
     */
    fun sectionOrNull(name: String): IniSection? = sectionsMap[name]

    /**
     * Returns a read-only view of all the global (root-level) properties.
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
     * Extracts the distinct next-level tokens (nodes) that follow a given section [prefix].
     * If [prefix] is empty, it returns the very first token of all section names.
     *
     * Example: if sections are `[db.mysql]`, `[db.postgres]` and `[server]`
     * - `getSectionNodes("db")` returns `["mysql", "postgres"]`
     * - `getSectionNodes()` returns `["db", "server"]`
     */
    @JvmOverloads
    fun getSectionNodes(prefix: String = ""): List<String> = nextLevelTokens(sectionsMap.keys, prefix)

    /**
     * Returns a filtered map of [IniSection] instances whose names start with the specified [prefix].
     *
     * @param prefix The prefix namespace to filter sections by.
     * @param delimiter The boundary string separating hierarchy levels. Defaults to `"."`.
     * @param stripPrefix If `true`, the prefix and trailing delimiter are removed from the keys in the returned map.
     *
     * Example:
     * - `getSectionGroup("modules", delimiter = ".")` -> `{"auth" -> IniSection, "payment" -> IniSection}`
     * - `getSectionGroup("modules", delimiter = ":")` -> matches `[modules:auth]`
     */
    @JvmOverloads
    fun getSectionGroup(
        prefix: String,
        delimiter: String = ".",
        stripPrefix: Boolean = true
    ): Map<String, IniSection> {
        if (prefix.isEmpty()) {
            return sectionsMap.toMap()
        }

        val fullPrefix = if (prefix.endsWith(delimiter)) prefix else "$prefix$delimiter"

        return sectionsMap.asSequence()
            .filter { it.key.startsWith(fullPrefix) }
            .associate { (key, section) ->
                val newKey = if (stripPrefix) key.substring(fullPrefix.length) else key
                Pair(newKey, section)
            }
    }

    /**
     * Groups all explicit sections by their root token (the part before the first dot in the section name).
     * Sections without a dot in their name will be grouped under the root key `""`.
     *
     * Example: `[db.mysql]` and `[server.api]`
     * Returns: `{"db" -> {"mysql" -> IniSection}, "server" -> {"api" -> IniSection}}`
     */
    fun groupBySectionRoot(): Map<String, Map<String, IniSection>> = groupByFirstToken(sectionsMap)

    /**
     * Merges all global properties and explicit sections from the given [ini] object into this one.
     *
     * **WARNING:** Existing properties with the same keys will be overwritten by the values from the
     * source [ini] object. Empty source sections are ignored.
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
     * Returns a deep copy of this INI: modifying the copy (or its sections) does not affect this instance.
     */
    override fun copy(): Ini {
        val copy = Ini()
        copy.properties.putAll(properties)
        sectionsMap.forEach { (name, section) -> copy.sectionsMap[name] = section.copy() }
        return copy
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
     * @param quoteMode The strategy determining when values are quoted. Defaults to [IniQuoteMode.WHEN_NEEDED].
     * @throws IOException If an I/O error occurs while opening or writing to the file.
     */
    @JvmOverloads
    fun store(path: Path, charset: Charset = Charsets.UTF_8, quoteMode: IniQuoteMode = IniQuoteMode.WHEN_NEEDED) {
        store(this, path, charset, quoteMode)
    }

    /**
     * Writes this INI to [writer] (the writer is flushed, not closed).
     *
     * @throws IOException If an I/O error occurs.
     */
    @JvmOverloads
    fun store(writer: Writer, quoteMode: IniQuoteMode = IniQuoteMode.WHEN_NEEDED) {
        writer.write(serialize(quoteMode))
        writer.write("\n")
        writer.flush()
    }

    /**
     * Writes this INI to [outputStream] using [charset] (the stream is flushed, not closed).
     *
     * @throws IOException If an I/O error occurs.
     */
    @JvmOverloads
    fun store(
        outputStream: OutputStream,
        charset: Charset = Charsets.UTF_8,
        quoteMode: IniQuoteMode = IniQuoteMode.WHEN_NEEDED
    ) {
        store(OutputStreamWriter(outputStream, charset), quoteMode)
    }

    /**
     * Serializes this INI instance into its standard textual file format representation.
     *
     * @return A formatted string representation of the entire INI document.
     */
    override fun toString(): String = serialize()

    /**
     * Serializes this INI instance into its textual representation, without a trailing line break.
     *
     * Global properties come first, then each non-empty section, in insertion order. Values are quoted
     * and escaped according to [quoteMode], so that [parse] of the result yields an equal [Ini]
     * (except for empty sections, which are omitted).
     */
    @JvmOverloads
    fun serialize(quoteMode: IniQuoteMode = IniQuoteMode.WHEN_NEEDED): String {
        val b = StringBuilder()

        properties.forEach { (k, v) ->
            b.append(k).append(" = ").append(quoteValue(v, quoteMode)).append('\n')
        }

        sectionsMap.values.filter { !it.isEmpty() }
            .forEach { section ->
                if (b.isNotEmpty()) b.append('\n')
                b.append('[').append(section.sectionName).append("]\n")
                section.forEach { (k, v) ->
                    b.append(k).append(" = ").append(quoteValue(v, quoteMode)).append('\n')
                }
            }

        if (b.isNotEmpty()) b.setLength(b.length - 1)
        return b.toString()
    }

    /**
     * Two [Ini] objects are equal when they have the same global properties and the same sections
     * (order is not significant).
     */
    override fun equals(other: Any?): Boolean {
        if (!super.equals(other)) return false
        return sectionsMap == (other as Ini).sectionsMap
    }

    override fun hashCode(): Int = 31 * super.hashCode() + sectionsMap.hashCode()

    companion object {
        /**
         * Internal identifier of the global/root section.
         */
        const val ROOT = "###root###"

        /**
         * Serializes and writes an [Ini] structure to a file at the specified [path].
         *
         * @param ini The [Ini] instance containing the sections and key-value pairs to store.
         * @param path The target file [Path] where the INI configuration will be written.
         * @param charset The character set encoding to use when writing to the file.
         * Defaults to [Charsets.UTF_8].
         * @param quoteMode The strategy determining when string values should be enclosed in quotes.
         * Defaults to [IniQuoteMode.WHEN_NEEDED].
         *
         * @throws java.io.IOException If an I/O error occurs while opening or writing to the file.
         *
         * @see Ini.serialize
         * @see IniQuoteMode
         */
        @JvmStatic
        @JvmOverloads
        fun store(
            ini: Ini,
            path: Path,
            charset: Charset = Charsets.UTF_8,
            quoteMode: IniQuoteMode = IniQuoteMode.WHEN_NEEDED
        ) {
            Files.newBufferedWriter(path, charset).use { writer -> ini.store(writer, quoteMode) }
        }

        /**
         * Parses INI text.
         *
         * @throws IniParseException If the text is malformed.
         */
        @JvmStatic
        fun parse(text: CharSequence): Ini = Ini().also { IniParser.parse(text, it) }

        /**
         * Parses an INI configuration from a file path string.
         *
         * @param path The string path leading to the target INI file.
         * @param charset The file encoding. Defaults to UTF-8.
         * @return An [Ini] instance populated with the parsed configuration.
         * @throws IOException If the file does not exist or cannot be read (or is not valid in [charset]).
         * @throws IniParseException If the file is malformed.
         */
        @JvmStatic
        @JvmOverloads
        fun load(path: String, charset: Charset = Charsets.UTF_8): Ini = load(Path(path), charset)

        /**
         * Parses an INI configuration from a [File] handle.
         *
         * @see load
         */
        @JvmStatic
        @JvmOverloads
        fun load(file: File, charset: Charset = Charsets.UTF_8): Ini = load(file.toPath(), charset)

        /**
         * Parses an INI configuration from a filesystem [Path].
         *
         * @param path The [Path] leading to the target INI file.
         * @param charset The file encoding. Defaults to UTF-8.
         * @return An [Ini] instance populated with the parsed configuration.
         * @throws IOException If an I/O error occurs during reading (or the file is not valid in [charset]).
         * @throws IniParseException If the file is malformed.
         */
        @JvmStatic
        @JvmOverloads
        fun load(path: Path, charset: Charset = Charsets.UTF_8): Ini =
            Files.newBufferedReader(path, charset).use { load(it) }

        /**
         * Parses an INI configuration from an [InputStream]. The stream is not closed.
         *
         * @throws IOException If an I/O error occurs during reading.
         * @throws IniParseException If the content is malformed.
         */
        @JvmStatic
        @JvmOverloads
        fun load(inputStream: InputStream, charset: Charset = Charsets.UTF_8): Ini =
            load(InputStreamReader(inputStream, charset))

        /**
         * Parses an INI configuration from a [Reader]. The reader is read to the end but not closed.
         *
         * @throws IOException If an I/O error occurs during reading.
         * @throws IniParseException If the content is malformed.
         */
        @JvmStatic
        fun load(reader: Reader): Ini = parse(reader.readText())

        /**
         * Attempts to safely load an INI configuration from a [Path].
         * Catches any I/O or parsing error, returning `null` instead of throwing exceptions.
         *
         * @param path The [Path] leading to the target INI file.
         * @return A populated [Ini] instance, or `null` if any exception occurred.
         */
        @JvmStatic
        @JvmOverloads
        fun loadOrNull(path: Path, charset: Charset = Charsets.UTF_8): Ini? = try {
            load(path, charset)
        } catch (e: IOException) {
            null
        } catch (e: IniParseException) {
            null
        }
    }

    /**
     * Scope-oriented DSL extension enabling fluent configuration of an explicit section via a lambda block.
     *
     * Example:
     * ```
     * ini.section("database") {
     *     setProperty("host", "localhost")
     *     setProperty("port", "5432")
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
