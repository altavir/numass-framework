package inr.numass.viewer

import hep.dataforge.kodex.configure
import hep.dataforge.kodex.fx.dfIcon
import hep.dataforge.kodex.fx.plots.PlotContainer
import hep.dataforge.meta.Meta
import hep.dataforge.plots.Plot
import hep.dataforge.plots.data.DataPlot
import hep.dataforge.plots.jfreechart.JFreeChartFrame
import hep.dataforge.storage.api.PointLoader
import hep.dataforge.storage.api.ValueIndex
import hep.dataforge.tables.ListTable
import hep.dataforge.tables.Table
import hep.dataforge.tables.XYAdapter
import hep.dataforge.values.Values
import javafx.scene.image.ImageView
import tornadofx.*

/**
 * Created by darksnake on 18.06.2017.
 */
class SlowControlView : View(title = "Numass slow control view", icon = ImageView(dfIcon)) {

    private val plot = JFreeChartFrame().configure {
        "xAxis.type" to "time"
        "yAxis.type" to "log"
    }

    override val root = borderpane {
        center = PlotContainer(plot).root
    }

    //TODO add multiple loaders
    fun load(loader: PointLoader) {
        runAsync {
            val data = getData(loader)
            ArrayList<Plot>().apply {
                loader.format.columns.filter { it.name != "timestamp" }.forEach {
                    val adapter = XYAdapter("timestamp", it.name);
                    this += DataPlot.plot(it.name, adapter, data).configure {
                        "showLine" to true
                        "showSymbol" to false
                        "showErrors" to false
                    }
                }
            }
        } ui {
            plot.setAll(it)
        }
    }

    private fun getData(loader: PointLoader, query: Meta = Meta.empty()): Table {
        val index: ValueIndex<Values> =
                if (query.hasValue("index")) {
                    //use custom index if needed
                    loader.getIndex(query.getString("index"))
                } else {
                    //use loader default one otherwise
                    loader.index
                }
        try {
            return ListTable(loader.format, index.query(query))
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

    }

}
