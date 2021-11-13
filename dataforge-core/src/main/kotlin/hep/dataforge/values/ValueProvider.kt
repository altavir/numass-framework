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

import hep.dataforge.exceptions.NameNotFoundException
import hep.dataforge.providers.Provides
import java.time.Instant
import java.util.*

interface ValueProvider {


    fun hasValue(path: String): Boolean {
        return optValue(path).isPresent
    }

    @Provides(VALUE_TARGET)
    fun optValue(path: String): Optional<Value>


    fun getValue(path: String): Value {
        return optValue(path).orElseThrow<NameNotFoundException> { NameNotFoundException(path) }
    }

    @Provides(BOOLEAN_TARGET)

    fun optBoolean(name: String): Optional<Boolean> {
        return optValue(name).map<Boolean> { it.boolean }
    }


    fun getBoolean(name: String, def: Boolean): Boolean {
        return optValue(name).map<Boolean> { it.boolean }.orElse(def)
    }


    fun getBoolean(name: String, def: () -> Boolean): Boolean {
        return optValue(name).map<Boolean> { it.boolean }.orElseGet(def)
    }


    fun getBoolean(name: String): Boolean {
        return getValue(name).boolean
    }

    @Provides(NUMBER_TARGET)

    fun optNumber(name: String): Optional<Number> {
        return optValue(name).map<Number> { it.number }
    }


    fun getDouble(name: String, def: Double): Double {
        return optValue(name).map<Double> { it.double }.orElse(def)
    }


    fun getDouble(name: String, def: () -> Double): Double {
        return optValue(name).map<Double> { it.double }.orElseGet(def)
    }


    fun getDouble(name: String): Double {
        return getValue(name).double
    }


    fun getInt(name: String, def: Int): Int {
        return optValue(name).map<Int> { it.int }.orElse(def)
    }


    fun getInt(name: String, def: () -> Int): Int {
        return optValue(name).map<Int> { it.int }.orElseGet(def)

    }


    fun getInt(name: String): Int {
        return getValue(name).int
    }


    @Provides(STRING_TARGET)
    fun optString(name: String): Optional<String> {
        return optValue(name).map<String> { it.string }
    }


    fun getString(name: String, def: String): String {
        return optString(name).orElse(def)
    }


    fun getString(name: String, def: () -> String): String {
        return optString(name).orElseGet(def)
    }


    fun getString(name: String): String {
        return getValue(name).string
    }


    fun getValue(name: String, def: Any): Value {
        return optValue(name).orElse(Value.of(def))
    }


    fun getValue(name: String, def: () -> Value): Value {
        return optValue(name).orElseGet(def)
    }

    @Provides(TIME_TARGET)

    fun optTime(name: String): Optional<Instant> {
        return optValue(name).map { it.time }
    }


    fun getStringArray(name: String): Array<String> {
        val vals = getValue(name).list
        return Array(vals.size) { vals[it].string }
    }


    fun getStringArray(name: String, def: () -> Array<String>): Array<String> {
        return if (this.hasValue(name)) {
            getStringArray(name)
        } else {
            def()
        }
    }


    fun getStringArray(name: String, def: Array<String>): Array<String> {
        return if (this.hasValue(name)) {
            getStringArray(name)
        } else {
            def
        }
    }

    companion object {

        const val VALUE_TARGET = "value"
        const val STRING_TARGET = "string"
        const val NUMBER_TARGET = "number"
        const val BOOLEAN_TARGET = "boolean"
        const val TIME_TARGET = "time"
    }
}
