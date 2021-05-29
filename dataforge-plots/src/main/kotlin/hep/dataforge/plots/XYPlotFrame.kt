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
package hep.dataforge.plots

import hep.dataforge.description.*
import hep.dataforge.meta.Meta
import hep.dataforge.meta.configNode
import hep.dataforge.meta.morphConfigNode
import hep.dataforge.names.Name
import hep.dataforge.tables.Adapters.X_AXIS
import hep.dataforge.tables.Adapters.Y_AXIS
import hep.dataforge.values.Value
import hep.dataforge.values.ValueType
import java.util.*

/**
 * Two-axis plot frame
 *
 * @author Alexander Nozik
 */
@NodeDefs(
        NodeDef(key = "xAxis", info = "The description of X axis", type = Axis::class),
        NodeDef(key = "yAxis", info = "The description of Y axis", type = Axis::class),
        NodeDef(key = "legend", info = "The configuration for plot legend", descriptor = "method::hep.dataforge.plots.XYPlotFrame.updateLegend")
)
abstract class XYPlotFrame : AbstractPlotFrame() {

    @Description("The description of X axis")
    var xAxis: Axis by morphConfigNode(def = Axis(Meta.empty()))

    @Description("The description of Y axis")
    var yAxis: Axis by morphConfigNode(def = Axis(Meta.empty()))

    private val legend by configNode()


    override fun applyConfig(config: Meta?) {
        if (config == null) {
            return
        }

        updateFrame(config)
        //Вызываем эти методы, чтобы не делать двойного обновления аннотаций
        updateAxis(X_AXIS, xAxis.meta, getConfig())

        updateAxis(Y_AXIS, yAxis.meta, getConfig())

        updateLegend(legend)

    }

    protected abstract fun updateFrame(annotation: Meta)

    /**
     * Configure axis
     *
     * @param axisName
     * @param axisMeta
     */
    @ValueDefs(
            ValueDef(key = "type", allowed = ["number", "log", "time"], def = "number", info = "The type of axis. By default number axis is used"),
            ValueDef(key = "title", info = "The title of the axis."),
            ValueDef(key = "units", def = "", info = "The units of the axis."),
            ValueDef(key = "range.from", type = [ValueType.NUMBER], info = "Lower boundary for fixed range"),
            ValueDef(key = "range.to", type = [ValueType.NUMBER], info = "Upper boundary for fixed range"),
            ValueDef(key = "crosshair", def = "none", allowed = ["none", "free", "data"], info = "Appearance and type of the crosshair")
    )
    @NodeDef(key = "range", info = "The definition of range for given axis")
    protected abstract fun updateAxis(axisName: String, axisMeta: Meta, plotMeta: Meta)

    @ValueDef(key = "show", type = [ValueType.BOOLEAN], def = "true", info = "Display or hide the legend")
    protected abstract fun updateLegend(legendMeta: Meta)

    /**
     * Get actual color value for displayed plot. Some color could be assigned even if it is missing from configuration
     *
     * @param name
     * @return
     */
    open fun getActualColor(name: Name): Optional<Value> {
        return plots[name]?.config?.optValue("color") ?: Optional.empty()
    }
}
