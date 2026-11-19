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

import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.charset.CharacterCodingException
import java.nio.file.FileSystemException
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import kotlin.io.path.*
import kotlin.test.*

class AtomicStoreTest {

    @TempDir
    lateinit var dir: Path

    private val sample = ini {
        this["g"] = "1"
        section("s") { this["k"] = "v" }
    }

    /** Contains a character that US-ASCII cannot encode: writing it fails half-way. */
    private val unencodable = ini {
        section("s") { (1..1000).forEach { this["k$it"] = "value $it" } }
        section("z") { this["euro"] = "€" }
    }

    private val isPosix = "posix" in FileSystems.getDefault().supportedFileAttributeViews()

    private fun Path.entries(): List<String> = listDirectoryEntries().map { it.name }.sorted()

    @Test
    fun createsAndOverwrites() {
        val file = dir.resolve("config.ini")
        sample.store(file)
        assertEquals(sample, Ini.load(file))

        val other = ini { this["x"] = "y" }
        other.store(file)
        assertEquals("x = y\n", file.readText())
        assertEquals(listOf("config.ini"), dir.entries(), "no temporary file must be left behind")
    }

    @Test
    fun relativePathIsSupported() {
        val file = dir.resolve("rel.ini")
        val relative = Path("").toAbsolutePath().relativize(file)
        assertFalse(relative.isAbsolute)
        sample.store(relative)
        assertEquals(sample, Ini.load(file))
    }

    @Test
    fun failureLeavesOriginalUntouched() {
        val file = dir.resolve("config.ini")
        sample.store(file)
        val before = file.readBytes()

        assertFailsWith<CharacterCodingException> { unencodable.store(file, Charsets.US_ASCII) }

        assertContentEquals(before, file.readBytes())
        assertEquals(listOf("config.ini"), dir.entries(), "the temporary file must be deleted")
    }

    @Test
    fun failureDoesNotCreateMissingFile() {
        val file = dir.resolve("new.ini")
        assertFailsWith<CharacterCodingException> { unencodable.store(file, Charsets.US_ASCII) }
        assertFalse(file.exists())
        assertEquals(emptyList(), dir.entries())
    }

    @Test
    fun nonAtomicWriteCorruptsOnFailure() {
        // documents why atomic writes are the default
        val file = dir.resolve("config.ini")
        sample.store(file)
        val before = file.readBytes()

        assertFailsWith<CharacterCodingException> {
            unencodable.store(file, Charsets.US_ASCII, IniQuoteMode.WHEN_NEEDED, atomic = false)
        }
        assertFalse(before.contentEquals(file.readBytes()))
    }

    @Test
    fun nonAtomicWriteWorks() {
        val file = dir.resolve("config.ini")
        sample.store(file, atomic = false)
        assertEquals(sample, Ini.load(file))
    }

    @Test
    fun companionStoreIsAtomicToo() {
        val file = dir.resolve("config.ini")
        Ini.store(sample, file)
        val before = file.readBytes()
        assertFailsWith<CharacterCodingException> { Ini.store(unencodable, file, Charsets.US_ASCII) }
        assertContentEquals(before, file.readBytes())
    }

    @Test
    fun directoryTargetIsRejected() {
        val target = dir.resolve("sub").createDirectory()
        assertFailsWith<FileSystemException> { sample.store(target) }
        assertTrue(target.isDirectory())
    }

    @Test
    fun missingParentDirectoryFails() {
        assertFailsWith<java.nio.file.NoSuchFileException> { sample.store(dir.resolve("missing/config.ini")) }
    }

    @Test
    fun symbolicLinkIsPreserved() {
        val real = dir.resolve("real.ini")
        ini { this["old"] = "1" }.store(real)
        val link = dir.resolve("link.ini")
        try {
            Files.createSymbolicLink(link, real.fileName)
        } catch (e: Exception) {
            assumeTrue(false, "symbolic links not supported: $e")
        }

        sample.store(link)

        assertTrue(link.isSymbolicLink(), "the link must not be replaced by a regular file")
        assertEquals(sample, Ini.load(real))
        assertEquals(listOf("link.ini", "real.ini"), dir.entries())
    }

    @Test
    fun danglingSymbolicLinkCreatesTarget() {
        val link = dir.resolve("link.ini")
        try {
            Files.createSymbolicLink(link, Path("target.ini"))
        } catch (e: Exception) {
            assumeTrue(false, "symbolic links not supported: $e")
        }

        sample.store(link)

        assertTrue(link.isSymbolicLink())
        assertEquals(sample, Ini.load(dir.resolve("target.ini")))
    }

    @Test
    fun posixPermissionsArePreserved() {
        assumeTrue(isPosix)
        val file = dir.resolve("config.ini")
        sample.store(file)
        val permissions = PosixFilePermissions.fromString("rw-r-----")
        file.setPosixFilePermissions(permissions)

        ini { this["x"] = "y" }.store(file)

        assertEquals(permissions, file.getPosixFilePermissions())
    }

    @Test
    fun newFileGetsDefaultPermissions() {
        assumeTrue(isPosix)
        val reference = dir.resolve("reference").createFile()  // default permissions (umask)
        val file = dir.resolve("config.ini")
        sample.store(file)
        assertEquals(reference.getPosixFilePermissions(), file.getPosixFilePermissions())
    }
}
