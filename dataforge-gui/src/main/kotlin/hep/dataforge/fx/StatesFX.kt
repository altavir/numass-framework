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

package hep.dataforge.fx

import hep.dataforge.states.State
import hep.dataforge.values.Value
import javafx.beans.property.*
import tornadofx.*

fun <T : Any> State<T>.asProperty(): ObjectProperty<T> {
    val property = SimpleObjectProperty<T>(null, this.name, this.value)
    onChange { property.set(it) }
    property.onChange { this.set(it) }
    return property
}

fun State<Value>.asStringProperty(): StringProperty {
    val property = SimpleStringProperty(null, this.name, this.value.string)
    onChange { property.set(it.string) }
    property.onChange { this.set(it) }
    return property
}

fun State<Value>.asBooleanProperty(): BooleanProperty {
    val property = SimpleBooleanProperty(null, this.name, this.value.boolean)
    onChange { property.set(it.boolean) }
    property.onChange { this.set(it) }
    return property
}

fun State<Value>.asDoubleProperty(): DoubleProperty {
    val property = SimpleDoubleProperty(null, this.name, this.value.double)
    onChange { property.set(it.double) }
    property.onChange { this.set(it) }
    return property
}