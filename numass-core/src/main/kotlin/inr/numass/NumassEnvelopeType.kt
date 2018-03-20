package inr.numass

import hep.dataforge.io.envelopes.*
import hep.dataforge.values.Value
import inr.numass.data.legacy.NumassFileEnvelope.Companion.LEGACY_END_SEQUENCE
import inr.numass.data.legacy.NumassFileEnvelope.Companion.LEGACY_START_SEQUENCE
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*

/**
 * An envelope type for legacy numass tags. Reads legacy tag and writes DF02 tags
 */
class NumassEnvelopeType : EnvelopeType {

    override val code: Int = DefaultEnvelopeType.DEFAULT_ENVELOPE_TYPE

    override val name: String = "numass"

    override fun description(): String = "Numass legacy envelope"

    override fun getReader(properties: Map<String, String>): EnvelopeReader {
        return NumassEnvelopeReader()
    }

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
                res[Envelope.META_TYPE_PROPERTY] = Value.of(metaType.name)
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

}
