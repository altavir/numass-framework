package inr.numass.viewer

import hep.dataforge.kodex.configure
import hep.dataforge.kodex.fx.dfIcon
import hep.dataforge.kodex.fx.plots.PlotContainer
import hep.dataforge.meta.Meta
import hep.dataforge.plots.PlotFrame
import hep.dataforge.plots.data.DataPlot
import hep.dataforge.plots.jfreechart.JFreeChartFrame
import hep.dataforge.tables.Table
import hep.dataforge.tables.XYAdapter
import inr.numass.data.NumassDataUtils
import inr.numass.data.analyzers.SimpleAnalyzer
import inr.numass.data.api.NumassAnalyzer
import inr.numass.data.api.NumassPoint
import javafx.beans.Observable
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableMap
import javafx.concurrent.Task
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
            "axisTitle" to "ADC"
            "axisUnits" to "channels"

        }
        node("yAxis") {
            "axisTitle" to "count rate"
            "axisUnits" to "Hz"
        }
        "legend.show" to false
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

    private val data: MutableMap<String, NumassPoint> = HashMap();
    private val taskMap: ObservableMap<String, Task<DataPlot>> = FXCollections.observableHashMap();

    init {
        binningProperty.onChange {
            putAll(data)
        }
        normalizeProperty.onChange {
            putAll(data)
        }
        taskMap.addListener { _: Observable ->
            runLater {
                val running = taskMap.values.count { it.isRunning }

                if (running == 0) {
                    container.progress = 1.0
                } else {
                    container.progress = running.toDouble() / taskMap.size
                }
            }
        }
    }

    override val root = borderpane {
        center = container.root
    }

    /**
     * Calculate or get spectrum from the cache
     */
    private fun getSpectrum(point: NumassPoint): Table {
        return cache.computeIfAbsent(point) { analyzer.getSpectrum(point, Meta.empty()) }

    }

    fun cleanTasks() {
        runLater {
            taskMap.entries.filter { !it.value.isRunning }.forEach { taskMap.remove(it.key) }
        }
    }

    /**
     * Put or replace current plot with name `key`
     */
    fun putOne(key: String, point: NumassPoint): Task<DataPlot> {
        val valueAxis = if (normalize) {
            NumassAnalyzer.COUNT_RATE_KEY
        } else {
            NumassAnalyzer.COUNT_KEY
        }

        data.put(key, point)

        val res = runAsync {
            val seriesName = String.format("%s: %.2f", key, point.voltage)
            DataPlot.plot(
                    seriesName,
                    XYAdapter(NumassAnalyzer.CHANNEL_KEY, valueAxis),
                    NumassDataUtils.spectrumWithBinning(getSpectrum(point), binning)
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
            //detectorDataExportButton.isDisable = false
        }

        taskMap.put(key, res);

        return res;
    }

    fun putAll(data: Map<String, NumassPoint>): Map<String, Task<DataPlot>> {
        cleanTasks()
        return data.mapValues { entry ->
            putOne(entry.key, entry.value)
        }
    }

    /**
     * Remove the plot and cancel loading task if it is in progress.
     */
    fun remove(name: String) {
        frame.remove(name);
        taskMap[name]?.cancel();
        taskMap.remove(name);
        data.remove(name)
    }

    /**
     * Set frame content to the given map. All keys not in the map are removed.
     */
    fun setAll(map: Map<String, NumassPoint>) {
        taskMap.clear();
        //Remove obsolete keys
        data.keys.filter { !map.containsKey(it) }.forEach {
            remove(it)
        }
        this.putAll(map);
    }

}
