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
import java.util.*
import kotlin.collections.ArrayList

/**
 * A wrapper for value lists which could be used both as listValue and as value.
 * When used as value only first element of listValue is used. If the listValue
 * is empty, than ListValue is equivalent of Null value.
 *
 * @author Alexander Nozik
 */
class ListValue(values: Collection<Value>) : Value {

    private val values: List<Value> = ArrayList(values)

    override val number: Number
        get() = if (values.isNotEmpty()) {
            values[0].number
        } else {
            0
        }

    override val boolean: Boolean
        get() = values.isNotEmpty() && values[0].boolean

    override val time: Instant
        get() = if (values.size > 0) {
            values[0].time
        } else {
            Instant.ofEpochMilli(0)
        }

    override val string: String
        get() = if (values.isEmpty()) {
            ""
        } else if (values.size == 1) {
            values[0].string
        } else {
            values.joinToString(prefix = "[", postfix = "]", separator = ", ") { it.string }
        }

    override val type: ValueType
        get() = if (values.isNotEmpty()) {
            values[0].type
        } else {
            ValueType.NULL
        }

    override val list: List<Value>
        get() = this.values

    override val isList: Boolean
        get() = true


    override fun hashCode(): Int {
        var hash = 3
        hash = 53 * hash + Objects.hashCode(this.values)
        return hash
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) {
            return false
        }
        if (javaClass != other.javaClass) {
            return false
        }
        return this.values == (other as ListValue).values
    }

    override fun toString(): String {
        return string
    }

    override val value: Any
        get() {
            return Collections.unmodifiableList(this.values)
        }
}
