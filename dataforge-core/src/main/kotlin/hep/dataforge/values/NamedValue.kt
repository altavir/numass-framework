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
package hep.dataforge.values

import hep.dataforge.Named

import java.time.Instant

/**
 * Content value
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
class NamedValue(override val name: String, val anonymous: Value) : Named, Value {

    /**
     * {@inheritDoc}
     */
    override val boolean: Boolean
        get() = anonymous.boolean

    /**
     * {@inheritDoc}
     */
    override val number: Number
        get() = anonymous.number

    /**
     * {@inheritDoc}
     */
    override val string: String
        get() = anonymous.string

    /**
     * {@inheritDoc}
     *
     * @return
     */
    override val time: Instant
        get() = anonymous.time

    /**
     * {@inheritDoc}
     *
     * @return
     */
    override val type: ValueType
        get() = anonymous.type

    override val value: Any
        get() {
            return anonymous.value
        }

    companion object {

        fun of(name: String, value: Any): NamedValue {
            return NamedValue(name, Value.of(value))
        }
    }

}
