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
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder
import hep.dataforge.meta.MetaMorph
import hep.dataforge.names.NameList
import hep.dataforge.utils.GenericBuilder
import java.util.*

/**
 * A simple [Values] implementation using HashMap.
 *
 * @author Alexander Nozik
 */

class ValueMap : Values, MetaMorph {


    private val valueMap = LinkedHashMap<String, Value>()

    /**
     * Serialization constructor
     */
    constructor(meta: Meta) {
        meta.valueNames.forEach { valName -> valueMap[valName] = meta.getValue(valName) }
    }

    constructor(map: Map<String, Value>) {
        this.valueMap.putAll(map)
    }

    /**
     * {@inheritDoc}
     */
    override fun hasValue(path: String): Boolean {
        return this.valueMap.containsKey(path)
    }

    /**
     * {@inheritDoc}
     */
    override fun getNames(): NameList {
        return NameList(this.valueMap.keys)
    }

    /**
     * {@inheritDoc}
     */
    @Throws(NameNotFoundException::class)
    override fun optValue(path: String): Optional<Value> {
        return Optional.ofNullable(valueMap[path])
    }

    /**
     * {@inheritDoc}
     */
    override fun toString(): String {
        val res = StringBuilder("[")
        var flag = true
        for (name in this.names) {
            if (flag) {
                flag = false
            } else {
                res.append(", ")
            }
            res.append(name).append(":").append(getValue(name).string)
        }
        return res.toString() + "]"
    }

    fun builder(): Builder {
        return Builder(LinkedHashMap(valueMap))
    }

    override fun toMeta(): Meta {
        val builder = MetaBuilder("point")
        for (name in namesAsArray()) {
            builder.putValue(name, getValue(name))
        }
        return builder.build()
    }

    //    @Override
    //    public Map<String, Value> asMap() {
    //        return Collections.unmodifiableMap(this.valueMap);
    //    }

    class Builder : GenericBuilder<ValueMap, Builder> {

        private val valueMap = LinkedHashMap<String, Value>()

        constructor(dp: Values) {
            for (name in dp.names) {
                valueMap[name] = dp.getValue(name)
            }

        }

        constructor(map: Map<String, Value>) {
            valueMap.putAll(map)
        }

        constructor() {

        }

        /**
         * if value exists it is replaced
         *
         * @param name  a [java.lang.String] object.
         * @param value a [hep.dataforge.values.Value] object.
         * @return a [ValueMap] object.
         */
        fun putValue(name: String, value: Value = Value.NULL): Builder {
            valueMap[name] = value
            return this
        }

        fun putValue(name: String, value: Any): Builder {
            valueMap[name] = Value.of(value)
            return this
        }

        infix fun String.to(value: Any?) {
            if (value == null) {
                valueMap.remove(this)
            } else {
                putValue(this, value)
            }
        }

        /**
         * Put the value at the beginning of the map
         *
         * @param name
         * @param value
         * @return
         */
        fun putFirstValue(name: String, value: Any): Builder {
            synchronized(valueMap) {
                val newMap = LinkedHashMap<String, Value>()
                newMap[name] = Value.of(value)
                newMap.putAll(valueMap)
                valueMap.clear()
                valueMap.putAll(newMap)
                return this
            }
        }

        fun addTag(tag: String): Builder {
            return putValue(tag, true)
        }

        override fun build(): ValueMap {
            return ValueMap(valueMap)
        }

        override fun self(): Builder {
            return this
        }
    }

    companion object {

        @JvmStatic
        fun ofMap(map: Map<String, Any>): ValueMap {
            return ValueMap(map.mapValues { Value.of(it.value) })
        }

        @SafeVarargs
        fun ofPairs(vararg pairs: Pair<String, Any>): ValueMap {
            val builder = Builder()
            for ((first, second) in pairs) {
                builder.putValue(first, second)
            }
            return builder.build()
        }

        @JvmStatic
        fun of(values: Iterable<NamedValue>): ValueMap {
            return ValueMap(values.associateBy(keySelector = { it.name }, valueTransform = { it.anonymous }))
        }

        @JvmStatic
        fun of(vararg values: NamedValue): ValueMap {
            return ValueMap(values.associateBy(keySelector = { it.name }, valueTransform = { it.anonymous }))
        }

        @JvmStatic
        fun of(list: Array<String>, vararg values: Any): ValueMap {
            if (list.size != values.size) {
                throw IllegalArgumentException()
            }
            val valueMap = LinkedHashMap<String, Value>()
            for (i in values.indices) {
                val `val` = Value.of(values[i])
                valueMap[list[i]] = `val`
            }
            return ValueMap(valueMap)
        }
    }
}