package inr.numass.viewer

import hep.dataforge.configure
import hep.dataforge.fx.dfIcon
import hep.dataforge.fx.except
import hep.dataforge.fx.plots.PlotContainer
import hep.dataforge.fx.runGoal
import hep.dataforge.fx.ui
import hep.dataforge.goals.Goal
import hep.dataforge.plots.PlotFrame
import hep.dataforge.plots.PlotGroup
import hep.dataforge.plots.Plottable
import hep.dataforge.plots.data.DataPlot
import hep.dataforge.plots.jfreechart.JFreeChartFrame
import hep.dataforge.tables.Adapters
import inr.numass.data.analyzers.NumassAnalyzer
import inr.numass.data.analyzers.withBinning
import javafx.beans.Observable
import javafx.beans.binding.DoubleBinding
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableMap
import javafx.scene.control.CheckBox
import javafx.scene.control.ChoiceBox
import javafx.scene.image.ImageView
import tornadofx.*

class AmplitudeView : View(title = "Numass amplitude spectrum plot", icon = ImageView(dfIcon)) {

    private val frame: PlotFrame = JFreeChartFrame().configure {
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

    val binningProperty = SimpleObjectProperty(20)
    var binning by binningProperty

    val normalizeProperty = SimpleBooleanProperty(true)
    var normalize by normalizeProperty


    private val container = PlotContainer(frame).apply {
        val binningSelector: ChoiceBox<Int> = ChoiceBox(FXCollections.observableArrayList(1, 2, 5, 10, 20, 50)).apply {
            minWidth = 0.0
            selectionModel.selectLast()
            binningProperty.bind(this.selectionModel.selectedItemProperty())
        }
        val normalizeSwitch: CheckBox = CheckBox("Normalize").apply {
            minWidth = 0.0
            this.selectedProperty().bindBidirectional(normalizeProperty)
        }
        addToSideBar(0, binningSelector, normalizeSwitch)
    }

    private val data: ObservableMap<String, CachedPoint> = FXCollections.observableHashMap()
    private val plots: ObservableMap<String, Goal<Plottable>> = FXCollections.observableHashMap()

    val isEmpty = booleanBinding(data) { isEmpty() }

    private val progress = object : DoubleBinding() {
        init {
            bind(plots)
        }

        override fun computeValue(): Double {
            return plots.values.count { it.isDone }.toDouble() / data.size;
        }

    }

    init {
        data.addListener { _: Observable ->
            invalidate()
        }

        binningProperty.onChange {
            frame.plots.clear()
            plots.clear()
            invalidate()
        }

        normalizeProperty.onChange {
            frame.plots.clear()
            plots.clear()
            invalidate()
        }

        container.progressProperty.bind(progress)
    }

    override val root = borderpane {
        center = container.root
    }

    /**
     * Put or replace current plot with name `key`
     */
    operator fun set(key: String, point: CachedPoint) {
        data[key] = point
    }

    fun addAll(data: Map<String, CachedPoint>) {
        this.data.putAll(data);
    }

    private fun invalidate() {
        isEmpty.invalidate()
        data.forEach { key, point ->
            plots.computeIfAbsent(key) {
                runGoal<Plottable>("loadAmplitudeSpectrum_$key") {
                    val valueAxis = if (normalize) {
                        NumassAnalyzer.COUNT_RATE_KEY
                    } else {
                        NumassAnalyzer.COUNT_KEY
                    }
                    val adapter = Adapters.buildXYAdapter(NumassAnalyzer.CHANNEL_KEY, valueAxis)

                    val channels = point.channelSpectra.await()

                    return@runGoal if (channels.size == 1) {
                        DataPlot.plot(
                                key,
                                channels.values.first().withBinning(binning),
                                adapter
                        )
                    } else {
                        val group = PlotGroup.typed<DataPlot>(key)
                        channels.forEach { key, spectrum ->
                            val plot = DataPlot.plot(
                                    key.toString(),
                                    spectrum.withBinning(binning),
                                    adapter
                            )
                            group.set(plot)
                        }
                        group
                    }
                } ui { plot ->
                    frame.add(plot)
                    progress.invalidate()
                } except {
                    throw it
                }
            }
            plots.keys.filter { !data.containsKey(it) }.forEach { remove(it) }
        }
    }

    fun clear() {
        data.clear()
        plots.values.forEach{
            it.cancel()
        }
        plots.clear()
        invalidate()
    }

    /**
     * Remove the plot and cancel loading task if it is in progress.
     */
    fun remove(name: String) {
        frame.remove(name);
        plots[name]?.cancel();
        plots.remove(name);
        data.remove(name)
    }

    /**
     * Set frame content to the given map. All keys not in the map are removed.
     */
    fun setAll(map: Map<String, CachedPoint>) {
        plots.clear();
        //Remove obsolete keys
        data.keys.filter { !map.containsKey(it) }.forEach {
            remove(it)
        }
        this.addAll(map);
    }

}
