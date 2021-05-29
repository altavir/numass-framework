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

import java.math.BigDecimal
import java.math.MathContext
import java.time.Instant
import java.util.*

/**
 *
 * @author Alexander Nozik
 */
internal class NumberValue(override val number: Number) : AbstractValue() {

    /**
     * {@inheritDoc}
     */
    override val boolean: Boolean
        get() = number.toDouble() > 0

    /**
     * {@inheritDoc}
     */
    override val string: String
        get() = number.toString()

    /**
     * {@inheritDoc}
     *
     * Время в СЕКУНДАХ
     */
    override val time: Instant
        get() = Instant.ofEpochMilli(number.toLong())

    /**
     * {@inheritDoc}
     */
    override val type: ValueType
        get() = ValueType.NUMBER

    /**
     * {@inheritDoc}
     */
    override fun hashCode(): Int {
        var hash = 7
        //TODO evaluate infinities
        hash = 59 * hash + Objects.hashCode(BigDecimal(this.number.toDouble(), MathContext.DECIMAL32))
        return hash
    }

    /**
     * {@inheritDoc}
     */
    override fun equals(other: Any?): Boolean {
        return when (other) {
            null -> false
            is Value -> ValueUtils.NUMBER_COMPARATOR.compare(this.number, other.number) == 0
            else -> false
        }
    }

    override val value: Any
        get() = this.number
}
