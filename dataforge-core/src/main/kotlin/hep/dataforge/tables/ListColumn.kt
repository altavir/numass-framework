/*
 * Copyright 2015 Alexander Nozik.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package hep.dataforge.tables

import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder
import hep.dataforge.meta.MetaMorph
import hep.dataforge.values.Value
import hep.dataforge.values.ValueFactory
import java.util.*
import java.util.stream.Stream
import kotlin.streams.toList

/**
 * A simple immutable Column implementation using list of values
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
class ListColumn : Column, MetaMorph {

    override lateinit var format: ColumnFormat
    private var values: List<Value>? = null

    constructor() {}

    internal constructor(meta: Meta) {
        this.format = MetaMorph.morph(ColumnFormat::class.java, meta.getMeta("format"))
        this.values = meta.getValue("data").list
    }

    constructor(format: ColumnFormat, values: Stream<Value>) {
        this.format = format
        this.values = values.toList()
        if (!this.values!!.stream().allMatch{ format.isAllowed(it) }) {
            throw IllegalArgumentException("Not allowed value in the column")
        }
    }

    override fun asList(): List<Value> {
        return Collections.unmodifiableList(values!!)
    }

    /**
     * {@inheritDoc}
     */
    override fun get(n: Int): Value {
        return values!![n]
    }

    override fun stream(): Stream<Value> {
        return values!!.stream()
    }

    override fun iterator(): Iterator<Value> {
        return values!!.iterator()
    }

    override fun size(): Int {
        return values!!.size
    }

    override fun toMeta(): Meta {
        return MetaBuilder("column")
                .putNode("format", format.toMeta())
                .putValue("data", values)
    }

    companion object {

        /**
         * Create a copy of given column if it is not ListColumn.
         *
         * @param column
         * @return
         */
        fun copy(column: Column): ListColumn {
            return column as? ListColumn ?: ListColumn(column.format, column.stream())
        }

        /**
         * Create a copy of given column renaming it in process
         *
         * @param name
         * @param column
         * @return
         */
        fun copy(name: String, column: Column): ListColumn {
            return if (name == column.name) {
                copy(column)
            } else {
                ListColumn(ColumnFormat.rename(name, column.format), column.stream())
            }
        }

        fun build(format: ColumnFormat, values: Stream<*>): ListColumn {
            return ListColumn(format, values.map{ ValueFactory.of(it) })
        }
    }
}
