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

import hep.dataforge.description.NodeDef
import hep.dataforge.description.ValueDef
import hep.dataforge.description.ValueDefs
import hep.dataforge.meta.Configuration.FINAL_TAG
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder
import hep.dataforge.tables.Adapters
import hep.dataforge.tables.ValuesAdapter
import hep.dataforge.tables.ValuesAdapter.ADAPTER_KEY
import hep.dataforge.values.Value
import hep.dataforge.values.ValueType
import hep.dataforge.values.Values
import java.util.stream.Stream

/**
 * Plot with x and y axis. It is possible to have multiple y axis
 *
 * @author Alexander Nozik
 */
//@ValueDef(name = "symbolType", info = "The type of the symbols for scatterplot.")
//@ValueDef(name = "symbolSize", type = "NUMBER", info = "The size of the symbols for scatterplot.")
//@ValueDef(name = "lineType", info = "The type of the line fill.")
@ValueDefs(
    ValueDef(key = "color", info = "The color of line or symbol.", tags = ["widget:color"]),
    ValueDef(key = "thickness", type = [ValueType.NUMBER], def = "1", info = "Thickness of the line if it is present"),
    ValueDef(key = "connectionType",
        def = "DEFAULT",
        enumeration = XYPlot.ConnectionType::class,
        info = "Connection line type")
)
@NodeDef(key = ADAPTER_KEY, info = "An adapter to interpret the dataset", tags = [FINAL_TAG])
abstract class XYPlot(name: String, meta: Meta, adapter: ValuesAdapter?) : AbstractPlot(name, meta, adapter) {

    enum class ConnectionType {
        DEFAULT,
        STEP,
        SPLINE
    }

    fun getData(from: Value, to: Value): List<Values> {
        return getData(MetaBuilder("").putValue("xRange.from", from).putValue("xRange.to", to))
    }

    fun getData(from: Value, to: Value, numPoints: Int): List<Values> {
        return getData(MetaBuilder("").putValue("xRange.from", from).putValue("xRange.to", to)
            .putValue("numPoints", numPoints))
    }

    /**
     * Apply range filters to data
     *
     * @param query
     * @return
     */
    @NodeDef(key = "xRange", info = "X filter")
    override fun getData(query: Meta): List<Values> {
        return if (query.isEmpty) {
            getRawData(query)
        } else {
            filterDataStream(getRawData(query).stream(), query).toList()
        }
    }

    protected abstract fun getRawData(query: Meta): List<Values>

    protected fun filterXRange(data: Stream<Values>, xRange: Meta): Stream<Values> {
        val from = xRange.getValue("from", Value.NULL)
        val to = xRange.getValue("to", Value.NULL)
        return if (from !== Value.NULL && to !== Value.NULL) {
            data.filter { point -> Adapters.getXValue(adapter, point) in from..to }
        } else if (from === Value.NULL && to !== Value.NULL) {
            data.filter { point -> Adapters.getXValue(adapter, point) < to }
        } else if (to === Value.NULL) {
            data.filter { point -> Adapters.getXValue(adapter, point) > from }
        } else {
            data
        }
    }

    protected fun filterDataStream(data: Stream<Values>, cfg: Meta): Stream<Values> {
        return if (cfg.hasMeta("xRange")) {
            filterXRange(data, cfg.getMeta("xRange"))
        } else {
            data
        }
    }

}
