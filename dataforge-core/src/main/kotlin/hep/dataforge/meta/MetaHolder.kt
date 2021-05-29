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

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.meta

import hep.dataforge.description.Described
import hep.dataforge.description.NodeDescriptor
import hep.dataforge.optional
import hep.dataforge.utils.Optionals
import hep.dataforge.values.Value
import hep.dataforge.values.ValueProvider
import java.util.*

/**
 * The base class for `Meta` objects with immutable meta which also
 * implements ValueProvider and Described interfaces
 *
 * @author Alexander Nozik
 */
open class MetaHolder(override val meta: Meta) : Metoid, Described, ValueProvider {

    override val descriptor: NodeDescriptor by lazy {
        super.descriptor
    }

    /**
     * If this object's meta provides given value, return it, otherwise, use
     * descriptor
     *
     * @param path
     * @return
     */
    override fun optValue(path: String): Optional<Value> {
        return Optionals
                .either(meta.optValue(path))
                .or { descriptor.getValueDescriptor(path)?.default.optional }
                .opt()
    }

    /**
     * true if this object's meta or description contains the value
     *
     * @param path
     * @return
     */
    override fun hasValue(path: String): Boolean {
        return meta.hasValue(path) || descriptor.hasDefaultForValue(path)
    }

}
