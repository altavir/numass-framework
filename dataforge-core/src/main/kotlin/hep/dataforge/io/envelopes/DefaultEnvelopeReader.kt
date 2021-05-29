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
package hep.dataforge.io.envelopes

import hep.dataforge.data.binary.Binary
import hep.dataforge.data.binary.BufferedBinary
import hep.dataforge.data.binary.FileBinary
import hep.dataforge.exceptions.EnvelopeFormatException
import hep.dataforge.io.envelopes.DefaultEnvelopeType.Companion.SEPARATOR
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaNode.DEFAULT_META_NAME
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.channels.ReadableByteChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption.READ
import java.text.ParseException

/**
 * @author darksnake
 */
open class DefaultEnvelopeReader : EnvelopeReader {

    protected open fun newTag(): EnvelopeTag {
        return EnvelopeTag()
    }

    override fun read(channel: ReadableByteChannel): Envelope {
        return read(Channels.newInputStream(channel))
    }

    @Throws(IOException::class)
    override fun read(stream: InputStream): Envelope {
        val tag = newTag().read(stream)
        val parser = tag.metaType.reader
        val metaLength = tag.metaSize
        val meta: Meta = if (metaLength == 0) {
            Meta.buildEmpty(DEFAULT_META_NAME)
        } else {
            try {
                parser.read(stream, metaLength.toLong())
            } catch (ex: ParseException) {
                throw EnvelopeFormatException("Error parsing meta", ex)
            }

        }

        val binary: Binary
        val dataLength = tag.dataSize
        //skipping separator for automatic meta reading
        if (metaLength == -1) {
            stream.skip(separator().size.toLong())
        }
        binary = readData(stream, dataLength)

        return SimpleEnvelope(meta, binary)
    }

    /**
     * The envelope is lazy meaning it will be calculated on demand. If the
     * stream will be closed before that, than an error will be thrown. In order
     * to avoid this problem, it is wise to call `getData` after read.
     *
     * @return
     */
    override fun read(file: Path): Envelope {
        val channel = Files.newByteChannel(file, READ)
        val tag = newTag().read(channel)
        val metaLength = tag.metaSize
        val dataLength = tag.dataSize
        if (metaLength < 0 || dataLength < 0) {
            LoggerFactory.getLogger(javaClass).error("Can't lazy read infinite data or meta. Returning non-lazy envelope")
            return read(file)
        }

        val metaBuffer = ByteBuffer.allocate(metaLength)
        channel.position(tag.length.toLong())
        channel.read(metaBuffer)
        val parser = tag.metaType.reader

        val meta: Meta = if (metaLength == 0) {
            Meta.buildEmpty(DEFAULT_META_NAME)
        } else {
            try {
                parser.readBuffer(metaBuffer)
            } catch (ex: ParseException) {
                throw EnvelopeFormatException("Error parsing annotation", ex)
            }

        }
        channel.close()

        return SimpleEnvelope(meta, FileBinary(file, (tag.length + metaLength).toLong()))
    }

    protected fun separator(): ByteArray {
        return SEPARATOR
    }

    @Throws(IOException::class)
    private fun readData(stream: InputStream, length: Int): Binary {
        return if (length == -1) {
            val baos = ByteArrayOutputStream()
            while (stream.available() > 0) {
                baos.write(stream.read())
            }
            BufferedBinary(baos.toByteArray())
        } else {
            val buffer = ByteBuffer.allocate(length)
            Channels.newChannel(stream).read(buffer)
            BufferedBinary(buffer)
        }
    }

    /**
     * Read envelope with data (without lazy reading)
     *
     * @param stream
     * @return
     * @throws IOException
     */
    @Throws(IOException::class)
    fun readWithData(stream: InputStream): Envelope {
        val res = read(stream)
        res.data
        return res
    }

    companion object {

        val INSTANCE = DefaultEnvelopeReader()
    }

}
