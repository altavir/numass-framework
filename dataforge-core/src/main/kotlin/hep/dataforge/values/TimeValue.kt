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

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

/**
 * @author Alexander Nozik
 */
internal class TimeValue(override val time: Instant) : AbstractValue() {

    constructor(value: LocalDateTime) : this(value.toInstant(ZoneOffset.UTC))

    /**
     * {@inheritDoc}
     */
    override val boolean: Boolean
        get() = time.isAfter(Instant.MIN)

    /**
     * {@inheritDoc}
     */
    override val number: Number
        get() = time.toEpochMilli()

    /**
     * {@inheritDoc}
     */
    override//        return LocalDateTime.ofInstant(value, ZoneId.systemDefault()).toString();
    val string: String
        get() = time.toString()

    /**
     * {@inheritDoc}
     */
    override val type: ValueType
        get() = ValueType.TIME


    /**
     * {@inheritDoc}
     */
    override fun hashCode(): Int {
        var hash = 3
        hash = 89 * hash + Objects.hashCode(this.time)
        return hash
    }

    /**
     * {@inheritDoc}
     */
    override fun equals(other: Any?): Boolean {
        return when (other) {
            null -> false
            is Value -> this.time == other.time
            else -> false
        }
    }

    override val value: Any
        get() {
            return this.time
        }
}
