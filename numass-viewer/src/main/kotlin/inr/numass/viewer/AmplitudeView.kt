package inr.numass.viewer

import hep.dataforge.fx.dfIcon
import hep.dataforge.fx.plots.PlotContainer
import hep.dataforge.fx.runGoal
import hep.dataforge.fx.ui
import hep.dataforge.goals.Goal
import hep.dataforge.kodex.configure
import hep.dataforge.kodex.toList
import hep.dataforge.meta.Meta
import hep.dataforge.plots.PlotFrame
import hep.dataforge.plots.PlotGroup
import hep.dataforge.plots.Plottable
import hep.dataforge.plots.data.DataPlot
import hep.dataforge.plots.jfreechart.JFreeChartFrame
import hep.dataforge.tables.Adapters
import hep.dataforge.tables.Table
import inr.numass.data.analyzers.NumassAnalyzer
import inr.numass.data.analyzers.SimpleAnalyzer
import inr.numass.data.analyzers.withBinning
import inr.numass.data.api.MetaBlock
import inr.numass.data.api.NumassBlock
import inr.numass.data.api.NumassPoint
import inr.numass.data.channel
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
        private val cache: MutableMap<NumassBlock, Table> = ConcurrentHashMap()
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
    }.apply {
        plots.configure {
            "connectionType" to "step"
            "thickness" to 2
            "showLine" to true
            "showSymbol" to false
            "showErrors" to false
        }.setType(DataPlot::class)
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

    private val data: ObservableMap<String, NumassPoint> = FXCollections.observableHashMap()
    private val plots: ObservableMap<String, Goal<Plottable>> = FXCollections.observableHashMap()

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
    private fun getSpectrum(point: NumassBlock): Table {
        return cache.computeIfAbsent(point) { analyzer.getAmplitudeSpectrum(point, Meta.empty()) }
    }

    /**
     * Put or replace current plot with name `key`
     */
    fun add(key: String, point: NumassPoint) {
        data[key] = point
    }

    fun addAll(data: Map<String, NumassPoint>) {
        this.data.putAll(data);
    }

    /**
     * Distinct map of channel number to corresponding grouping block
     */
    private fun NumassPoint.getChannels(): Map<Int, NumassBlock> {
        return blocks.toList().groupBy { it.channel ?: 0 }.mapValues { entry ->
            if (entry.value.size == 1) {
                entry.value.first()
            } else {
                MetaBlock(entry.value)
            }
        }
    }

    private fun invalidate() {
        data.forEach { key, point ->
            plots.computeIfAbsent(key) {
                runGoal<Plottable>("loadAmplitudeSpectrum_$key") {
                    val valueAxis = if (normalize) {
                        NumassAnalyzer.COUNT_RATE_KEY
                    } else {
                        NumassAnalyzer.COUNT_KEY
                    }
                    val adapter = Adapters.buildXYAdapter(NumassAnalyzer.CHANNEL_KEY, valueAxis)

                    val channels = point.getChannels()

                    return@runGoal if (channels.size == 1) {
                        DataPlot.plot(
                                key,
                                adapter,
                                getSpectrum(point).withBinning(binning)
                        )
                    } else {
                        val group = PlotGroup.typed<DataPlot>(key)
                        channels.forEach { key, block ->
                            val plot = DataPlot.plot(
                                    key.toString(),
                                    adapter,
                                    getSpectrum(block).withBinning(binning)
                            )
                            group.add(plot)
                        }
                        group
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
