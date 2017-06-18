package inr.numass.viewer

import hep.dataforge.kodex.buildMeta
import hep.dataforge.meta.Meta
import hep.dataforge.plots.Plottable
import hep.dataforge.plots.data.PlottableData
import hep.dataforge.plots.fx.PlotContainer
import hep.dataforge.plots.jfreechart.JFreeChartFrame
import hep.dataforge.storage.api.PointLoader
import hep.dataforge.storage.api.ValueIndex
import hep.dataforge.tables.DataPoint
import hep.dataforge.tables.ListTable
import hep.dataforge.tables.Table
import hep.dataforge.tables.XYAdapter
import tornadofx.*

/**
 * Created by darksnake on 18.06.2017.
 */
class SlowControlView : View("My View") {
    private val plotMeta = buildMeta("plot") {
    }

    val plot = JFreeChartFrame(plotMeta)

    override val root = borderpane {
        PlotContainer.centerIn(this).plot = plot
    }

    fun load(loader: PointLoader) {
        runAsync {
            val data = getData(loader)
            ArrayList<Plottable>().apply {
                loader.format.columns.filter { it.name != "timestamp" }.forEach {
                    val adapter = XYAdapter("timestamp", it.name);
                    this += PlottableData.plot("data", adapter, data);
                }
            }
        } ui {
            plot.setAll(it)
        }
    }

    private fun getData(loader: PointLoader, query: Meta = Meta.empty()): Table {
        val index: ValueIndex<DataPoint>

        //use custom index if needed
        if (query.hasValue("index")) {
            index = loader.getIndex(query.getString("index", ""))
        } else {
            //use loader default one otherwise
            index = loader.index
        }
        try {
            return ListTable(loader.format, index.query(query))
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

    }

}
