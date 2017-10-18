package inr.numass.viewer

import hep.dataforge.kodex.configure
import hep.dataforge.kodex.fx.dfIcon
import hep.dataforge.kodex.fx.plots.PlotContainer
import hep.dataforge.plots.PlotFrame
import hep.dataforge.plots.data.TimePlot
import hep.dataforge.plots.jfreechart.JFreeChartFrame
import inr.numass.data.api.NumassSet
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


    fun update(vararg sets: NumassSet) {
        frame.plots.clear()
        container.sideBarExpanded = false

        val progress = AtomicInteger(0);
        runLater { container.progress = -1.0 }

        sets.forEach { data ->
            runAsync {
                val res = data.hvData
                runLater { container.progress = progress.incrementAndGet().toDouble() / sets.size }
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
