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

import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder
import hep.dataforge.plots.data.DataPlot
import hep.dataforge.tables.Adapters
import hep.dataforge.tables.ListTable
import hep.dataforge.tables.Table

/**
 * @author darksnake
 */
object PlotUtils {

    fun getThickness(reader: Meta): Double {
        return reader.getDouble("thickness", -1.0)
    }

    /**
     * Строка для отображениея в легенде
     *
     * @return a [java.lang.String] object.
     */
    fun getTitle(reader: Meta): String {
        return reader.getString("title", "")
    }

    //TODO change arguments order, introduce defaults
    fun setXAxis(frame: PlotFrame, title: String, units: String, type: String) {
        val builder = MetaBuilder("xAxis")
                .setValue("title", title)
                .setValue("units", units)
                .setValue("type", type)
        frame.config.setNode(builder)
    }

    fun setYAxis(frame: PlotFrame, title: String, units: String, type: String) {
        val builder = MetaBuilder("yAxis")
                .setValue("title", title)
                .setValue("units", units)
                .setValue("type", type)
        frame.config.setNode(builder)
    }

    fun setTitle(frame: PlotFrame, title: String) {
        frame.configureValue("title", title)
    }

    fun extractData(plot: DataPlot, query: Meta): Table {
        return ListTable(Adapters.getFormat(plot.adapter, Adapters.X_VALUE_KEY, Adapters.Y_VALUE_KEY), plot.getData(query))

    }
}
