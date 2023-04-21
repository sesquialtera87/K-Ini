![GitHub](https://img.shields.io/github/license/sesquialtera87/k-ini)
x
# K-INI

Simple Kotlin/Java library to read and write `.ini` files (requires Java 11 or above)

## Usage

---

#### Working with `.ini` files

An `.ini` file is represented by an instance of the class `org.mth.kini.Ini`, describing
sections and properties contained in the `.ini` file.

Instances of the `org.mth.kini.Ini` class can be obtained in two ways:

- instantiate an object through the default constructor `val ini = Ini()`

- using `Ini.newIni` function to build a `Ini` object in the style of Kotlin scope functions

```kotlin
val ini = Ini.newIni {
    section("Section 1") {
        set("property 1", "value 1")
        set("property 2", "value 2")
    }
    section("Section 2") {
        set("property 1", "value 1")
        set("property 2", "value 2")
    }
}

```

Existing `.ini` files can be loaded into a `Ini` object through one of the `load` functions
of the `org.mth.kini.Ini` class.

```kotlin
val ini = Ini.load(Path.of("test\\sample.ini"))
```

The content of an `Ini` object can be store to the local filesystem with a call to the
`store(ini: Ini, path: Path, charset: Charset)` function, or by a direct call of the
`store` function of the `Ini` object.

```kotlin
val ini = Ini()
ini.store(Path.of("test\\sample.ini"))
```

#### Sections

Use the `hasSection(name: String)` to check that a section is present in the `Ini`.
The function `section(name: String)` retrieves a named section for property retrieval.

```kotlin
val section: IniSection = ini.section("my-section")
```

When the `Ini` object doesn't contain a section with the given name, the function
still returns a new `IniSection` object, so that the same function serves
for both creation ad retrieval purposes.

A section (with all its properties) can be removed from the `Ini` object by calling the
`removeSection(name: String)` function.

#### Properties

To check for a property use the `hasProperty(name: String)` function of the `org.mth.kini.IniSection` class.

`IniSection` behaves like a map, so properties are stored as key-value pairs, where
both the key and value are `String` objects.
To insert a new property or to modify an existing value the `IniSection` class provides the
`set(name: String, value: String)` function.
To access a property value the class provides the function `get(name: String)` to return the related string
value.

The `set` and `get` functions are also implemented as Kotlin operator functions, so properties
can be referred by index access:

```kotlin
val value = section["my-property"]      // get a property value
section["my-property"] = "new value"    // set a property value
```

**NOTE:** K-INI supports default properties, viz properties without a parent section,
belonging to the top-level context of the `.ini` file. To access them use the `set` and `get`
functions or the index access syntax as for the `IniSection`.

```kotlin
val ini = Ini()
// some initialization code ...
val value = ini["my-property"]  // obtain a top-level property value
```

To avoid casts to other types, `IniSection` offers functions to get property values
in the most common types (boolean and numerics):

- `getBoolean(name: String)`
- `getShort(name: String)`
- `getInt(name: String)`
- `getLong(name: String)`
- `getDouble(name: String)`
- `getFloat(name: String)`

**NOTE:** if the value cannot be parsed into the requested datatype an exception is thrown.

## License

---

Distributed under the MIT License.