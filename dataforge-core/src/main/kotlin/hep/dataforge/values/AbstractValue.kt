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
 * Created by darksnake on 05-Aug-16.
 */
abstract class AbstractValue : Value {

    /**
     * Smart equality condition
     * @param other
     * @return
     */
    override fun equals(other: Any?): Boolean {
        //TODO add list values equality condition
        return when (other) {
            is Value -> {
                try {
                    when (type) {
                        ValueType.BOOLEAN -> this.boolean == other.boolean
                        ValueType.TIME -> this.time == other.time
                        ValueType.STRING -> this.string == other.string
                        ValueType.NUMBER -> ValueUtils.NUMBER_COMPARATOR.compare(this.number, other.number) == 0
                        ValueType.NULL -> other.type == ValueType.NULL
                        else ->
                            //unreachable statement, but using string comparison just to be sure
                            this.string == other.string
                    }
                } catch (ex: ValueConversionException) {
                    false
                }
            }

            is Double -> this.double == other
            is Int -> this.int == other
            is Number -> ValueUtils.NUMBER_COMPARATOR.compare(this.number, other as Number?) == 0
            is String -> this.string == other
            is Boolean -> this.boolean == other
            is Instant -> this.time == other
            null -> this.type == ValueType.NULL
            else -> super.equals(other)
        }
    }

    /**
     * Groovy smart cast support
     *
     * @param type
     * @return
     */
    fun asType(type: Class<*>): Any {
        return when {
            type.isAssignableFrom(String::class.java) -> this.string
            type.isAssignableFrom(Double::class.java) -> this.double
            type.isAssignableFrom(Int::class.java) -> this.int
            type.isAssignableFrom(Number::class.java) -> this.number
            type.isAssignableFrom(Boolean::class.java) -> this.boolean
            type.isAssignableFrom(Instant::class.java) -> this.time
            else -> type.cast(this)
        }
    }

    override fun toString(): String {
        return string
    }
}
