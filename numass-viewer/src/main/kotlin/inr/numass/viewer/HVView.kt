package inr.numass.viewer

import hep.dataforge.configure
import hep.dataforge.fx.dfIcon
import hep.dataforge.fx.plots.PlotContainer
import hep.dataforge.fx.runGoal
import hep.dataforge.fx.ui
import hep.dataforge.names.Name
import hep.dataforge.plots.data.DataPlot
import hep.dataforge.plots.data.TimePlot
import hep.dataforge.plots.jfreechart.JFreeChartFrame
import hep.dataforge.tables.Adapters
import inr.numass.data.api.NumassSet
import javafx.collections.MapChangeListener
import javafx.scene.image.ImageView
import tornadofx.*


/**
 * View for hv
 */
class HVView : View(title = "High voltage time plot", icon = ImageView(dfIcon)) {

    private val dataController by inject<DataController>()
    private val data get() = dataController.sets

    private val frame = JFreeChartFrame().configure {
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
        plots.setType<TimePlot>()
    }
    private val container = PlotContainer(frame);

    override val root = borderpane {
        center = PlotContainer(frame).root
    }

    val isEmpty = booleanBinding(data) { data.isEmpty() }

    init {
        data.addListener { change: MapChangeListener.Change<out Name, out NumassSet> ->
            isEmpty.invalidate()
            if (change.wasRemoved()) {
                frame.plots.remove(change.key)
            }
            if (change.wasAdded()) {
                runLater { container.progress = -1.0 }
                runGoal(app.context,"hvData[${change.key}]") {
                    change.valueAdded.getHvData()
                } ui { table ->
                    if (table != null) {
                        ((frame[change.key.toString()] as? DataPlot)
                                ?: DataPlot(change.key.toString(), adapter = Adapters.buildXYAdapter("timestamp", "value")).also { frame.add(it) })
                                .fillData(table)
                    }

                    container.progress = 1.0;
                }
            }

        }
    }

}
