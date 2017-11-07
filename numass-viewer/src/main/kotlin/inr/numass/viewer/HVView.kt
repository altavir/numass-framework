package inr.numass.viewer

import hep.dataforge.fx.dfIcon
import hep.dataforge.fx.plots.PlotContainer
import hep.dataforge.fx.runGoal
import hep.dataforge.fx.ui
import hep.dataforge.kodex.configure
import hep.dataforge.plots.PlotFrame
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
    }
    private val container = PlotContainer(frame);

    override val root = borderpane {
        center = PlotContainer(frame).root
    }

    private val data: ObservableMap<String, NumassSet> = FXCollections.observableHashMap()
    val isEmpty = booleanBinding(data) { data.isEmpty() }

    init {
        data.addListener { change: MapChangeListener.Change<out String, out NumassSet> ->
            if (change.wasRemoved()) {
                frame.remove(change.key)
            }
            if (change.wasAdded()) {
                runLater { container.progress = -1.0 }
                runGoal("hvData[${change.key}]") {
                    change.valueAdded.hvData
                } ui { hvData ->
                    hvData.ifPresent {
                        for (dp in it) {
                            //val blockName = dp.getString("block", "default").replace(".", "_");
                            //val opt = frame.opt(blockName)
                            val plot = frame.opt(change.key).orElseGet {
                                TimePlot(change.key).configure {
                                    "connectionType" to "step"
                                    "thickness" to 2
                                    "showLine" to true
                                    "showSymbol" to false
                                    "showErrors" to false
                                }.apply { frame.add(this) }
                            } as TimePlot;
                            plot.put(dp.getValue("timestamp").timeValue(), dp.getValue("value"))
                        }
                    }
                    container.progress = 1.0;
                }
            }

        }
    }


    fun add(id: String, set: NumassSet) {
        data.put(id, set)
    }

    fun remove(id: String) {
        data.remove(id);
    }


}
