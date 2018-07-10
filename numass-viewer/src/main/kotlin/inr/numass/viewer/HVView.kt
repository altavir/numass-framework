package inr.numass.viewer

import hep.dataforge.fx.dfIcon
import hep.dataforge.fx.plots.PlotContainer
import hep.dataforge.fx.runGoal
import hep.dataforge.fx.ui
import hep.dataforge.kodex.configure
import hep.dataforge.plots.PlotFrame
import hep.dataforge.plots.data.DataPlot
import hep.dataforge.plots.data.TimePlot
import hep.dataforge.plots.jfreechart.JFreeChartFrame
import inr.numass.data.api.NumassSet
import javafx.collections.FXCollections
import javafx.collections.MapChangeListener
import javafx.collections.ObservableMap
import javafx.scene.image.ImageView
import tornadofx.*


/**
 * View for hv
 */
class HVView : View(title = "High voltage time plot", icon = ImageView(dfIcon)) {

    private val frame: PlotFrame = JFreeChartFrame().configure {
        "xAxis.title" to "time"
        "xAxis.type" to "time"
        "yAxis.title" to "HV"
    }.apply {
        plots.configure {
            "connectionType" to "step"
            "thickness" to 2
            "showLine" to true
            "showSymbol" to false
            "showErrors" to false
        }
        plots.setType<DataPlot>()
    }
    private val container = PlotContainer(frame);

    override val root = borderpane {
        center = PlotContainer(frame).root
    }

    private val data: ObservableMap<String, NumassSet> = FXCollections.observableHashMap()
    val isEmpty = booleanBinding(data) { data.isEmpty() }

    init {
        data.addListener { change: MapChangeListener.Change<out String, out NumassSet> ->
            isEmpty.invalidate()
            if (change.wasRemoved()) {
                frame.remove(change.key)
            }
            if (change.wasAdded()) {
                runLater { container.progress = -1.0 }
                runGoal("hvData[${change.key}]") {
                    change.valueAdded.hvData.await()
                } ui { hvData ->
                    hvData?.let {
                        for (dp in it) {
                            val plot: TimePlot = frame[change.key] as TimePlot?
                                    ?: TimePlot(change.key).apply { frame.add(this) }
                            plot.put(dp.getValue("timestamp").time, dp.getValue("value"))
                        }
                    }
                    container.progress = 1.0;
                }
            }

        }
    }


    operator fun set(id: String, set: NumassSet) {
        data[id] = set
    }

    fun remove(id: String) {
        data.remove(id);
    }

    fun clear() {
        data.clear()
    }


}
