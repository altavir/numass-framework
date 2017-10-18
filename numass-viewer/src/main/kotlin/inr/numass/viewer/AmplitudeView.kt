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
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.scene.control.CheckBox
import javafx.scene.control.ChoiceBox
import javafx.scene.image.ImageView
import tornadofx.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

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

    override val root = borderpane {
        center = container.root
    }

    private fun getSpectrum(point: NumassPoint): Table {
        return cache.computeIfAbsent(point) { analyzer.getSpectrum(point, Meta.empty()) }

    }

    private fun updateView() {
        val valueAxis = if (normalize) {
            NumassAnalyzer.COUNT_RATE_KEY
        } else {
            NumassAnalyzer.COUNT_KEY
        }

        val progress = AtomicInteger(0);
        runLater { container.progress = 0.0 }

        runAsync {
            val totalCount = data.size

            data.map { entry ->
                val seriesName = String.format("%s: %.2f", entry.key, entry.value.voltage)
                DataPlot.plot(
                        seriesName,
                        XYAdapter(NumassAnalyzer.CHANNEL_KEY, valueAxis),
                        NumassDataUtils.spectrumWithBinning(getSpectrum(entry.value), binning)
                ).configure {
                    "connectionType" to "step"
                    "thickness" to 2
                    "showLine" to true
                    "showSymbol" to false
                    "showErrors" to false
                    "JFreeChart.cache" to true
                }.also {
                    runLater { container.progress = progress.incrementAndGet().toDouble() / data.size }
                }
            }
        } ui { plots ->
            frame.setAll(plots)
            //detectorDataExportButton.isDisable = false
        }
    }

    fun update(map: Map<String, NumassPoint>) {
        synchronized(data) {
            //Remove obsolete keys
            data.keys.filter { !map.containsKey(it) }.forEach {
                data.remove(it)
                frame.remove(it);
            }
            this.data.putAll(map);
            updateView()
        }
    }
}
