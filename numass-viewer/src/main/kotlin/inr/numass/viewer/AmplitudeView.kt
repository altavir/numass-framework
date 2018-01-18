package inr.numass.viewer

import hep.dataforge.fx.dfIcon
import hep.dataforge.fx.plots.PlotContainer
import hep.dataforge.fx.runGoal
import hep.dataforge.fx.ui
import hep.dataforge.goals.Goal
import hep.dataforge.kodex.configure
import hep.dataforge.meta.Meta
import hep.dataforge.plots.PlotFrame
import hep.dataforge.plots.data.DataPlot
import hep.dataforge.plots.jfreechart.JFreeChartFrame
import hep.dataforge.tables.Adapters
import hep.dataforge.tables.Table
import inr.numass.data.analyzers.NumassAnalyzer
import inr.numass.data.analyzers.SimpleAnalyzer
import inr.numass.data.analyzers.spectrumWithBinning
import inr.numass.data.api.NumassPoint
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
import java.util.concurrent.ConcurrentHashMap

class AmplitudeView(
        private val analyzer: NumassAnalyzer = SimpleAnalyzer(),
        private val cache: MutableMap<NumassPoint, Table> = ConcurrentHashMap()
) : View(title = "Numass amplitude spectrum plot", icon = ImageView(dfIcon)) {

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
    }

    val binningProperty = SimpleObjectProperty<Int>(20)
    var binning by binningProperty

    val normalizeProperty = SimpleBooleanProperty(true)
    var normalize by normalizeProperty


    private val container = PlotContainer(frame).apply {
        val binnintSelector: ChoiceBox<Int> = ChoiceBox(FXCollections.observableArrayList(1, 2, 5, 10, 20, 50)).apply {
            minWidth = 0.0
            selectionModel.selectLast()
            binningProperty.bind(this.selectionModel.selectedItemProperty())
        }
        val normalizeSwitch: CheckBox = CheckBox("Normalize").apply {
            minWidth = 0.0
            this.selectedProperty().bindBidirectional(normalizeProperty)
        }
        addToSideBar(0, binnintSelector, normalizeSwitch)
    }

    private val data: ObservableMap<String, NumassPoint> = FXCollections.observableHashMap()
    private val plots: ObservableMap<String, Goal<DataPlot>> = FXCollections.observableHashMap()

    val isEmpty = booleanBinding(data) { data.isEmpty() }

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
            clear()
        }
        normalizeProperty.onChange {
            clear()
        }

        container.progressProperty.bind(progress)
    }

    override val root = borderpane {
        center = container.root
    }

    /**
     * Calculate or get spectrum from the immutable
     */
    private suspend fun getSpectrum(point: NumassPoint): Table {
        return cache.computeIfAbsent(point) { analyzer.getAmplitudeSpectrum(point, Meta.empty()) }
    }

    /**
     * Put or replace current plot with name `key`
     */
    fun add(key: String, point: NumassPoint) {
        data.put(key, point)
    }

    fun addAll(data: Map<String, NumassPoint>) {
        this.data.putAll(data);
    }

    private fun invalidate() {
        data.forEach { key, point ->
            plots.computeIfAbsent(key) {
                runGoal("loadAmplitudeSpectrum_$key") {
                    val valueAxis = if (normalize) {
                        NumassAnalyzer.COUNT_RATE_KEY
                    } else {
                        NumassAnalyzer.COUNT_KEY
                    }
                    DataPlot.plot(
                            key,
                            Adapters.buildXYAdapter(NumassAnalyzer.CHANNEL_KEY, valueAxis),
                            spectrumWithBinning(getSpectrum(point), binning)
                    ).configure {
                        "connectionType" to "step"
                        "thickness" to 2
                        "showLine" to true
                        "showSymbol" to false
                        "showErrors" to false
                        "JFreeChart.cache" to true
                    }
                } ui { plot ->
                    frame.add(plot)
                    progress.invalidate()
                }
            }
            plots.keys.filter { !data.containsKey(it) }.forEach { remove(it) }
        }
    }

    fun clear() {
        frame.plots.clear()
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
    fun setAll(map: Map<String, NumassPoint>) {
        plots.clear();
        //Remove obsolete keys
        data.keys.filter { !map.containsKey(it) }.forEach {
            remove(it)
        }
        this.addAll(map);
    }

}
