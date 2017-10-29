package inr.numass.viewer

import hep.dataforge.kodex.configure
import hep.dataforge.kodex.fx.dfIcon
import hep.dataforge.kodex.fx.plots.PlotContainer
import hep.dataforge.plots.PlotFrame
import hep.dataforge.plots.data.TimePlot
import hep.dataforge.plots.jfreechart.JFreeChartFrame
import inr.numass.data.api.NumassSet
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.image.ImageView
import tornadofx.*
import java.util.concurrent.atomic.AtomicInteger


/**
 * View for hv
 */
class HVView : View(title = "High voltage time plot", icon = ImageView(dfIcon)) {

    private val frame: PlotFrame = JFreeChartFrame().configure {
        "xAxis.axisTitle" to "time"
        "xAxis.type" to "time"
        "yAxis.axisTitle" to "HV"
    }
    private val container = PlotContainer(frame);

    override val root = borderpane {
        center = PlotContainer(frame).root
    }

    private val data: ObservableList<NumassSet> = FXCollections.observableArrayList()
    val isEmpty = booleanBinding(data) { data.isEmpty() }

    init {
        data.onChange { change ->
            frame.plots.clear()
            container.sideBarExpanded = false

            val progress = AtomicInteger(0);
            runLater { container.progress = -1.0 }

            change.list.forEach { data ->
                runAsync {
                    val res = data.hvData
                    runLater { container.progress = progress.incrementAndGet().toDouble() / change.list.size }
                    res
                } ui { hvData ->
                    hvData.ifPresent {
                        for (dp in it) {
                            val blockName = dp.getString("block", "default").replace(".", "_");
                            //val opt = frame.opt(blockName)
                            val plot = frame.opt(blockName).orElseGet {
                                TimePlot(blockName).configure {
                                    "connectionType" to "step"
                                    "thickness" to 2
                                    "showLine" to true
                                    "showSymbol" to false
                                    "showErrors" to false
                                }
                                        .apply { frame.add(this) }
                            } as TimePlot;
                            plot.put(dp.getValue("timestamp").timeValue(), dp.getValue("value"))
                        }
                    }
                    container.progress = 1.0;
                }
            }
        }
    }


    fun update(vararg sets: NumassSet) {
        data.setAll(*sets)
    }

    fun add(set: NumassSet) {
        this.data.add(set)
    }

    fun remove(set: NumassSet) {
        this.data.remove(set);
    }


}
