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

import hep.dataforge.exceptions.NameNotFoundException
import hep.dataforge.exceptions.NamingException
import hep.dataforge.meta.Meta
import hep.dataforge.values.*
import java.util.stream.Stream
import java.util.stream.StreamSupport
import kotlin.streams.toList

/**
 * An immutable row-based Table based on ArrayList. Row access is fast, but
 * column access could be complicated
 *
 * @param format Формат описывает набор полей, которые ОБЯЗАТЕЛЬНО присутствуют в каждой
 * точке из набора данных. Набор полей каждой точки может быть шире, но не
 * уже.
 *
 * @param unsafe if `true`, skip format compatibility on the init.
 * @author Alexander Nozik
 */
class ListTable @JvmOverloads constructor(override val format: TableFormat, points: List<Values>, unsafe: Boolean = false) : ListOfPoints(points), Table {


    constructor(meta: Meta) : this(
            MetaTableFormat(meta.getMeta("format")),
            ListOfPoints.buildFromMeta(meta.getMeta("data"))
    )

    init {
        if (!unsafe) {
            points.forEach {
                if (!it.names.contains(format.names)) {
                    throw NamingException("Row $it does not contain all off the fields declared in ${format.names}")
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param name
     * @return
     */
    @Throws(NameNotFoundException::class)
    override fun getColumn(name: String): Column {
        if (!this.format.names.contains(name)) {
            throw NameNotFoundException(name)
        }
        return object : Column {
            override val format: ColumnFormat = this@ListTable.format.getColumn(name)

            override fun get(n: Int): Value {
                return asList()[n]
            }

            override fun asList(): List<Value> {
                return StreamSupport.stream(this.spliterator(), false).toList()
            }

            override fun stream(): Stream<Value> {
                return this@ListTable.rows.map { point -> point.getValue(name) }
            }

            override fun iterator(): MutableIterator<Value> {
                return stream().iterator()
            }

            override fun size(): Int {
                return this@ListTable.size()
            }
        }
    }

    override val columns: Collection<Column> by lazy {
        format.names.map { getColumn(it) }
    }

    override fun get(name: String, index: Int): Value {
        return getRow(index).getValue(name)
    }

    override fun toMeta(): Meta {
        return super<ListOfPoints>.toMeta()
    }

    class Builder(private var _format: TableFormat? = null) {

        private val points: MutableList<Values> = ArrayList()

        var format: TableFormat
            get() = _format ?: throw RuntimeException("Format not defined")
            set(value) {
                _format = value
            }

        constructor(format: Iterable<String>) : this(MetaTableFormat.forNames(format))

        constructor(vararg format: String) : this(MetaTableFormat.forNames(*format))

        /**
         * Если formatter == null, то могут быть любые точки
         *
         * @param e
         * @throws hep.dataforge.exceptions.NamingException if any.
         */
        fun row(e: Values): Builder {
            if (_format == null) {
                _format = MetaTableFormat.forValues(e)
            }
            points.add(e)
            return this
        }

        /**
         * Add new point constructed from a list of objects using current table format
         *
         * @param values
         * @return
         * @throws NamingException
         */
        @Throws(NamingException::class)
        fun row(vararg values: Any): Builder = row(ValueMap.of(format.namesAsArray(), *values))

        fun row(values: ValueProvider): Builder {
            val names = format.namesAsArray()
            val map = names.associateBy({ it }) { values.getValue(it) }
            return row(ValueMap(map))
        }

        fun row(vararg values: NamedValue): Builder = row(ValueMap.of(*values))

        fun row(vararg values: Pair<String, Any>): Builder =
            row(ValueMap.of(values.map { NamedValue.of(it.first, it.second) }))

        fun row(map: Map<String, Any>): Builder = row(ValueMap.ofMap(map))

        fun rows(points: Iterable<Values>): Builder {
            for (point in points) {
                row(point)
            }
            return this
        }

        fun rows(stream: Stream<out Values>): Builder {
            stream.forEach { this.row(it) }
            return this
        }

        fun build(): ListTable = ListTable(format, points)

        /**
         * Build table without points name check
         */
        fun buildUnsafe(): Table = ListTable(format, points, true)
    }

    companion object {

        fun copy(table: Table): ListTable {
            return table as? ListTable ?: ListTable(table.format, table.rows.toList())
        }
    }

}

fun buildTable(format: TableFormat? = null, builder: ListTable.Builder.() -> Unit): Table {
    return ListTable.Builder(format).apply(builder).build()
}
