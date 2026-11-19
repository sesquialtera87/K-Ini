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

import java.io.IOException
import java.io.Writer
import java.nio.channels.Channels
import java.nio.channels.FileChannel
import java.nio.charset.Charset
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.FileAlreadyExistsException
import java.nio.file.FileSystemException
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.util.concurrent.ThreadLocalRandom

/**
 * Writes a file so that readers (and a crash) observe either the old content or the new one, never a
 * truncated or partially written file.
 *
 * The content is written to a temporary file in the same directory, flushed to the storage device,
 * and then renamed over the target. In detail:
 *
 * - if [path] is a symbolic link, the file it points to is replaced and the link is kept;
 * - the POSIX permissions of an existing target are copied to the new file (owner and group are not);
 * - if the target exists but is not a regular file (e.g. a device or a named pipe), it is written in place;
 * - if the file system cannot rename atomically, a plain replacing rename is used instead;
 * - on failure the temporary file is deleted and the target is left untouched.
 *
 * Replacing the file gives it a new identity: hard links to the old file keep the old content.
 *
 * @throws IOException if an I/O error occurs; [path] is then unchanged.
 */
internal fun writeAtomically(path: Path, charset: Charset, content: (Writer) -> Unit) {
    val target = resolveTarget(path)

    if (Files.isDirectory(target)) throw FileSystemException(target.toString(), null, "Is a directory")
    if (Files.exists(target) && !Files.isRegularFile(target)) {
        writeInPlace(target, charset, content)
        return
    }

    val directory = target.parent ?: throw FileSystemException(target.toString(), null, "No parent directory")
    val temp = createTempSibling(directory, target.fileName.toString())
    try {
        copyPermissions(target, temp)
        FileChannel.open(temp, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING).use { channel ->
            // newEncoder() reports unmappable characters instead of silently replacing them with '?'
            val writer = Channels.newWriter(channel, charset.newEncoder(), -1)
            content(writer)
            writer.flush()
            channel.force(true)
        }
        try {
            Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE)
        } catch (e: AtomicMoveNotSupportedException) {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING)
        }
    } catch (e: Throwable) {
        try {
            Files.deleteIfExists(temp)
        } catch (suppressed: IOException) {
            e.addSuppressed(suppressed)
        }
        throw e
    }
    syncDirectory(directory)
}

/**
 * Writes [path] directly, truncating it first: a failure may leave the file partially written.
 */
internal fun writeInPlace(path: Path, charset: Charset, content: (Writer) -> Unit) {
    Files.newBufferedWriter(path, charset).use(content)
}

/** Follows a symbolic link (also a dangling one) to the file that must actually be replaced. */
private fun resolveTarget(path: Path): Path {
    val absolute = path.toAbsolutePath()
    if (!Files.isSymbolicLink(absolute)) return absolute
    return try {
        absolute.toRealPath()
    } catch (e: NoSuchFileException) {
        // dangling link: create the file it points to
        absolute.resolveSibling(Files.readSymbolicLink(absolute)).normalize()
    }
}

/**
 * Creates an empty hidden file next to the target. [Files.createFile] (unlike [Files.createTempFile],
 * which uses mode 600) applies the default permissions of new files, as a direct write would.
 */
private fun createTempSibling(directory: Path, name: String): Path {
    while (true) {
        val suffix = java.lang.Long.toHexString(ThreadLocalRandom.current().nextLong())
        try {
            return Files.createFile(directory.resolve(".$name.$suffix.tmp"))
        } catch (e: FileAlreadyExistsException) {
            // extremely unlikely: try another name
        }
    }
}

private fun copyPermissions(from: Path, to: Path) {
    if (!Files.exists(from)) return
    try {
        Files.setPosixFilePermissions(to, Files.getPosixFilePermissions(from))
    } catch (e: UnsupportedOperationException) {
        // not a POSIX file system (e.g. Windows): nothing to copy
    }
}

/** Makes the rename durable on POSIX systems; not supported (and not needed) everywhere, hence best effort. */
private fun syncDirectory(directory: Path) {
    try {
        FileChannel.open(directory, StandardOpenOption.READ).use { it.force(true) }
    } catch (e: IOException) {
        // e.g. Windows does not allow opening a directory
    }
}
