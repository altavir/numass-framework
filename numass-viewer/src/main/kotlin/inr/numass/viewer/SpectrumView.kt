package inr.numass.viewer

import hep.dataforge.configure
import hep.dataforge.fx.dfIcon
import hep.dataforge.fx.plots.PlotContainer
import hep.dataforge.names.Name
import hep.dataforge.plots.data.DataPlot
import hep.dataforge.plots.jfreechart.JFreeChartFrame
import hep.dataforge.tables.Adapters
import inr.numass.data.analyzers.countInWindow
import inr.numass.data.api.NumassSet
import javafx.beans.property.SimpleIntegerProperty
import javafx.collections.MapChangeListener
import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.scene.image.ImageView
import javafx.util.converter.NumberStringConverter
import kotlinx.coroutines.*
import kotlinx.coroutines.javafx.JavaFx
import org.controlsfx.control.RangeSlider
import tornadofx.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.sqrt

/**
 * View for energy spectrum
 * @param analyzer
 * @param cache - optional global point immutable
 */
class SpectrumView : View(title = "Numass spectrum plot", icon = ImageView(dfIcon)) {

    private val dataController by inject<DataController>()
    private val data get() = dataController.sets

    private val frame = JFreeChartFrame().configure {
        "xAxis.title" to "U"
        "xAxis.units" to "V"
        "yAxis.title" to "count rate"
        "yAxis.units" to "Hz"
        //"legend.show" to false
    }
    private val container = PlotContainer(frame)

    private val loChannelProperty = SimpleIntegerProperty(500).apply {
        addListener { _ -> updateView() }
    }
    private var loChannel by loChannelProperty

    private val upChannelProperty = SimpleIntegerProperty(3100).apply {
        addListener { _ -> updateView() }
    }
    private var upChannel by upChannelProperty

    private val isEmpty = booleanBinding(data) { data.isEmpty() }

    override val root = borderpane {
        top {
            toolbar {
                prefHeight = 40.0
                vbox {
                    label("Lo channel")
                    textfield {
                        prefWidth = 60.0
                        textProperty().bindBidirectional(loChannelProperty, NumberStringConverter())
                    }
                }

                items += RangeSlider().apply {
                    padding = Insets(0.0, 10.0, 0.0, 10.0)
                    prefWidth = 300.0
                    majorTickUnit = 500.0
                    minorTickCount = 5
                    prefHeight = 38.0
                    isShowTickLabels = true
                    isShowTickMarks = true

                    max = 4000.0
                    highValueProperty().bindBidirectional(upChannelProperty)
                    lowValueProperty().bindBidirectional(loChannelProperty)

                    lowValue = 500.0
                    highValue = 3100.0
                }

                vbox {
                    label("Up channel")
                    textfield {
                        isEditable = true
                        prefWidth = 60.0
                        textProperty().bindBidirectional(upChannelProperty, NumberStringConverter())
                    }
                }
                separator(Orientation.VERTICAL)
            }
        }
        center = container.root
    }

    init {
        data.addListener { change: MapChangeListener.Change<out Name, out NumassSet> ->
            if (change.wasRemoved()) {
                frame.plots.remove(Name.ofSingle(change.key.toString()))
            }

            if (change.wasAdded()) {
                updateView()
            }
            isEmpty.invalidate()
        }
    }

    private fun updateView() {
        runLater { container.progress = 0.0 }
        val progress = AtomicInteger(0)
        val totalProgress = data.values.stream().mapToInt { it.points.size }.sum()

        data.forEach { (name, set) ->
            val plot: DataPlot = frame.plots[name] as DataPlot? ?: DataPlot(name.toString()).apply {
                frame.add(this)
            }

            app.context.launch {
                val points = set.points.map { point ->
                    dataController.getCachedPoint(Name.join("$name","${point.voltage}[${point.index}]"), point).also {
                        it.spectrum.start()
                    }
                }.map { cachedPoint ->
                    val count = cachedPoint.spectrum.await().countInWindow(loChannel.toShort(), upChannel.toShort())
                    val seconds = cachedPoint.length.toMillis() / 1000.0

                    Adapters.buildXYDataPoint(
                        cachedPoint.voltage,
                        (count / seconds),
                        sqrt(count.toDouble()) / seconds
                    )
                }
                withContext(Dispatchers.JavaFx) {
                    plot.fillData(points)
                    container.progress = 1.0
                }
            }
        }
    }
}
