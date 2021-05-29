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

import java.io.ObjectStreamException
import java.util.function.Supplier

/**
 * The envelope that does not store data part in memory but reads it on demand.
 *
 * @property Return supplier of data for lazy calculation. The supplier is supposed to
 * @author darksnake
 */
class LazyEnvelope(override val meta: Meta, private val dataSupplier: Supplier<Binary>) : Envelope {

    constructor(meta: Meta, sup: () -> Binary) : this(meta, Supplier(sup))

    /**
     * Calculate data buffer if it is not already calculated and return result.
     *
     * @return
     */
    override val data: Binary by lazy { dataSupplier.get() }

    @Throws(ObjectStreamException::class)
    private fun writeReplace(): Any {
        return SimpleEnvelope(meta, data)
    }

}
