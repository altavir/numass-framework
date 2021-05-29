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
package hep.dataforge.plots.data

import hep.dataforge.description.ValueDef
import hep.dataforge.description.ValueDefs
import hep.dataforge.meta.KMetaBuilder
import hep.dataforge.meta.Meta
import hep.dataforge.tables.Adapters
import hep.dataforge.tables.Adapters.*
import hep.dataforge.tables.ValuesAdapter
import hep.dataforge.values.ValueMap
import hep.dataforge.values.ValueType
import hep.dataforge.values.Values
import java.util.*
import java.util.stream.Stream

/**
 * @author Alexander Nozik
 */
@ValueDefs(
        ValueDef(key = "showLine", type = [ValueType.BOOLEAN], def = "false", info = "Show the connecting line."),
        ValueDef(key = "showSymbol", type = [ValueType.BOOLEAN], def = "true", info = "Show symbols for data point."),
        ValueDef(key = "showErrors", type = [ValueType.BOOLEAN], def = "true", info = "Show errors for points.")
)
class DataPlot(name: String, meta: Meta = Meta.empty(), adapter: ValuesAdapter? = null, data: Iterable<Values> = emptyList()) : XYPlot(name, meta, adapter) {

    /**
     *
     * Setter is unsafe
     */
    override var data: MutableList<Values> = ArrayList()

    init {
        this.data.addAll(data)
    }

    @JvmOverloads
    fun fillData(it: Iterable<Values>, append: Boolean = false): DataPlot {
        if (!append) {
            this.data = ArrayList()
        }
        for (dp in it) {
            data.add(dp)
        }
        notifyDataChanged()
        return this
    }

    fun fillData(it: Stream<out Values>): DataPlot {
        this.data = ArrayList()
        it.forEach { dp -> data.add(dp) }
        notifyDataChanged()
        return this
    }

    fun append(dp: Values): DataPlot {
        data.add(dp)
        notifyDataChanged()
        return this
    }

    fun append(x: Number, y: Number): DataPlot {
        return append(Adapters.buildXYDataPoint(adapter, x.toDouble(), y.toDouble()))
    }

    override fun getRawData(query: Meta): List<Values> {
        return data
    }

    fun clear() {
        data.clear()
        notifyDataChanged()
    }

    companion object {
        @JvmOverloads
        @JvmStatic
        fun plot(name: String, x: DoubleArray, y: DoubleArray, xErrs: DoubleArray? = null, yErrs: DoubleArray? = null): DataPlot {
            val adapter = if (yErrs == null) {
                Adapters.DEFAULT_XY_ADAPTER
            } else {
                Adapters.DEFAULT_XYERR_ADAPTER
            }
            if (x.size != y.size) {
                throw IllegalArgumentException("Arrays size mismatch")
            }

            val data = ArrayList<Values>()
            for (i in y.indices) {
                val point = ValueMap.of(arrayOf(X_AXIS, Y_AXIS), x[i], y[i]).builder()

                if (xErrs != null) {
                    point.putValue(X_ERROR_KEY, xErrs[i])
                }

                if (yErrs != null) {
                    point.putValue(Y_ERROR_KEY, yErrs[i])
                }

                data.add(point.build())
            }
            return DataPlot(name, Meta.empty(), adapter, data)
        }

        @JvmStatic
        @JvmOverloads
        fun plot(name: String, data: Iterable<Values> = emptyList(), adapter: ValuesAdapter? = null, metaBuilder: KMetaBuilder.() -> Unit = {}): DataPlot {
            val meta = KMetaBuilder("plot").apply(metaBuilder)
            return DataPlot(name, meta, adapter, data)
        }
    }

}