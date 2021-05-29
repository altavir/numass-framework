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

import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder
import hep.dataforge.meta.MetaMorph
import hep.dataforge.names.NameSetContainer
import java.util.*

/**
 * A named set of values with fixed name list.
 */
interface Values : NameSetContainer, ValueProvider, MetaMorph, Iterable<NamedValue> {

    /**
     * Faster search for existing values
     *
     * @param path
     * @return
     */
    @JvmDefault
    override fun hasValue(path: String): Boolean {
        return this.names.contains(path)
    }

    /**
     * A convenient method to access value by its index. Has generally worse performance.
     *
     * @param num
     * @return
     */
    @JvmDefault
    operator fun get(num: Int): Value {
        return getValue(this.names.get(num))
    }

    @JvmDefault
    operator fun get(key: String): Value {
        return getValue(key)
    }

    /**
     * Convert a DataPoint to a Map. Order is not guaranteed
     * @return
     */
    @JvmDefault
    fun asMap(): Map<String, Value> {
        val res = HashMap<String, Value>()
        for (field in this.names) {
            res[field] = getValue(field)
        }
        return res
    }

    @JvmDefault
    override fun iterator(): Iterator<NamedValue> {
        return names.map { NamedValue(it, get(it)) }.iterator()
    }

    /**
     * Simple check for boolean tag
     *
     * @param name
     * @return
     */
    @JvmDefault
    fun hasTag(name: String): Boolean {
        return names.contains(name) && getValue(name).boolean
    }

    @JvmDefault
    override fun toMeta(): Meta {
        val builder = MetaBuilder("point")
        for (name in namesAsArray()) {
            builder.putValue(name, getValue(name))
        }
        return builder.build()
    }
}

fun Values.builder() = ValueMap.Builder(this)

fun Values.edit(block: ValueMap.Builder.()->Unit) = builder().apply(block).build()