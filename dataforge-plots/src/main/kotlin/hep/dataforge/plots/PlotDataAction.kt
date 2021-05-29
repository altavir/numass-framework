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

import hep.dataforge.actions.OneToOneAction
import hep.dataforge.context.Context
import hep.dataforge.description.NodeDef
import hep.dataforge.description.NodeDefs
import hep.dataforge.description.TypedActionDef
import hep.dataforge.meta.Laminate
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder
import hep.dataforge.plots.data.DataPlot
import hep.dataforge.plots.output.plot
import hep.dataforge.tables.Adapters
import hep.dataforge.tables.Table
import java.util.*

/**
 * Аннотация действия может содержать несколько различных описаний рамки. При
 * выполнинии каждый набор данных помещается в ту рамку, имя которой указано в
 * параметре plot_frame_name. Если параметр не указан, используется рамка
 * по-умолчанию
 *
 * @author Alexander Nozik
 */
@TypedActionDef(name = "plotData", info = "Scatter plot of given DataSet", inputType = Table::class, outputType = Table::class)
@NodeDefs(
        NodeDef(key = "plotFrame", multiple = true, info = "The description of plot frame", descriptor = "class::hep.dataforge.plots.XYPlotFrame"),
        NodeDef(key = "adapter", info = "Adapter for data")
)
//@NodeDef(name = "snapshot", info = "Save plot shapshots to file",
//        target = "method::hep.dataforge.plots.PlotDataAction.snapshot")
//@NodeDef(name = "serialize", info = "Serialize plot to file",
//        target = "method::hep.dataforge.plots.PlotDataAction.serialize")
//@ValueDef(name = "snapshot", type = "BOOLEAN", def = "false",
//        info = "Save plot shapshots to file with default parameters")
//@ValueDef(name = "serialize", type = "BOOLEAN", def = "false",
//        info = "Serialize plot to file with default parameters")
class PlotDataAction : OneToOneAction<Table, Table>("plotData", Table::class.java, Table::class.java) {

    private val snapshotTasks = HashMap<String, Runnable>()
    private val serializeTasks = HashMap<String, Runnable>()

    private fun findFrameDescription(meta: Meta, name: String): Meta {
        //TODO сделать тут возможность подстановки стилей?
        val frameDescriptions = meta.getMetaList("plotFrame")
        var defaultDescription = MetaBuilder("plotFrame").build()
        for (an in frameDescriptions) {
            val frameName = meta.getString("frameName")
            if ("default" == frameName) {
                defaultDescription = an
            }
            if (frameName == name) {
                return an
            }
        }

        return defaultDescription.builder.putValue("title", meta.getString("plotTitle", "")).build()
    }

    override fun execute(context: Context, name: String, input: Table, meta: Laminate): Table {
        val groupBy = meta.getString("groupBy")
        val frameName = meta.getString(groupBy, "default")

        val adapter = Adapters.buildAdapter(meta.getMeta("adapter", Meta.empty()))
        val plottableData = DataPlot.plot(name, input, adapter)
        plottableData.configure(meta)

        context.plot(plottableData, frameName, this.name)

        //        if (meta.hasMeta("snapshot")) {
        //            snapshot(name, frame, meta.getMeta("snapshot"));
        //        } else if (meta.getBoolean("snapshot", false)) {
        //            snapshot(name, frame, MetaBuilder.buildEmpty("snapshot"));
        //        }
        //
        //        if (meta.hasMeta("serialize")) {
        //            serialize(name, frame, meta.getMeta("serialize"));
        //        } else if (meta.getBoolean("serialize", false)) {
        //            serialize(name, frame, MetaBuilder.buildEmpty("serialize"));
        //        }

        return input
    }

    override fun afterAction(context: Context, name: String, res: Table, meta: Laminate) {
        // это необходимо сделать, чтобы снапшоты и сериализация выполнялись после того, как все графики построены
        //        snapshotTasks.values().stream().forEach((r) -> r.run());
        //        snapshotTasks.clear();
        //        serializeTasks.values().stream().forEach((r) -> r.run());
        //        serializeTasks.clear();
        super.afterAction(context, name, res, meta)
    }
}
