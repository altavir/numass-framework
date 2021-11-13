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

package hep.dataforge.tables

import hep.dataforge.exceptions.NamingException
import hep.dataforge.nullable
import hep.dataforge.values.*
import java.util.function.Predicate
import java.util.stream.Stream


object Tables {

    @JvmStatic
    fun sort(table: Table, comparator: java.util.Comparator<Values>): Table {
        return ListTable(table.format, table.rows.sorted(comparator).toList())
    }

    @JvmStatic
    fun sort(table: Table, name: String, ascending: Boolean): Table {
        return sort(
            table,
            Comparator { o1: Values, o2: Values ->
                val signum = if (ascending) +1 else -1
                o1.getValue(name).compareTo(o2.getValue(name)) * signum
            }
        )
    }

    /**
     * Фильтрует набор данных и оставляет только те точки, что удовлетовряют
     * условиям
     *
     * @param condition a [java.util.function.Predicate] object.
     * @return a [hep.dataforge.tables.Table] object.
     * @throws hep.dataforge.exceptions.NamingException if any.
     */
    @Throws(NamingException::class)
    @JvmStatic
    fun filter(table: Table, condition: Predicate<Values>): Table {
        return ListTable(table.format, table.rows.filter(condition).toList())
    }

    /**
     * Быстрый фильтр для значений одного поля
     *
     * @param valueName
     * @param a
     * @param b
     * @return
     * @throws hep.dataforge.exceptions.NamingException
     */
    @Throws(NamingException::class)
    @JvmStatic
    fun filter(table: Table, valueName: String, a: Value, b: Value): Table {
        return filter(table, Filtering.getValueCondition(valueName, a, b))
    }

    @Throws(NamingException::class)
    @JvmStatic
    fun filter(table: Table, valueName: String, a: Number, b: Number): Table {
        return filter(table, Filtering.getValueCondition(valueName, ValueFactory.of(a), ValueFactory.of(b)))
    }

    /**
     * Быстрый фильтр по меткам
     *
     * @param tags
     * @return a [hep.dataforge.tables.Column] object.
     * @throws hep.dataforge.exceptions.NamingException
     * @throws hep.dataforge.exceptions.NameNotFoundException if any.
     */
    @Throws(NamingException::class)
    @JvmStatic
    fun filter(table: Table, vararg tags: String): Table {
        return filter(table, Filtering.getTagCondition(*tags))
    }


    /**
     * Create a ListTable for list of rows and infer format from the first row
     */
    @JvmStatic
    fun infer(points: List<Values>): ListTable {
        if (points.isEmpty()) {
            throw IllegalArgumentException("Can't create ListTable from the empty list. Format required.")
        }
        return ListTable(MetaTableFormat.forValues(points[0]), points)
    }
}

/**
 *  Extension methods for tables
 */

/* Column operations */

/**
 * Return a new table with additional column.
 * Warning: if initial table is not a column table, then the whole amount of data will be copied, which could be ineffective for large tables
 */
operator fun Table.plus(column: Column): Table {
    return ColumnTable.copy(this).addColumn(column)
}

/**
 *  Warning: if initial table is not a column table, then the whole amount of data will be copied, which could be ineffective for large tables
 */
fun Table.addColumn(name: String, type: ValueType, data: Stream<*>, vararg tags: String): Table {
    return ColumnTable.copy(this).addColumn(name, type, data, *tags)
}

/**
 *  Warning: if initial table is not a column table, then the whole amount of data will be copied, which could be ineffective for large tables
 */
fun Table.addColumn(format: ColumnFormat, transform: Values.() -> Any): Table {
    return ColumnTable.copy(this).buildColumn(format, transform)
}

fun Table.addColumn(name: String, type: ValueType, transform: Values.() -> Any): Table =
    addColumn(ColumnFormat.build(name, type), transform)

fun Table.replaceColumn(name: String, transform: Values.() -> Any): Table {
    return ColumnTable.copy(this).replaceColumn(name, transform)
}

/* Row filtering and sorting */

fun Table.filter(condition: (Values) -> Boolean): Table {
    return ListTable(format, rows.filter(condition).toList())
}

fun Table.sort(comparator: Comparator<Values>): Table {
    return ListTable(format, rows.sorted(comparator).toList())
}

fun Table.sort(name: String = format.first().name, ascending: Boolean = true): Table {
    return sort { o1: Values, o2: Values ->
        val signum = if (ascending) +1 else -1
        o1.getValue(name).compareTo(o2.getValue(name)) * signum
    }
}


/* Row reduction */

fun <K> Table.reduceRows(format: TableFormat? = null, keySelector: (Values) -> K, mapper: (K, List<Values>) -> Values) =
    ListTable(format ?: this.format, this.groupBy(keySelector).map { (key, value) -> mapper(key, value) }, false)

/**
 * A helper for table row reduction
 * @param default the default reduction performed for columns that are not explicitly mentioned
 */
class RowReducer(val default: (Iterable<Value>) -> Value) {
    private val reducers = HashMap<String, (Iterable<Value>) -> Value>()

    /**
     * Add custom rule
     */
    fun rule(key: String, reducer: (Iterable<Value>) -> Value) {
        reducers[key] = reducer
    }

    fun sumByDouble(key: String) = rule(key) { rows -> rows.sumOf { it.double }.asValue() }
    fun sumByInt(key: String) = rule(key) { rows -> rows.sumOf { it.int }.asValue() }

    fun averageByDouble(key: String) = rule(key) { rows -> rows.map { it.double }.average().asValue() }
    fun averageByInt(key: String) = rule(key) { rows -> rows.map { it.int }.average().asValue() }

    fun reduce(key: String, values: Iterable<Value>): Value = reducers.getOrDefault(key, default).invoke(values)

    /**
     * Reduce list of rows to a single row
     */
    fun reduce(keys: Iterable<String>, rows: Iterable<Values>): Values {
        val map = keys.associate { key ->
            key to rows.map { it.optValue(key).nullable ?: Value.NULL }
        }.mapValues { reduce(it.key, it.value) }
        return ValueMap.ofMap(map)
    }
}

/**
 * Rows are grouped by specific column value and step and then reduced by group.
 * By default, uses averaging operation for key column and sum for others, but could be customized by [customizer].
 * Missing values are treated as zeroes
 */
fun Table.sumByStep(key: String, step: Double, customizer: (RowReducer) -> Unit = {}): Table {
    assert(step > 0) { "Step must be positive" }

    val reducer = RowReducer { rows -> rows.sumOf { it.double }.asValue() }.apply {
        averageByDouble(key)
    }.apply(customizer)

    val rows = this.groupBy {
        kotlin.math.ceil((it.optValue(key).nullable?.double ?: 0.0) / step)
    }.map { (_, value) ->
        reducer.reduce(format.names, value)
    }
    return ListTable(format, rows, true)
}
