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

/**
 * A value that delays its parsing allowing to parse meta from text much faster, since the parsing is the most expensive operation
 */
class LateParseValue(str: String) : AbstractValue() {

    private val _value: Value by lazy { str.parseValue() }
    override val value: Any
        get() = _value.value

    override val number: Number
        get() = _value.number
    override val boolean: Boolean
        get() = _value.boolean
    override val time: Instant
        get() = _value.time
    override val string: String
        get() = _value.string
    override val type: ValueType
        get() = _value.type

}