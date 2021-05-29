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

import hep.dataforge.exceptions.ValueConversionException

import java.time.Instant

/**
 * @author Alexander Nozik
 */
internal class BooleanValue private constructor(override val boolean: Boolean) : AbstractValue() {

    /**
     * {@inheritDoc}
     */
    override val number: Number
        get() = if (boolean) {
            1
        } else {
            0
        }

    /**
     * {@inheritDoc}
     */
    override val string: String
        get() = java.lang.Boolean.toString(boolean)

    /**
     * {@inheritDoc}
     */
    override val time: Instant
        get() = throw ValueConversionException(this, ValueType.TIME)

    /**
     * {@inheritDoc}
     */
    override val type: ValueType
        get() = ValueType.BOOLEAN

    /**
     * {@inheritDoc}
     */
    override fun hashCode(): Int {
        var hash = 3
        hash = 11 * hash + if (this.boolean) 1 else 0
        return hash
    }

    /**
     * {@inheritDoc}
     */
    override fun equals(other: Any?): Boolean {
        return when (other) {
            null -> false
            is Value -> this.boolean == other.boolean
            else -> false
        }
    }

    override val value: Any
        get() {
            return this.boolean
        }

    companion object {

        var TRUE: Value = BooleanValue(true)
        var FALSE: Value = BooleanValue(false)

        fun ofBoolean(b: Boolean): Value {
            return if (b) {
                TRUE
            } else {
                FALSE
            }
        }

        fun ofBoolean(b: String): Value {
            return ofBoolean(java.lang.Boolean.parseBoolean(b))
        }
    }
}
