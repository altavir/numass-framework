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

import hep.dataforge.data.binary.BufferedBinary
import hep.dataforge.io.envelopes.Envelope.Companion.DATA_LENGTH_PROPERTY
import hep.dataforge.meta.Meta
import java.io.*
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.channels.ReadableByteChannel
import java.text.ParseException
import java.util.regex.Pattern

/**
 * A tagless envelope. No tag. Data infinite by default
 */
class TaglessEnvelopeType : EnvelopeType {

    override val code: Int = 0x4446544c //DFTL

    override val name: String = TAGLESS_ENVELOPE_TYPE

    override fun description(): String {
        return "Tagless envelope. Text only. By default uses XML meta with utf encoding and data end auto-detection."
    }

    override fun getReader(properties: Map<String, String>): EnvelopeReader = TaglessReader(properties)

    override fun getWriter(properties: Map<String, String>): EnvelopeWriter = TaglessWriter(properties)

    class TaglessWriter(var properties: Map<String, String> = emptyMap()) : EnvelopeWriter {

        @Throws(IOException::class)
        override fun write(stream: OutputStream, envelope: Envelope) {
            val writer = PrintWriter(stream)

            //printing header
            writer.println(TAGLESS_ENVELOPE_HEADER)

            //printing all properties
            properties.forEach { key, value -> writer.printf("#? %s: %s;%n", key, value) }
            writer.printf("#? %s: %s;%n", DATA_LENGTH_PROPERTY, envelope.data.size)

            //Printing meta
            if (envelope.hasMeta()) {
                //print optional meta start tag
                writer.println(properties.getOrDefault(META_START_PROPERTY, DEFAULT_META_START))
                writer.flush()

                //define meta type
                val metaType = MetaType.resolve(properties)

                //writing meta
                metaType.writer.write(stream, envelope.meta)
            }

            //Printing data
            if (envelope.hasData()) {
                //print mandatory data start tag
                writer.println(properties.getOrDefault(DATA_START_PROPERTY, DEFAULT_DATA_START))
                writer.flush()
                Channels.newChannel(stream).write(envelope.data.buffer)
            }
            stream.flush()

        }
    }

    class TaglessReader(private val override: Map<String, String>) : EnvelopeReader {

        @Throws(IOException::class)
        override fun read(stream: InputStream): Envelope = read(Channels.newChannel(stream))

        override fun read(channel: ReadableByteChannel): Envelope {
            val properties = HashMap(override)
            val buffer = ByteBuffer.allocate(BUFFER_SIZE).apply { position(BUFFER_SIZE) }
            val meta = readMeta(channel, buffer, properties)
            return LazyEnvelope(meta) { BufferedBinary(readData(channel, buffer, properties)) }
        }


        /**
         * Read lines using provided channel and buffer. Buffer state is changed by this operation
         */
        private fun readLines(channel: ReadableByteChannel, buffer: ByteBuffer): Sequence<String> = sequence {
            val builder = ByteArrayOutputStream()
            while (true) {
                if (!buffer.hasRemaining()) {
                    if (!channel.isOpen) {
                        break
                    }
                    buffer.flip()
                    val count = channel.read(buffer)
                    buffer.flip()
                    if (count < BUFFER_SIZE) {
                        channel.close()
                    }
                }
                val b = buffer.get()
                builder.write(b.toInt())
                if (b == '\n'.code.toByte()) {
                    yield(String(builder.toByteArray(), Charsets.UTF_8))
                    builder.reset()
                }
            }
        }

        @Throws(IOException::class)
        private fun readMeta(
            channel: ReadableByteChannel,
            buffer: ByteBuffer,
            properties: MutableMap<String, String>,
        ): Meta {
            val sb = StringBuilder()
            val metaEnd = properties.getOrDefault(DATA_START_PROPERTY, DEFAULT_DATA_START)
            readLines(channel, buffer).takeWhile { it.trim { char -> char <= ' ' } != metaEnd }.forEach { line ->
                if (line.startsWith("#?")) {
                    readProperty(line.trim(), properties)
                } else if (line.isEmpty() || line.startsWith("#~")) {
                    //Ignore headings, do nothing
                } else {
                    sb.append(line).append("\r\n")
                }
            }


            return if (sb.isEmpty()) {
                Meta.empty()
            } else {
                val metaType = MetaType.resolve(properties)
                try {
                    metaType.reader.readString(sb.toString())
                } catch (e: ParseException) {
                    throw RuntimeException("Failed to parse meta", e)
                }

            }
        }


        @Throws(IOException::class)
        private fun readData(
            channel: ReadableByteChannel,
            buffer: ByteBuffer,
            properties: Map<String, String>,
        ): ByteBuffer {
            val array = ByteArray(buffer.remaining());
            buffer.get(array)
            if (properties.containsKey(DATA_LENGTH_PROPERTY)) {
                val result = ByteBuffer.allocate(Integer.parseInt(properties[DATA_LENGTH_PROPERTY]))
                result.put(array)//TODO fix it to not use direct array access
                if(result.limit() < result.capacity()) {
                    channel.read(result)
                }
                return result
            } else {
                val baos = ByteArrayOutputStream()
                baos.write(array)
                while (channel.isOpen) {
                    val read = channel.read(buffer)
                    buffer.flip()
                    if (read < BUFFER_SIZE) {
                        channel.close()
                    }

                    baos.write(buffer.array())
                }
                val remainingArray: ByteArray = ByteArray(buffer.remaining())
                buffer.get(remainingArray)
                baos.write(remainingArray)
                return ByteBuffer.wrap(baos.toByteArray())
            }
        }

        private fun readProperty(line: String, properties: MutableMap<String, String>) {
            val pattern = Pattern.compile("#\\?\\s*(?<key>[\\w.]*)\\s*:\\s*(?<value>[^;]*);?")
            val matcher = pattern.matcher(line)
            if (matcher.matches()) {
                val key = matcher.group("key")
                val value = matcher.group("value")
                properties.putIfAbsent(key, value)
            } else {
                throw RuntimeException("Custom property definition does not match format")
            }
        }
    }

    companion object {
        const val TAGLESS_ENVELOPE_TYPE = "tagless"

        const val TAGLESS_ENVELOPE_HEADER = "#~DFTL~#"
        const val META_START_PROPERTY = "metaSeparator"
        const val DEFAULT_META_START = "#~META~#"
        const val DATA_START_PROPERTY = "dataSeparator"
        const val DEFAULT_DATA_START = "#~DATA~#"
        private val BUFFER_SIZE = 1024
        val INSTANCE = TaglessEnvelopeType()
    }

}
