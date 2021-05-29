package hep.dataforge.tables

import hep.dataforge.exceptions.NameNotFoundException
import hep.dataforge.meta.MetaMorph
import hep.dataforge.meta.MorphTarget
import hep.dataforge.values.*
import java.util.*
import java.util.stream.IntStream
import java.util.stream.Stream
import kotlin.streams.toList

/**
 * A column based table. Column access is fast, but row access is slow. Best memory efficiency.
 * Column table is immutable all operations create new tables.
 * Created by darksnake on 12.07.2017.
 */
@MorphTarget(target = ListTable::class)
class ColumnTable : Table {

    private val map = LinkedHashMap<String, Column>()

    override val columns: Collection<Column>
        get() = map.values

    private val size: Int

    override val format: TableFormat
        get() =  TableFormat { columns.stream().map { it.format }}

    override val rows: Stream<Values>
        get() = IntStream.range(0, size).mapToObj{ this.getRow(it) }

    /**
     * Build a table from pre-constructed columns
     *
     * @param columns
     */
    constructor(columns: Collection<Column>) {
        columns.forEach { it -> map[it.name] = ListColumn.copy(it) }
        if (map.values.stream().mapToInt{ it.size() }.distinct().count() != 1L) {
            throw IllegalArgumentException("Column dimension mismatch")
        }
        size = map.values.stream().findFirst().map { it.size() }.orElse(0)
    }

    /**
     * Create empty column table
     */
    constructor() {
        size = 0
    }

    override fun getRow(i: Int): Values {
        return ValueMap(columns.associate { it.name to get(it.name,i) })
    }

    override fun size(): Int {
        return size
    }

    override fun getColumn(name: String): Column {
        return map[name]?: error("Column with name $name not found")
    }

    override fun get(columnName: String, rowNumber: Int): Value {
        return getColumn(columnName).get(rowNumber)
    }

    override fun iterator(): Iterator<Values> {
        return rows.iterator()
    }


    /**
     * Add or replace column
     *
     * @param column
     * @return
     */
    fun addColumn(column: Column): ColumnTable {
        val map = LinkedHashMap(map)
        map[column.name] = column
        return ColumnTable(map.values)
    }

    /**
     * Add a new column built from object stream
     *
     * @param name
     * @param type
     * @param data
     * @param tags
     * @return
     */
    fun addColumn(name: String, type: ValueType, data: Stream<*>, vararg tags: String): ColumnTable {
        val format = ColumnFormat.build(name, type, *tags)
        val column = ListColumn(format, data.map { ValueFactory.of(it) })
        return addColumn(column)
    }

    /**
     * Create a new table with values derived from appropriate rows. The operation does not consume a lot of memory
     * and time since existing columns are immutable and are reused.
     *
     *
     * If column with given name exists, it is replaced.
     *
     * @param format
     * @param transform
     * @return
     */
    fun buildColumn(format: ColumnFormat, transform: Values.() -> Any): ColumnTable {
        val list = ArrayList(columns)
        val newColumn = ListColumn.build(format, rows.map(transform))
        list.add(newColumn)
        return ColumnTable(list)
    }

    fun buildColumn(name: String, type: ValueType, transform: Values.() -> Any): ColumnTable {
        val format = ColumnFormat.build(name, type)
        return buildColumn(format, transform)
    }

    /**
     * Replace existing column with new values (without changing format)
     *
     * @param columnName
     * @param transform
     * @return
     */
    fun replaceColumn(columnName: String, transform: (Values)->Any): ColumnTable {
        if (!map.containsKey(columnName)) {
            throw NameNotFoundException(columnName)
        }
        val newColumn = ListColumn.build(getColumn(columnName).format, rows.map(transform))
        map[columnName] = newColumn
        return ColumnTable(map.values)
    }

    /**
     * Return a new Table with given columns being removed
     *
     * @param columnName
     * @return
     */
    fun removeColumn(vararg columnName: String): ColumnTable {
        val map = LinkedHashMap(map)
        for (c in columnName) {
            map.remove(c)
        }
        return ColumnTable(map.values)
    }

    override fun equals(other: Any?): Boolean {
        return other != null && javaClass == other.javaClass && (other as MetaMorph).toMeta() == this.toMeta()
    }

    companion object {


        fun copy(table: Table): ColumnTable {
            return ColumnTable(table.columns)
        }

        /**
         * Create instance of column table using given columns with appropriate names
         *
         * @param columns
         * @return
         */
        fun of(columns: Map<String, Column>): ColumnTable {
            return ColumnTable(
                    columns.entries
                            .stream()
                            .map { entry -> ListColumn.copy(entry.key, entry.value) }
                            .toList()
            )
        }
    }
}


fun Table.asColumnTable(): ColumnTable = ColumnTable.copy(this)