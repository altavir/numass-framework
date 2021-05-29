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
 * A lazy calculated value defined by some supplier. The value is calculated
 * only once on first call, after that it is stored and not recalculated.
 *
 *
 * **WARNING** Since the value is calculated on demand it is not
 * strictly immutable. Use it only then it is impossible to avoid or ensure that
 * supplier does not depend on external state.
 *
 *
 * @author Darksnake
 */
class LazyValue(override val type: ValueType, supplier: () -> Value) : AbstractValue() {

    override val value: Value by lazy(supplier)

    override val number: Number
        get() = value.number

    override val boolean: Boolean
        get() = value.boolean

    override val time: Instant
        get() = value.time

    override val string: String
        get() = value.string

}
