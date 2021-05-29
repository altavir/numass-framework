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

import hep.dataforge.values.Value
import hep.dataforge.values.ValueType
import hep.dataforge.values.asValue
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.SeekableByteChannel
import java.util.*

/**
 * Envelope tag converter v2
 * Created by darksnake on 25-Feb-17.
 */
open class EnvelopeTag {

    val values = HashMap<String, Value>()
    var metaType: MetaType = xmlMetaType
    var envelopeType: EnvelopeType = DefaultEnvelopeType.INSTANCE


    protected open val startSequence: ByteArray = "#~".toByteArray()

    protected open val endSequence: ByteArray = "~#\r\n".toByteArray()

    /**
     * Get the length of tag in bytes. -1 means undefined size in case tag was modified
     *
     * @return
     */
    open val length: Int = 20

    val metaSize: Int
        get() = values[Envelope.META_LENGTH_PROPERTY]?.int ?: 0

    var dataSize: Int
        get() = values[Envelope.DATA_LENGTH_PROPERTY]?.int ?: 0
        set(value) {
            values[Envelope.DATA_LENGTH_PROPERTY] = value.asValue()
        }

    /**
     * Read header line
     *
     * @throws IOException
     */
    @Throws(IOException::class)
    protected open fun readHeader(buffer: ByteBuffer): Map<String, Value> {

        val res = HashMap<String, Value>()

        val lead = ByteArray(2)

        buffer.get(lead)

        if (!Arrays.equals(lead, startSequence)) {
            throw IOException("Wrong start sequence for envelope tag")
        }

        //reading type
        val type = buffer.getInt(2)
        val envelopeType = EnvelopeType.resolve(type)

        if (envelopeType != null) {
            res[Envelope.TYPE_PROPERTY] = envelopeType.name.asValue()
        } else {
            LoggerFactory.getLogger(EnvelopeTag::class.java).warn("Could not resolve envelope type code. Using default")
        }

        //reading meta type
        val metaTypeCode = buffer.getShort(6)
        val metaType = MetaType.resolve(metaTypeCode)

        if (metaType != null) {
            res[Envelope.META_TYPE_PROPERTY] = metaType.name.asValue()
        } else {
            LoggerFactory.getLogger(EnvelopeTag::class.java).warn("Could not resolve meta type. Using default")
        }

        //reading meta length
        val metaLength = Integer.toUnsignedLong(buffer.getInt(8))
        res[Envelope.META_LENGTH_PROPERTY] = Value.of(metaLength)
        //reading data length
        val dataLength = Integer.toUnsignedLong(buffer.getInt(12))
        res[Envelope.DATA_LENGTH_PROPERTY] = Value.of(dataLength)

        val endSequence = ByteArray(4)
        buffer.position(16)
        buffer.get(endSequence)

        if (!Arrays.equals(endSequence, endSequence)) {
            throw IOException("Wrong ending sequence for envelope tag")
        }
        return res
    }

    /**
     * Convert tag to properties
     *
     * @return
     */
    fun getValues(): Map<String, Value> {
        return values
    }

    /**
     * Update existing properties
     *
     * @param props
     */
    fun setValues(props: Map<String, Value>) {
        props.forEach { name, value -> this.setValue(name, value) }
    }

    fun setValue(name: String, value: Any) {
        setValue(name, Value.of(value))
    }

    fun setValue(name: String, value: Value) {
        if (Envelope.TYPE_PROPERTY == name) {
            val type = if (value.type == ValueType.NUMBER) EnvelopeType.resolve(value.int) else EnvelopeType.resolve(value.string)
            if (type != null) {
                envelopeType = type
            } else {
                LoggerFactory.getLogger(javaClass).trace("Can't resolve envelope type")
            }
        } else if (Envelope.META_TYPE_PROPERTY == name) {
            val type = if (value.type == ValueType.NUMBER) MetaType.resolve(value.int.toShort()) else MetaType.resolve(value.string)
            if (type != null) {
                metaType = type
            } else {
                LoggerFactory.getLogger(javaClass).error("Can't resolve meta type")
            }
        } else {
            values[name] = value
        }
    }

    @Throws(IOException::class)
    fun read(channel: SeekableByteChannel): EnvelopeTag {
        val header: Map<String, Value>

        val bytes = ByteBuffer.allocate(length)
        channel.read(bytes)
        bytes.flip()
        header = readHeader(bytes)

        setValues(header)
        return this
    }

    @Throws(IOException::class)
    fun read(stream: InputStream): EnvelopeTag {
        val header: Map<String, Value>
        val body = ByteArray(length)
        stream.read(body)
        header = readHeader(ByteBuffer.wrap(body).order(ByteOrder.BIG_ENDIAN))
        setValues(header)
        return this
    }

    fun toBytes(): ByteBuffer {
        val buffer = ByteBuffer.allocate(20)
        buffer.put(startSequence)

        buffer.putInt(envelopeType.code)
        buffer.putShort(metaType.codes[0])
        buffer.putInt(values[Envelope.META_LENGTH_PROPERTY]!!.long.toInt())
        buffer.putInt(values[Envelope.DATA_LENGTH_PROPERTY]!!.long.toInt())
        buffer.put(endSequence)
        buffer.position(0)
        return buffer
    }

}
