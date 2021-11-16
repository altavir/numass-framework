package inr.numass.viewer

import hep.dataforge.configure
import hep.dataforge.fx.dfIcon
import hep.dataforge.fx.plots.PlotContainer
import hep.dataforge.names.Name
import hep.dataforge.plots.data.DataPlot
import hep.dataforge.plots.jfreechart.JFreeChartFrame
import hep.dataforge.tables.Adapters
import hep.dataforge.tables.Table
import javafx.beans.binding.DoubleBinding
import javafx.collections.FXCollections
import javafx.collections.MapChangeListener
import javafx.collections.ObservableMap
import javafx.scene.image.ImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tornadofx.*

class TimeView : View(title = "Numass time spectrum plot", icon = ImageView(dfIcon)) {

    private val dataController by inject<DataController>()

    private val frame = JFreeChartFrame().configure {
        "title" to "Time plot"
        node("xAxis") {
            "title" to "delay"
            "units" to "us"

        }
        node("yAxis") {
            "title" to "number of events"
            "type" to "log"
        }
    }.apply {
        plots.configure {
            "connectionType" to "step"
            "thickness" to 2
            "showLine" to true
            "showSymbol" to false
            "showErrors" to false
        }.setType<DataPlot>()
    }

    private val container = PlotContainer(frame)

    //private val data: ObservableMap<String, NumassPoint> = FXCollections.observableHashMap()
    private val data get() = dataController.points
    private val plotJobs: ObservableMap<String, Job> = FXCollections.observableHashMap()

    val isEmpty = booleanBinding(data) { isEmpty() }

    private val progress = object : DoubleBinding() {
        init {
            bind(plotJobs)
        }

        override fun computeValue(): Double = plotJobs.values.count { it.isCompleted }.toDouble() / data.size

    }

    init {
        data.addListener(MapChangeListener { change ->
            val key = change.key
            if (change.wasAdded()) {
                replotOne(key, change.valueAdded)
            } else if(change.wasRemoved()){
                plotJobs[key]?.cancel()
                plotJobs.remove(key)
                frame.plots.remove(Name.ofSingle(key))
                progress.invalidate()
            }
        })

    }

    override val root = borderpane {
        center = container.root
    }

    private fun replotOne(key: String, point: DataController.CachedPoint) {
        plotJobs[key]?.cancel()
        plotJobs[key] = app.context.launch {
            try {
                val histogram: Table = point.timeSpectrum.await()

                val plot = DataPlot(key, adapter = Adapters.buildXYAdapter("x", "count"))
                    .configure {
                        "showLine" to true
                        "showSymbol" to false
                        "showErrors" to false
                        "connectionType" to "step"
                    }.fillData(histogram)
                withContext(Dispatchers.JavaFx) {
                    frame.add(plot)
                }
            } finally {
                withContext(Dispatchers.JavaFx) {
                    progress.invalidate()
                }
            }
        }
    }


    private fun replot() {
        frame.plots.clear()
        plotJobs.forEach { (_, job) -> job.cancel() }
        plotJobs.clear()

        data.forEach { (key, point) ->
            replotOne(key, point)
        }
    }

}

