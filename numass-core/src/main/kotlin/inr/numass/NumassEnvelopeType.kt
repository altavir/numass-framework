package inr.numass

import hep.dataforge.io.envelopes.*
import hep.dataforge.values.Value
import hep.dataforge.values.parseValue
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.*

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

        val LEGACY_START_SEQUENCE = byteArrayOf('#'.toByte(), '!'.toByte())
        val LEGACY_END_SEQUENCE = byteArrayOf('!'.toByte(), '#'.toByte(), '\r'.toByte(), '\n'.toByte())

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
                    val array = ByteArray(6)
                    buffer.get(array)
                    val header = String(array)
                    when {
                        //TODO use templates from appropriate types
                        header.startsWith("#!") -> NumassEnvelopeType.INSTANCE
                        header.startsWith("#~DFTL") -> TaglessEnvelopeType.INSTANCE
                        header.startsWith("#~") -> DefaultEnvelopeType.INSTANCE
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
