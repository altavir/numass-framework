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
import hep.dataforge.meta.Meta

import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

/**
 * The simplest in-memory envelope
 *
 * @author Alexander Nozik
 */
open class SimpleEnvelope(meta: Meta = Meta.empty(), data: Binary = Binary.EMPTY) : Envelope {

    override var meta: Meta = meta
        protected set

    override var data: Binary = data
        protected set

    @Throws(IOException::class)
    private fun writeObject(out: ObjectOutputStream) {
        DefaultEnvelopeWriter(DefaultEnvelopeType.INSTANCE, binaryMetaType).write(out, this)
    }

    @Throws(IOException::class, ClassNotFoundException::class)
    private fun readObject(`in`: ObjectInputStream) {
        val envelope = DefaultEnvelopeReader.INSTANCE.read(`in`)

        this.meta = envelope.meta
        this.data = envelope.data
    }

}
