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

import java.util.Collections

/**
 * A named group of key-value properties.
 *
 * Properties keep their insertion order, which is also the order used when the section is written to a file.
 * Keys must be representable in an INI file (see [set]); values can be any string.
 *
 * Instances are **not** thread-safe.
 *
 * @param sectionName The section name; it must be non-empty, without leading/trailing blanks, `]` or line breaks.
 * @throws IllegalArgumentException if [sectionName] is not a valid section name.
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
open class IniSection(val sectionName: String) : Iterable<Map.Entry<String, String>> {

    init {
        requireValidSectionName(sectionName)
    }

    protected val properties: MutableMap<String, String> = LinkedHashMap()

    /**
     * Get the number of properties in this section
     */
    val propertyCount: Int get() = properties.size

    /**
     * Returns a read-only, live [Set] of all property keys configured in this section.
     */
    val keys: Set<String> get() = Collections.unmodifiableSet(properties.keys)

    /**
     * Returns a read-only, live [Collection] of all property string values configured in this section.
     */
    val values: Collection<String> get() = Collections.unmodifiableCollection(properties.values)

    /**
     * Returns a read-only, live view of the properties of this section.
     */
    fun properties(): Map<String, String> = Collections.unmodifiableMap(properties)

    // ------------------------------------------------------------------------------------------------------------
    // Basic access
    // ------------------------------------------------------------------------------------------------------------

    /**
     * Returns the value of the property [name], or `null` if it is not defined.
     */
    operator fun get(name: String): String? = properties[name]

    /**
     * Returns the value of the property [name], or [defaultValue] if it is not defined.
     */
    fun get(name: String, defaultValue: String): String = properties[name] ?: defaultValue

    /**
     * Sets the property [name] to [value].
     *
     * @throws IllegalArgumentException if [name] cannot be written to an INI file and read back unchanged:
     * empty, with leading/trailing blanks, containing `=`, `:` or a line break, or starting with `[`, `#` or `;`.
     */
    operator fun set(name: String, value: String) {
        requireValidKey(name)
        properties[name] = value
    }

    /**
     * Sets the property [name] to the string representation of [value].
     *
     * @see set
     */
    operator fun set(name: String, value: Any) = set(name, value.toString())

    /**
     * Sets the property [name] to the string representation of [value].
     *
     * @see set
     */
    fun setProperty(name: String, value: Any) = set(name, value.toString())

    /**
     * Copies all the entries of [map] into this section, overwriting existing keys.
     */
    fun putAll(map: Map<String, Any>) = map.forEach { (k, v) -> set(k, v.toString()) }

    /**
     * Removes the property [name], returning its previous value (or `null` if it was not defined).
     */
    fun removeProperty(name: String): String? = properties.remove(name)

    /**
     * Remove all the properties in this section
     */
    fun clear() = properties.clear()

    fun isEmpty(): Boolean = properties.isEmpty()

    /**
     * Check if there's a property with the given `name`
     * @param name The property name
     */
    fun hasProperty(name: String): Boolean = properties.containsKey(name)

    /**
     * Returns the value of the property [name].
     *
     * @throws NoSuchElementException if the property is not defined.
     */
    fun getRequired(name: String): String =
        properties[name] ?: throw NoSuchElementException("Property '$name' not found in section [$sectionName]")

    // ------------------------------------------------------------------------------------------------------------
    // Typed getters
    //
    // For every type T there are three flavours:
    //   getT(name)           -> throws NoSuchElementException if missing, IllegalArgumentException if malformed
    //   getT(name, default)  -> default if missing, IllegalArgumentException if malformed
    //   getTOrNull(name)     -> null if missing or malformed
    //
    // Numeric values are trimmed before parsing.
    // ------------------------------------------------------------------------------------------------------------

    private inline fun <T : Any> parse(name: String, parser: (String) -> T): T {
        val value = getRequired(name)
        return try {
            parser(value.trim())
        } catch (e: IllegalArgumentException) {
            throw wrap(name, value, e)
        }
    }

    private inline fun <T : Any> parse(name: String, defaultValue: T, parser: (String) -> T): T =
        if (properties.containsKey(name)) parse(name, parser) else defaultValue

    private inline fun <T : Any> parseOrNull(name: String, parser: (String) -> T): T? {
        val value = properties[name] ?: return null
        return try {
            parser(value.trim())
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    private fun wrap(name: String, value: String, cause: IllegalArgumentException): IllegalArgumentException {
        val message = "Property '$name' in section [$sectionName] has an invalid value '$value'" +
                (cause.message?.let { ": $it" } ?: "")
        return if (cause is NumberFormatException) NumberFormatException(message).apply { initCause(cause) }
        else IllegalArgumentException(message, cause)
    }

    /**
     * Parses the property as a boolean. Accepted values (case-insensitive) are
     * `true`/`yes`/`on`/`1` and `false`/`no`/`off`/`0`.
     *
     * @throws NoSuchElementException if the property is not defined.
     * @throws IllegalArgumentException if the value is not a recognized boolean literal.
     */
    fun getBoolean(name: String): Boolean = parse(name, ::parseBoolean)

    fun getBoolean(name: String, defaultValue: Boolean): Boolean = parse(name, defaultValue, ::parseBoolean)

    fun getBooleanOrNull(name: String): Boolean? = parseOrNull(name, ::parseBoolean)

    fun getInt(name: String): Int = parse(name, String::toInt)

    fun getInt(name: String, defaultValue: Int): Int = parse(name, defaultValue, String::toInt)

    fun getIntOrNull(name: String): Int? = parseOrNull(name, String::toInt)

    /**
     * Parses the property as a [Long]. An optional `L`/`l` suffix is accepted (e.g. `100L`).
     */
    fun getLong(name: String): Long = parse(name, ::parseLong)

    fun getLong(name: String, defaultValue: Long): Long = parse(name, defaultValue, ::parseLong)

    fun getLongOrNull(name: String): Long? = parseOrNull(name, ::parseLong)

    fun getShort(name: String): Short = parse(name, String::toShort)

    fun getShort(name: String, defaultValue: Short): Short = parse(name, defaultValue, String::toShort)

    fun getShortOrNull(name: String): Short? = parseOrNull(name, String::toShort)

    /**
     * Parses the property as a [Double], using [java.lang.Double.parseDouble]
     * (so `3.14`, `1e3`, `2.5d`, `NaN`, `Infinity` are all accepted).
     */
    fun getDouble(name: String): Double = parse(name, java.lang.Double::parseDouble)

    fun getDouble(name: String, defaultValue: Double): Double =
        parse(name, defaultValue, java.lang.Double::parseDouble)

    fun getDoubleOrNull(name: String): Double? = parseOrNull(name, java.lang.Double::parseDouble)

    fun getFloat(name: String): Float = parse(name, java.lang.Float::parseFloat)

    fun getFloat(name: String, defaultValue: Float): Float = parse(name, defaultValue, java.lang.Float::parseFloat)

    fun getFloatOrNull(name: String): Float? = parseOrNull(name, java.lang.Float::parseFloat)

    /**
     * Parses the property as a constant of the enum [enumClass], matching the constant name case-insensitively.
     *
     * @throws NoSuchElementException if the property is not defined.
     * @throws IllegalArgumentException if no constant matches.
     */
    fun <E : Enum<E>> getEnum(name: String, enumClass: Class<E>): E = parse(name) { parseEnum(it, enumClass) }

    fun <E : Enum<E>> getEnum(name: String, enumClass: Class<E>, defaultValue: E): E =
        parse(name, defaultValue) { parseEnum(it, enumClass) }

    fun <E : Enum<E>> getEnumOrNull(name: String, enumClass: Class<E>): E? =
        parseOrNull(name) { parseEnum(it, enumClass) }

    /**
     * Kotlin-friendly variant of [getEnum]: `section.getEnum<LogLevel>("level")`.
     */
    inline fun <reified E : Enum<E>> getEnum(name: String): E = getEnum(name, E::class.java)

    inline fun <reified E : Enum<E>> getEnum(name: String, defaultValue: E): E =
        getEnum(name, E::class.java, defaultValue)

    inline fun <reified E : Enum<E>> getEnumOrNull(name: String): E? = getEnumOrNull(name, E::class.java)

    // ------------------------------------------------------------------------------------------------------------
    // Lists / arrays
    // ------------------------------------------------------------------------------------------------------------

    /**
     * Parses a list property into its elements.
     *
     * The value may be wrapped in `[...]` or `{...}`; an empty or blank list (`[]`) yields no elements.
     * Elements are trimmed; an element can be quoted (`"a, b"` or `'a, b'`) to preserve blanks or include
     * the separator, in which case the escape sequences supported in INI values are resolved.
     *
     * @throws NoSuchElementException if the property is not defined.
     * @throws IllegalArgumentException if a quoted element is malformed.
     */
    @JvmOverloads
    fun getList(name: String, separator: String = ","): List<String> {
        val value = getRequired(name)
        return try {
            splitList(value, separator)
        } catch (e: IllegalArgumentException) {
            throw wrap(name, value, e)
        }
    }

    /**
     * Same as [getList], returning an array.
     */
    @JvmOverloads
    fun getArray(name: String, separator: String = ","): Array<String> = getList(name, separator).toTypedArray()

    @JvmOverloads
    fun getIntArray(name: String, separator: String = ","): IntArray =
        convertList(name, separator, String::toInt).toIntArray()

    @JvmOverloads
    fun getLongArray(name: String, separator: String = ","): LongArray =
        convertList(name, separator, ::parseLong).toLongArray()

    @JvmOverloads
    fun getDoubleArray(name: String, separator: String = ","): DoubleArray =
        convertList(name, separator, java.lang.Double::parseDouble).toDoubleArray()

    /**
     * Parses a list property string into an array of typed [Number] elements.
     *
     * Prefer the type-safe [getIntArray], [getLongArray] and [getDoubleArray].
     *
     * @throws NoSuchElementException If the property is not defined.
     * @throws UnsupportedOperationException If the class type is unsupported.
     */
    @JvmOverloads
    fun <T : Number> getNumberArray(name: String, clazz: Class<T>, separator: String = ","): Array<out Number> {
        val converter: (String) -> Number = when (clazz) {
            Int::class.javaPrimitiveType, Int::class.javaObjectType -> String::toInt
            Float::class.javaPrimitiveType, Float::class.javaObjectType -> java.lang.Float::parseFloat
            Double::class.javaPrimitiveType, Double::class.javaObjectType -> java.lang.Double::parseDouble
            Long::class.javaPrimitiveType, Long::class.javaObjectType -> ::parseLong
            Short::class.javaPrimitiveType, Short::class.javaObjectType -> String::toShort
            Byte::class.javaPrimitiveType, Byte::class.javaObjectType -> String::toByte
            else -> throw UnsupportedOperationException("Unsupported number type: ${clazz.name}")
        }
        return convertList(name, separator, converter).toTypedArray()
    }

    private inline fun <T> convertList(name: String, separator: String, converter: (String) -> T): List<T> {
        val value = getRequired(name)
        return try {
            splitList(value, separator).map(converter)
        } catch (e: IllegalArgumentException) {
            throw wrap(name, value, e)
        }
    }

    /**
     * Stores [values] as a list property, in the form `[a, b, c]`, readable back with [getList].
     * Elements are converted with `toString()` and quoted only when needed.
     */
    fun setList(name: String, values: Iterable<Any>) {
        set(name, values.joinToString(", ", "[", "]") { formatListElement(it.toString()) })
    }

    /**
     * Vararg variant of [setList].
     */
    fun setList(name: String, vararg values: Any) = setList(name, values.asList())

    // ------------------------------------------------------------------------------------------------------------
    // Hierarchical navigation
    // ------------------------------------------------------------------------------------------------------------

    /**
     * Extracts the distinct next-level tokens (nodes) that follow a given [prefix].
     * If [prefix] is empty, it returns the very first token of all keys in this section.
     *
     * Example: if keys are `db.mysql.host` and `db.postgres.host`
     * - `getNodes("db")` returns `["mysql", "postgres"]`
     * - `getNodes()` returns `["db"]`
     */
    @JvmOverloads
    fun getNodes(prefix: String = ""): Collection<String> = nextLevelTokens(properties.keys, prefix)

    /**
     * Groups all properties in this section by their root token (the part before the first dot).
     * Properties without a dot in their key will be grouped under an empty string key `""`.
     *
     * Example:
     * `db.host = localhost` and `server.port = 8080`
     * Returns: `{"db" -> {"host" -> "localhost"}, "server" -> {"port" -> "8080"}}`
     */
    fun groupByRoot(): Map<String, Map<String, String>> = groupByFirstToken(properties)

    /**
     * Returns a filtered map containing all properties that start with the specified [prefix].
     *
     * @param prefix The dot-separated prefix to filter by (e.g., "db.mysql").
     * @param stripPrefix If `true`, the prefix and its trailing dot are removed from the resulting map keys.
     *
     * Example with `stripPrefix = true`:
     * `db.mysql.host = localhost` becomes `host = localhost` in the returned map.
     */
    @JvmOverloads
    fun getGroup(prefix: String, stripPrefix: Boolean = true): Map<String, String> {
        val fullPrefix = "$prefix."
        return properties.filter { it.key.startsWith(fullPrefix) }
            .mapKeys { if (stripPrefix) it.key.substring(fullPrefix.length) else it.key }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Object contract
    // ------------------------------------------------------------------------------------------------------------

    /**
     * Returns an independent copy of this section.
     */
    open fun copy(): IniSection = IniSection(sectionName).also { it.properties.putAll(properties) }

    override fun toString(): String {
        val builder = StringBuilder()
        builder.append("[").append(sectionName).append("]")
        properties.forEach { (name, value) ->
            builder.append("\n\t").append(name).append("=").append(value)
        }
        return builder.toString().trim()
    }

    /**
     * Two sections are equal when they have the same class, the same name and the same properties
     * (property order is not significant).
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        other as IniSection
        return sectionName == other.sectionName && properties == other.properties
    }

    override fun hashCode(): Int = 31 * sectionName.hashCode() + properties.hashCode()

    /**
     * Iterates over the properties, in insertion order. The iterator does not support removal.
     */
    override fun iterator(): Iterator<Map.Entry<String, String>> = properties().entries.iterator()

    // ------------------------------------------------------------------------------------------------------------
    // DSL
    // ------------------------------------------------------------------------------------------------------------

    /**
     * Infix operator to set a property value using a syntax similar to Kotlin Maps.
     *
     * Example: `"port" to 8080`
     *
     * Note: inside a section block this shadows [kotlin.to]; use [Pair] explicitly to build pairs there.
     */
    infix fun String.to(value: Any) {
        setProperty(this, value)
    }

    /**
     * Operator overload to configure a subsection or set properties inside a section block.
     * Combined with the [to] infix operator, it allows a highly declarative DSL.
     *
     * Inside an [Ini] block it configures the section with that name; inside a section block it
     * sets dot-prefixed properties (`"pool" { "size" to 10 }` sets `pool.size`).
     *
     * Example:
     * ```
     * "database" {
     *    "host" to "localhost"
     *    "port" to 5432
     * }
     * ```
     */
    operator fun String.invoke(block: IniSection.() -> Unit) {
        if (this@IniSection is Ini) {
            this@IniSection.section(this, block)
        } else {
            val subSection = IniSection(this)
            subSection.block()
            subSection.forEach { (k, v) -> this@IniSection["$this.$k"] = v }
        }
    }
}

// ----------------------------------------------------------------------------------------------------------------
// Helpers
// ----------------------------------------------------------------------------------------------------------------

private val TRUE_LITERALS = setOf("true", "yes", "on", "1")
private val FALSE_LITERALS = setOf("false", "no", "off", "0")

internal fun parseBoolean(value: String): Boolean {
    val v = value.lowercase()
    return when (v) {
        in TRUE_LITERALS -> true
        in FALSE_LITERALS -> false
        else -> throw IllegalArgumentException("not a boolean (expected true/false, yes/no, on/off, 1/0)")
    }
}

internal fun parseLong(value: String): Long =
    if (value.length > 1 && (value.last() == 'L' || value.last() == 'l')) value.dropLast(1).toLong()
    else value.toLong()

internal fun <E : Enum<E>> parseEnum(value: String, enumClass: Class<E>): E =
    enumClass.enumConstants.firstOrNull { it.name.equals(value, ignoreCase = true) }
        ?: throw IllegalArgumentException(
            "expected one of ${enumClass.enumConstants.joinToString { it.name }}"
        )

internal fun nextLevelTokens(names: Collection<String>, prefix: String): List<String> {
    val fullPrefix = if (prefix.isEmpty()) "" else "$prefix."
    return names.asSequence()
        .filter { it.startsWith(fullPrefix) && it.length > fullPrefix.length }
        .map { name ->
            val remaining = name.substring(fullPrefix.length)
            val dotIndex = remaining.indexOf('.')
            if (dotIndex < 0) remaining else remaining.substring(0, dotIndex)
        }
        .distinct()
        .toList()
}

internal fun <V> groupByFirstToken(map: Map<String, V>): Map<String, Map<String, V>> {
    val result = LinkedHashMap<String, MutableMap<String, V>>()
    map.forEach { (key, value) ->
        val dotIndex = key.indexOf('.')
        val root = if (dotIndex < 0) "" else key.substring(0, dotIndex)
        val rest = if (dotIndex < 0) key else key.substring(dotIndex + 1)
        result.getOrPut(root) { LinkedHashMap() }[rest] = value
    }
    return result
}

/**
 * Splits a list value such as `[a, "b, c", d]` into its elements.
 */
internal fun splitList(value: String, separator: String): List<String> {
    require(separator.isNotEmpty()) { "separator must not be empty" }
    var text = value.trim()
    if (text.length >= 2 &&
        ((text.first() == '[' && text.last() == ']') || (text.first() == '{' && text.last() == '}'))
    ) {
        text = text.substring(1, text.length - 1)
    }
    if (text.isBlank()) return emptyList()

    val result = ArrayList<String>()
    var i = 0
    while (true) {
        while (i < text.length && text[i].isWhitespace()) i++
        if (i < text.length && (text[i] == '"' || text[i] == '\'')) {
            val quote = text[i]
            var j = i + 1
            while (j < text.length && text[j] != quote) j += if (text[j] == '\\') 2 else 1
            require(j < text.length) { "unterminated quoted element at position $i" }
            result += IniParser.unescape(text, i + 1, j)
            i = j + 1
            while (i < text.length && text[i].isWhitespace()) i++
            if (i >= text.length) return result
            require(text.startsWith(separator, i)) { "unexpected text after quoted element at position $i" }
            i += separator.length
        } else {
            val next = text.indexOf(separator, i)
            if (next < 0) {
                result += text.substring(i).trim()
                return result
            }
            result += text.substring(i, next).trim()
            i = next + separator.length
        }
    }
}

private fun formatListElement(element: String): String {
    val needsQuotes = element.isEmpty() ||
            element.first().isWhitespace() || element.last().isWhitespace() ||
            element.first() == '"' || element.first() == '\'' ||
            ',' in element || element.any { it.isISOControl() }
    return if (needsQuotes) quoteValue(element, IniQuoteMode.ALWAYS) else element
}
