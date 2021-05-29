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
import hep.dataforge.io.envelopes.Envelope.Companion.ENVELOPE_DATA_TYPE_KEY
import hep.dataforge.io.envelopes.Envelope.Companion.ENVELOPE_DESCRIPTION_KEY
import hep.dataforge.io.envelopes.Envelope.Companion.ENVELOPE_TIME_KEY
import hep.dataforge.io.envelopes.Envelope.Companion.ENVELOPE_TYPE_KEY
import hep.dataforge.meta.KMetaBuilder
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder
import hep.dataforge.meta.buildMeta

import java.io.ByteArrayOutputStream
import java.io.ObjectStreamException
import java.io.OutputStream
import java.nio.ByteBuffer
import java.time.Instant

/**
 * The convenient builder for envelopes
 *
 * @author Alexander Nozik
 */
class EnvelopeBuilder : Envelope {

    /**
     * Get modifiable meta builder for this envelope
     *
     * @return
     */
    override var meta = MetaBuilder()

    //initializing with empty buffer
    override var data: Binary = BufferedBinary(ByteArray(0))
        private set

    constructor(envelope: Envelope) {
        this.meta = envelope.meta.builder
        this.data = envelope.data
    }

    constructor() {

    }

    fun meta(annotation: Meta): EnvelopeBuilder {
        this.meta = annotation.builder
        return this
    }

    fun meta(builder: KMetaBuilder.()->Unit) : EnvelopeBuilder{
        return meta(buildMeta("meta", builder))
    }

    /**
     * Helper to fast put node to envelope meta
     *
     * @param element
     * @return
     */
    fun putMetaNode(nodeName: String, element: Meta): EnvelopeBuilder {
        this.meta.putNode(nodeName, element)
        return this
    }

    fun putMetaNode(element: Meta): EnvelopeBuilder {
        this.meta.putNode(element)
        return this
    }

    /**
     * Helper to fast put value to meta root
     *
     * @param name
     * @param value
     * @return
     */
    fun setMetaValue(name: String, value: Any): EnvelopeBuilder {
        this.meta.setValue(name, value)
        return this
    }

    fun data(data: Binary): EnvelopeBuilder {
        this.data = data
        return this
    }

    fun data(data: ByteBuffer): EnvelopeBuilder {
        this.data = BufferedBinary(data)
        return this
    }

    fun data(data: ByteArray): EnvelopeBuilder {
        this.data = BufferedBinary(data)
        return this
    }

    fun data(consumer: (OutputStream) -> Unit): EnvelopeBuilder {
        val baos = ByteArrayOutputStream()
        consumer(baos)
        return data(baos.toByteArray())
    }

    //TODO kotlinize

    fun setEnvelopeType(type: String): EnvelopeBuilder {
        setMetaValue(ENVELOPE_TYPE_KEY, type)
        return this
    }

    fun setDataType(type: String): EnvelopeBuilder {
        setMetaValue(ENVELOPE_DATA_TYPE_KEY, type)
        return this
    }

    fun setDescription(description: String): EnvelopeBuilder {
        setMetaValue(ENVELOPE_DESCRIPTION_KEY, description)
        return this
    }

    fun build(): Envelope {
        setMetaValue(ENVELOPE_TIME_KEY, Instant.now())
        return SimpleEnvelope(this.meta, data)
    }

    @Throws(ObjectStreamException::class)
    private fun writeReplace(): Any {
        return SimpleEnvelope(this.meta, data)
    }
}

fun buildEnvelope(action: EnvelopeBuilder.()-> Unit): Envelope = EnvelopeBuilder().apply(action).build()
