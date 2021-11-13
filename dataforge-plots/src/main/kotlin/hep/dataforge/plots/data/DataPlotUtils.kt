/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.plots.data

import hep.dataforge.plots.Plot
import hep.dataforge.plots.XYPlotFrame
import hep.dataforge.tables.Adapters
import hep.dataforge.tables.ListTable
import hep.dataforge.tables.MetaTableFormat
import hep.dataforge.tables.Table
import hep.dataforge.values.Value
import hep.dataforge.values.ValueMap

/**
 * @author Alexander Nozik
 */
object DataPlotUtils {

    fun collectXYDataFromPlot(frame: XYPlotFrame, visibleOnly: Boolean): Table {

        val points = LinkedHashMap<Value, ValueMap.Builder>()
        val names = ArrayList<String>()
        names.add("x")

        frame.plots.stream().map { it.second }
            .filter { !visibleOnly || it.config.getBoolean("visible", true) }
            .forEach {
                (it as? Plot)?.let { plot ->
                    names.add(plot.title)
                    plot.data.forEach { point ->
                        val x = Adapters.getXValue(plot.adapter, point)
                        val mdp: ValueMap.Builder = points.getOrPut(x) {
                            ValueMap.Builder().apply { putValue("x", x) }
                        }
                        mdp.putValue(plot.title, Adapters.getYValue(plot.adapter, point))
                    }
                }
            }

        val res = ListTable.Builder(MetaTableFormat.forNames(names))
        res.rows(points.values.stream().map { it.build() }.toList())
        return res.build()
    }
}
