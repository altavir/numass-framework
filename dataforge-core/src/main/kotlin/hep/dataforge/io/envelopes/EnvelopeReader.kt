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


import hep.dataforge.io.BufferChannel
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.channels.ReadableByteChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption.READ

/**
 * interface for reading envelopes
 *
 * @author Alexander Nozik
 */
interface EnvelopeReader {

    /**
     * Read the whole envelope from the stream.
     *
     * @param stream
     * @return
     * @throws IOException
     */
    fun read(stream: InputStream): Envelope

    /**
     * Read the envelope from channel
     */
    @JvmDefault
    fun read(channel: ReadableByteChannel): Envelope {
        return read(Channels.newInputStream(channel))
    }

    /**
     * Read the envelope from buffer (could produce lazy envelope)
     */
    @JvmDefault
    fun read(buffer: ByteBuffer): Envelope {
        return read(BufferChannel(buffer))//read(ByteArrayInputStream(buffer.array()))
    }

    /**
     * Read the envelope from NIO file (could produce lazy envelope)
     */
    @JvmDefault
    fun read(file: Path): Envelope {
        return Files.newByteChannel(file, READ).use { read(it) }
    }

    companion object {

        /**
         * Resolve envelope type and use it to read the file as envelope
         *
         * @param path
         * @return
         */
        @Throws(IOException::class)
        fun readFile(path: Path): Envelope {
            val type = EnvelopeType.infer(path) ?: error("The file is not envelope")
            return type.reader.read(path)
        }
    }
}
