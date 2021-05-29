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

import hep.dataforge.io.envelopes.DefaultEnvelopeType.Companion.SEPARATOR
import hep.dataforge.io.envelopes.Envelope.Companion.DATA_LENGTH_PROPERTY
import hep.dataforge.io.envelopes.Envelope.Companion.META_LENGTH_PROPERTY
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.channels.Channels

/**
 * @author darksnake
 */
class DefaultEnvelopeWriter(private val envelopeType: EnvelopeType, private val metaType: MetaType) : EnvelopeWriter {

    @Throws(IOException::class)
    override fun write(stream: OutputStream, envelope: Envelope) {
        val tag = EnvelopeTag().also {
            it.envelopeType = envelopeType
            it.metaType = metaType
        }
        write(stream, tag, envelope)
    }

    /**
     * Automatically define meta size and data size if it is not defined already
     * and write envelope to the stream
     *
     * @param stream
     * @param envelope
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun write(stream: OutputStream, tag: EnvelopeTag, envelope: Envelope) {

        val writer = tag.metaType.writer
        val meta: ByteArray
        val metaSize: Int
        if (envelope.meta.isEmpty) {
            meta = ByteArray(0)
            metaSize = 0
        } else {
            val baos = ByteArrayOutputStream()
            writer.write(baos, envelope.meta)
            meta = baos.toByteArray()
            metaSize = meta.size + 2
        }
        tag.setValue(META_LENGTH_PROPERTY, metaSize)

        tag.setValue(DATA_LENGTH_PROPERTY, envelope.data.size)

        stream.write(tag.toBytes().array())

        stream.write(meta)
        if (meta.isNotEmpty()) {
            stream.write(SEPARATOR)
        }

        Channels.newChannel(stream).write(envelope.data.buffer)
    }

//    companion object {
//        private val TAG_PROPERTIES = HashSet(
//                Arrays.asList(Envelope.TYPE_PROPERTY, Envelope.META_TYPE_PROPERTY, Envelope.META_LENGTH_PROPERTY, Envelope.DATA_LENGTH_PROPERTY)
//        )
//    }

}
