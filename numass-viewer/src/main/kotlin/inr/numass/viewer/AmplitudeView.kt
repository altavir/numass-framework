package inr.numass.viewer

import hep.dataforge.configure
import hep.dataforge.fx.dfIcon
import hep.dataforge.fx.plots.PlotContainer
import hep.dataforge.names.Name
import hep.dataforge.plots.PlotGroup
import hep.dataforge.plots.data.DataPlot
import hep.dataforge.plots.jfreechart.JFreeChartFrame
import hep.dataforge.tables.Adapters
import inr.numass.data.analyzers.NumassAnalyzer
import inr.numass.data.analyzers.withBinning
import javafx.beans.binding.DoubleBinding
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.collections.MapChangeListener
import javafx.collections.ObservableMap
import javafx.scene.control.CheckBox
import javafx.scene.control.ChoiceBox
import javafx.scene.image.ImageView
import kotlinx.coroutines.*
import kotlinx.coroutines.javafx.JavaFx
import tornadofx.*

class AmplitudeView : View(title = "Numass amplitude spectrum plot", icon = ImageView(dfIcon)) {
    private val dataController by inject<DataController>()
    private val data get() = dataController.points

    private val frame = JFreeChartFrame().configure {
        "title" to "Detector response plot"
        node("xAxis") {
            "title" to "ADC"
            "units" to "channels"

        }
        node("yAxis") {
            "title" to "count rate"
            "units" to "Hz"
        }
        "legend.showComponent" to false
    }.apply {
        plots.configure {
            "connectionType" to "step"
            "thickness" to 2
            "showLine" to true
            "showSymbol" to false
            "showErrors" to false
        }.setType<DataPlot>()
    }

    val binningProperty = SimpleIntegerProperty(2)
    var binning: Int by binningProperty

    val normalizeProperty = SimpleBooleanProperty(true)
    var normalize by normalizeProperty


    private val plotContainer = PlotContainer(frame).apply {
        val binningSelector: ChoiceBox<Int> = ChoiceBox(FXCollections.observableArrayList(1, 2, 8, 16, 32)).apply {
            minWidth = 0.0
            selectionModel.select(binning as Int?)
            binningProperty.bind(this.selectionModel.selectedItemProperty())
        }
        val normalizeSwitch: CheckBox = CheckBox("Normalize").apply {
            minWidth = 0.0
            this.selectedProperty().bindBidirectional(normalizeProperty)
        }
        addToSideBar(0, binningSelector, normalizeSwitch)
    }

    private val plotJobs: ObservableMap<String, Job> = FXCollections.observableHashMap()


    private val progress = object : DoubleBinding() {
        init {
            bind(plotJobs)
        }

        override fun computeValue(): Double = plotJobs.values.count { it.isCompleted }.toDouble() / plotJobs.size
    }

    init {
        data.addListener(MapChangeListener { change ->
            val key = change.key.toString()
            if (change.wasAdded()) {
                replotOne(key, change.valueAdded)
            } else if (change.wasRemoved()) {
                plotJobs[key]?.cancel()
                plotJobs.remove(key)
                frame.plots.remove(Name.ofSingle(key))
                progress.invalidate()
            }
        })

        binningProperty.onChange {
            replot()
        }

        normalizeProperty.onChange {
            replot()
        }

        plotContainer.progressProperty.bind(progress)
    }

    private fun replotOne(key: String, point: DataController.CachedPoint) {
        plotJobs[key]?.cancel()
        frame.plots.remove(Name.ofSingle(key))
        plotJobs[key] = app.context.launch {
            withContext(Dispatchers.JavaFx) {
                progress.invalidate()
            }
            val valueAxis = if (normalize) {
                NumassAnalyzer.COUNT_RATE_KEY
            } else {
                NumassAnalyzer.COUNT_KEY
            }
            val adapter = Adapters.buildXYAdapter(NumassAnalyzer.CHANNEL_KEY, valueAxis)

            val channels = point.channelSpectra.await()

            val plot = if (channels.size == 1) {
                DataPlot.plot(
                    key,
                    channels.values.first().withBinning(binning),
                    adapter
                )
            } else {
                val group = PlotGroup.typed<DataPlot>(key)
                channels.forEach { (key, spectrum) ->
                    val plot = DataPlot.plot(
                        key.toString(),
                        spectrum.withBinning(binning),
                        adapter
                    )
                    group.add(plot)
                }
                group
            }
            withContext(Dispatchers.JavaFx) {
                frame.add(plot)
            }
        }.apply {
            invokeOnCompletion {
                runLater{
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
            replotOne(key.toString(), point)
        }
    }

    override val root = borderpane {
        center = plotContainer.root
    }
}
