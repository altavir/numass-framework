/*
 * Copyright  2018 Alexander Nozik.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package hep.dataforge.context

import hep.dataforge.context.FileReference.FileReferenceScope.*
import hep.dataforge.data.binary.Binary
import hep.dataforge.data.binary.FileBinary
import hep.dataforge.names.Name
import java.io.File
import java.io.OutputStream
import java.net.URI
import java.nio.channels.SeekableByteChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption


/**
 * A context aware reference to file with content not managed by DataForge
 */
class FileReference private constructor(override val context: Context, val path: Path, val scope: FileReferenceScope = WORK) : ContextAware {

    /**
     * Absolute path for this reference
     */
    val absolutePath: Path = when (scope) {
        SYS -> path
        DATA -> context.dataDir.resolve(path)
        WORK -> context.workDir.resolve(path)
        TMP -> context.tmpDir.resolve(path)
    }.toAbsolutePath()

    /**
     * The name of the file excluding path
     */
    val name: String = path.fileName.toString()

    /**
     * Get binary references by this file reference
     */
    val binary: Binary
        get() {
            return if (exists) {
                FileBinary(absolutePath)
            } else {
                Binary.EMPTY
            }
        }

    /**
     * A flag showing that internal modification of reference content is allowed
     */
    val mutable: Boolean = scope == WORK || scope == TMP

    val exists: Boolean = Files.exists(absolutePath)


    private fun prepareWrite() {
        if (!mutable) {
            throw RuntimeException("Trying to write to immutable file reference")
        }
        absolutePath.parent.apply {
            if (!Files.exists(this)) {
                Files.createDirectories(this)
            }
        }
    }

    /**
     * Write and replace content of the file
     */
    fun write(content: ByteArray) {
        prepareWrite()
        Files.write(absolutePath, content, StandardOpenOption.WRITE, StandardOpenOption.CREATE)
    }

    fun append(content: ByteArray) {
        prepareWrite()
        Files.write(absolutePath, content, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND)
    }


    /**
     * Output stream for this file reference
     *
     * TODO cache stream?
     */
    val outputStream: OutputStream
        get() {
            prepareWrite()
            return Files.newOutputStream(absolutePath, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
        }

    val channel: SeekableByteChannel
        get() {
            prepareWrite()
            return Files.newByteChannel(absolutePath, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
        }

    /**
     * Delete refenrenced file on exit
     */
    fun delete() {
        Files.deleteIfExists(absolutePath)
    }

//    /**
//     * Checksum of the file
//     */
//    val md5 = MessageDigest.getInstance("MD5").digest(Files.readAllBytes(absolutePath))


    enum class FileReferenceScope {
        SYS, // absolute system path, content immutable
        DATA, // a reference in data directory, content immutable
        WORK, // a reference in work directory, mutable
        TMP // A temporary file reference, mutable
    }

    companion object {

        private fun resolvePath(parent: Path, name: String): Path {
            return if (name.contains("://")) {
                Paths.get(URI(name))
            } else {
                parent.resolve(name)
            }
        }

        /**
         * Provide a reference to a new file in tmp directory with unique ID.
         */
        fun newTmpFile(context: Context, prefix: String, suffix: String = "tmp"): FileReference {
            val path = Files.createTempFile(context.tmpDir, prefix, suffix)
            return FileReference(context, path, TMP)
        }

        /**
         * Create a reference for a file in a work directory. File itself is not created
         */
        fun newWorkFile(context: Context, prefix: String, suffix: String, path: Name = Name.EMPTY): FileReference {
            val dir = if (path.isEmpty()) {
                context.workDir
            } else {
                val relativeDir = path.tokens.joinToString(File.separator) { it.toString() }
                resolvePath(context.workDir,relativeDir)
            }

            val file = dir.resolve("$prefix.$suffix")
            return FileReference(context, file, WORK)
        }

        /**
         * Create a reference using data scope file using path
         */
        fun openDataFile(context: Context, path: Path): FileReference {
            return FileReference(context, path, DATA)
        }

        fun openDataFile(context: Context, name: String): FileReference {
            val path = resolvePath(context.dataDir,name)
            return FileReference(context, path, DATA)
        }

        /**
         * Create a reference to the system scope file using path
         */
        fun openFile(context: Context, path: Path): FileReference {
            return FileReference(context, path, SYS)
        }

        /**
         * Create a reference to the system scope file using string
         */
        fun openFile(context: Context, path: String): FileReference {
            return FileReference(context, resolvePath(context.rootDir, path), SYS)
        }

    }
}