package inr.numass.viewer

import hep.dataforge.configure
import hep.dataforge.fx.dfIcon
import hep.dataforge.fx.plots.PlotContainer
import hep.dataforge.fx.runGoal
import hep.dataforge.fx.ui
import hep.dataforge.plots.PlotGroup
import hep.dataforge.plots.data.DataPlot
import hep.dataforge.plots.jfreechart.JFreeChartFrame
import hep.dataforge.storage.tables.TableLoader
import hep.dataforge.storage.tables.asTable
import hep.dataforge.tables.Adapters
import hep.dataforge.tables.Table
import javafx.collections.FXCollections
import javafx.collections.MapChangeListener
import javafx.collections.ObservableMap
import javafx.scene.image.ImageView
import kotlinx.coroutines.Dispatchers
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

    val data: ObservableMap<String, TableLoader> = FXCollections.observableHashMap();
    val isEmpty = booleanBinding(data) {
        data.isEmpty()
    }

    init {
        data.addListener { change: MapChangeListener.Change<out String, out TableLoader> ->
            if (change.wasRemoved()) {
                plot.remove(change.key)
            }
            if (change.wasAdded()) {
                runGoal(app.context,"loadTable[${change.key}]", Dispatchers.IO) {
                    val plotData = getData(change.valueAdded)
                    val names = plotData.format.namesAsArray().filter { it != "timestamp" }

                    val group = PlotGroup(change.key)

                    names.forEach {
                        val adapter = Adapters.buildXYAdapter("timestamp", it);
                        val plot = DataPlot.plot(it, plotData, adapter).configure {
                            "showLine" to true
                            "showSymbol" to false
                            "showErrors" to false
                        }
                        group.add(plot)
                    }
                    group
                } ui {
                    plot.add(it);
                }
            }
            isEmpty.invalidate()
        }
    }

    private suspend fun getData(loader: TableLoader): Table {
        //TODO add query
        return loader.asTable().await()
    }

    operator fun set(id: String, loader: TableLoader) {
        this.data[id] = loader
    }

    fun remove(id: String) {
        this.data.remove(id)
    }

    fun clear(){
        data.clear()
    }

}
