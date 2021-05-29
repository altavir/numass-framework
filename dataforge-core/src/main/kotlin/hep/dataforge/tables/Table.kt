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

import hep.dataforge.Type
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder
import hep.dataforge.meta.MetaMorph
import hep.dataforge.tables.Table.Companion.TABLE_TYPE


/**
 * An immutable table of values
 *
 * @author Alexander Nozik
 */
@Type(TABLE_TYPE)
interface Table : NavigableValuesSource, MetaMorph {

    /**
     * Get columns as a stream
     *
     * @return
     */
    val columns: Collection<Column>

    /**
     * A minimal set of fields to be displayed in this table. Could return empty format if source is unformatted
     *
     * @return
     */
    val format: TableFormat

    /**
     * Get an immutable column from this table
     *
     * @param name
     * @return
     */
    fun getColumn(name: String): Column

    override fun toMeta(): Meta {
        val res = MetaBuilder("table")
        res.putNode("format", format.toMeta())
        val dataNode = MetaBuilder("data")
        forEach { dp -> dataNode.putNode("point", dp.toMeta()) }
        res.putNode(dataNode)
        return res
    }

    companion object {
        const val TABLE_TYPE = "hep.dataforge.table"
    }
}
