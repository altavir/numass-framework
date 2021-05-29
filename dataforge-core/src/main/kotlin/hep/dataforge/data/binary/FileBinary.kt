/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.data.binary

import java.io.IOException
import java.io.InputStream
import java.io.ObjectStreamException
import java.io.WriteAbortedException
import java.nio.ByteBuffer
import java.nio.channels.ByteChannel
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.nio.file.StandardOpenOption.READ

/**
 *
 * @param file File to create binary from
 * @param dataOffset  dataOffset form beginning of file
 */
class FileBinary(
        private val file: Path,
        private val dataOffset: Long = 0,
        private val _size: Long = -1
) : Binary {

    override val stream: InputStream
        get() = Files.newInputStream(file, READ).also { it.skip(dataOffset) }

    override val channel: ByteChannel
        get() = FileChannel.open(file, READ).position(dataOffset)

    override val buffer: ByteBuffer
        get() = read(0, size.toInt())

    /**
     * Read a buffer with given dataOffset in respect to data block start and given size. If data size w
     *
     * @param offset
     * @param size
     * @return
     * @throws IOException
     */
    override fun read(offset: Int, size: Int): ByteBuffer {
        return FileChannel.open(file, StandardOpenOption.READ).use { it.map(FileChannel.MapMode.READ_ONLY, dataOffset + offset, size.toLong())}
    }

    override val size: Long
        get() = if (_size >= 0) _size else Files.size(file) - dataOffset

    @Throws(ObjectStreamException::class)
    private fun writeReplace(): Any {
        try {
            return BufferedBinary(buffer.array())
        } catch (e: IOException) {
            throw WriteAbortedException("Failed to get byte buffer", e)
        }

    }

}
