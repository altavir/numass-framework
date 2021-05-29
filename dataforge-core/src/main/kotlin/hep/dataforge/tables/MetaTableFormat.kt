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

import hep.dataforge.description.NodeDef
import hep.dataforge.description.NodeDefs
import hep.dataforge.exceptions.NameNotFoundException
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder
import hep.dataforge.meta.MetaNode.DEFAULT_META_NAME
import hep.dataforge.meta.MetaUtils
import hep.dataforge.meta.SimpleMetaMorph
import hep.dataforge.names.NameList
import hep.dataforge.values.Values
import java.util.stream.Stream

/**
 * A class for point set visualization
 *
 * @author Alexander Nozik
 */
@NodeDefs(
        NodeDef(key = "column", multiple = true, required = true, info = "A column format", descriptor = "class::hep.dataforge.tables.ColumnFormat"),
        NodeDef(key = "defaultColumn", info = "Default column format. Used when format for specific column is not given"),
        NodeDef(key = DEFAULT_META_NAME, info = "Custom table information")
)
class MetaTableFormat(meta: Meta) : SimpleMetaMorph(meta), TableFormat {
    //TODO add transformation to use short column description

    val isEmpty: Boolean
        get() = !meta.hasMeta("column")

    private  val _names: NameList by lazy {
        NameList(columns.map<String> { it.name })
    }

    override fun getNames(): NameList {
        return _names
    }

    private fun getColumnMeta(column: String): Meta {
        return MetaUtils.findNodeByValue(meta, "column", "name", column).orElseThrow { NameNotFoundException(column) }
    }

    override fun getColumn(column: String): ColumnFormat {
        return ColumnFormat(getColumnMeta(column))
    }

    override fun getColumns(): Stream<ColumnFormat> {
        return meta.getMetaList("column").stream().map { ColumnFormat(it) }
    }


    override fun iterator(): MutableIterator<ColumnFormat> {
        return columns.iterator()
    }

    override fun toMeta(): Meta {
        return super<SimpleMetaMorph>.toMeta()
    }

    companion object {

        /**
         * An empty format holding information only about the names of columns
         *
         * @param names
         * @return
         */
        fun forNames(vararg names: String): TableFormat {
            val builder = MetaBuilder("format")
            for (n in names) {
                builder.putNode(MetaBuilder("column").setValue("name", n))
            }
            return MetaTableFormat(builder.build())
        }


        fun forNames(names: Iterable<String>): TableFormat {
            return forNames(*names.toList().toTypedArray())
        }

        /**
         * Build a table format using given data point as reference
         *
         * @param dataPoint
         * @return
         */
        fun forValues(dataPoint: Values): TableFormat {
            val builder = MetaBuilder("format")
            for (n in dataPoint.names) {
                builder.putNode(MetaBuilder("column").setValue("name", n).setValue("type", dataPoint.getValue(n).type.name))
            }
            return MetaTableFormat(builder.build())
        }
    }

}
