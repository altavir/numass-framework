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

package inr.numass.data

import hep.dataforge.io.envelopes.*
import hep.dataforge.values.Value
import hep.dataforge.values.parseValue
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/**
 * An envelope type for legacy numass tags. Reads legacy tag and writes DF02 tags
 */
class NumassEnvelopeType : EnvelopeType {

    override val code: Int = DefaultEnvelopeType.DEFAULT_ENVELOPE_CODE

    override val name: String = "numass"

    override fun description(): String = "Numass legacy envelope"

    /**
     * Read as legacy
     */
    override fun getReader(properties: Map<String, String>): EnvelopeReader {
        return NumassEnvelopeReader()
    }

    /**
     * Write as default
     */
    override fun getWriter(properties: Map<String, String>): EnvelopeWriter {
        return DefaultEnvelopeWriter(this, MetaType.resolve(properties))
    }

    class LegacyTag : EnvelopeTag() {
        override val startSequence: ByteArray
            get() = LEGACY_START_SEQUENCE

        override val endSequence: ByteArray
            get() = LEGACY_END_SEQUENCE

        /**
         * Get the length of tag in bytes. -1 means undefined size in case tag was modified
         *
         * @return
         */
        override val length: Int
            get() = 30

        /**
         * Read leagscy version 1 tag without leading tag head
         *
         * @param buffer
         * @return
         * @throws IOException
         */
        override fun readHeader(buffer: ByteBuffer): Map<String, Value> {
            val res = HashMap<String, Value>()

            val type = buffer.getInt(2)
            res[Envelope.TYPE_PROPERTY] = Value.of(type)

            val metaTypeCode = buffer.getShort(10)
            val metaType = MetaType.resolve(metaTypeCode)

            if (metaType != null) {
                res[Envelope.META_TYPE_PROPERTY] = metaType.name.parseValue()
            } else {
                LoggerFactory.getLogger(EnvelopeTag::class.java).warn("Could not resolve meta type. Using default")
            }

            val metaLength = Integer.toUnsignedLong(buffer.getInt(14))
            res[Envelope.META_LENGTH_PROPERTY] = Value.of(metaLength)
            val dataLength = Integer.toUnsignedLong(buffer.getInt(22))
            res[Envelope.DATA_LENGTH_PROPERTY] = Value.of(dataLength)
            return res
        }
    }

    private class NumassEnvelopeReader : DefaultEnvelopeReader() {
        override fun newTag(): EnvelopeTag {
            return LegacyTag()
        }
    }

    companion object {
        val INSTANCE = NumassEnvelopeType()

        val LEGACY_START_SEQUENCE = byteArrayOf('#'.code.toByte(), '!'.code.toByte())
        val LEGACY_END_SEQUENCE =
            byteArrayOf('!'.code.toByte(), '#'.code.toByte(), '\r'.code.toByte(), '\n'.code.toByte())

        /**
         * Replacement for standard type infer to include legacy type
         *
         * @param path
         * @return
         */
        fun infer(path: Path): EnvelopeType? {
            return try {
                FileChannel.open(path, StandardOpenOption.READ).use {
                    val buffer = it.map(FileChannel.MapMode.READ_ONLY, 0, 6)
                    when {
                        //TODO use templates from appropriate types
                        buffer.get(0) == '#'.code.toByte() && buffer.get(1) == '!'.code.toByte() -> INSTANCE
                        buffer.get(0) == '#'.code.toByte() && buffer.get(1) == '!'.code.toByte() &&
                                buffer.get(4) == 'T'.code.toByte() && buffer.get(5) == 'L'.code.toByte() -> TaglessEnvelopeType.INSTANCE
                        buffer.get(0) == '#'.code.toByte() && buffer.get(1) == '~'.code.toByte() -> DefaultEnvelopeType.INSTANCE
                        else -> null
                    }
                }
            } catch (ex: Exception) {
                LoggerFactory.getLogger(EnvelopeType::class.java).warn("Could not infer envelope type of file {} due to exception: {}", path, ex)
                null
            }

        }

    }

}
