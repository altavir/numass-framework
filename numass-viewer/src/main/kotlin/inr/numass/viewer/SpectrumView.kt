package inr.numass.viewer

import hep.dataforge.fx.dfIcon
import hep.dataforge.fx.plots.PlotContainer
import hep.dataforge.fx.runGoal
import hep.dataforge.fx.ui
import hep.dataforge.kodex.configure
import hep.dataforge.meta.Meta
import hep.dataforge.plots.PlotFrame
import hep.dataforge.plots.data.DataPlot
import hep.dataforge.plots.jfreechart.JFreeChartFrame
import hep.dataforge.tables.Adapters
import hep.dataforge.tables.Table
import inr.numass.data.analyzers.NumassAnalyzer
import inr.numass.data.analyzers.SimpleAnalyzer
import inr.numass.data.analyzers.countInWindow
import inr.numass.data.api.NumassPoint
import inr.numass.data.api.NumassSet
import javafx.beans.property.SimpleIntegerProperty
import javafx.collections.FXCollections
import javafx.collections.MapChangeListener
import javafx.collections.ObservableMap
import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.scene.image.ImageView
import javafx.util.converter.NumberStringConverter
import org.controlsfx.control.RangeSlider
import tornadofx.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.Collectors

/**
 * View for energy spectrum
 * @param analyzer
 * @param cache - optional global point immutable
 */
class SpectrumView(
        val analyzer: NumassAnalyzer = SimpleAnalyzer(),
        val cache: MutableMap<NumassPoint, Table> = ConcurrentHashMap()
) : View(title = "Numass spectrum plot", icon = ImageView(dfIcon)) {

    private val frame: PlotFrame = JFreeChartFrame().configure {
        "xAxis.title" to "U"
        "xAxis.units" to "V"
        "yAxis.title" to "count rate"
        "yAxis.units" to "Hz"
        //"legend.show" to false
    }
    private val container = PlotContainer(frame);


    private val loChannelProperty = SimpleIntegerProperty(500).apply {
        addListener { _ -> updateView() }
    }
    private var loChannel by loChannelProperty

    private val upChannelProperty = SimpleIntegerProperty(3100).apply {
        addListener { _ -> updateView() }
    }
    private var upChannel by upChannelProperty


    private val data: ObservableMap<String, NumassSet> = FXCollections.observableHashMap();
    val isEmpty = booleanBinding(data) { data.isEmpty() }

    /*
                    <BorderPane fx:id="spectrumPlotPane" prefHeight="200.0" prefWidth="200.0"
                            AnchorPane.bottomAnchor="0.0" AnchorPane.leftAnchor="0.0"
                            AnchorPane.rightAnchor="0.0" AnchorPane.topAnchor="0.0">
                    <top>
                        <ToolBar prefHeight="40.0" prefWidth="200.0" BorderPane.alignment="CENTER">
                            <VBox>
                                <Label text="Lo channel"/>
                                <TextField fx:id="lowChannelField" prefWidth="60.0"/>
                            </VBox>
                            <RangeSlider fx:id="channelSlider" accessibleRole="SLIDER"
                                         highValue="1900.0" lowValue="300.0" majorTickUnit="500.0"
                                         max="4000.0" minorTickCount="5" prefHeight="38.0"
                                         prefWidth="276.0" showTickLabels="true" showTickMarks="true">
                                <padding>
                                    <Insets left="10.0" right="10.0"/>
                                </padding>
                            </RangeSlider>
                            <VBox>
                                <Label text="Up channel"/>
                                <TextField fx:id="upChannelField" prefWidth="60.0"/>
                            </VBox>
                            <Separator orientation="VERTICAL"/>
                            <VBox>
                                <Label text="Dead time (us)"/>
                                <TextField fx:id="dTimeField" prefHeight="25.0" prefWidth="0.0"
                                           text="7.2"/>
                            </VBox>
                            <Separator orientation="VERTICAL"/>
                            <Pane minWidth="0.0" HBox.hgrow="ALWAYS"/>
                            <Button fx:id="spectrumExportButton" mnemonicParsing="false" text="Export"/>
                        </ToolBar>
                    </top>
                </BorderPane>
     */
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
                        isEditable = true;
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
        data.addListener { change: MapChangeListener.Change<out String, out NumassSet> ->
            if (change.wasRemoved()) {
                frame.remove(change.key);
            }

            if (change.wasAdded()) {
                updateView()
            }
        }
    }

    private fun getSpectrum(point: NumassPoint): Table {
        return cache.computeIfAbsent(point) { analyzer.getAmplitudeSpectrum(point, Meta.empty()) }

    }

    private fun updateView() {
        runLater { container.progress = 0.0 }
        val progress = AtomicInteger(0)
        val totalProgress = data.values.stream().mapToLong() { it.points.count() }.sum()

        data.forEach { name, set ->
            val plot = frame.opt(name).orElseGet {
                DataPlot(name).apply {
                    frame.add(this)
                }
            } as DataPlot

            runGoal("spectrumData[$name]") {
                set.points.map { point ->
                    val count = countInWindow(getSpectrum(point), loChannel.toShort(), upChannel.toShort());
                    val seconds = point.length.toMillis() / 1000.0;
                    runLater {
                        container.progress = progress.incrementAndGet().toDouble() / totalProgress
                    }
                    Adapters.buildXYDataPoint(
                            point.voltage,
                            (count / seconds),
                            Math.sqrt(count.toDouble()) / seconds
                    )
                }.collect(Collectors.toList())
            } ui { points ->
                plot.fillData(points)
                container.progress = 1.0
                //spectrumExportButton.isDisable = false
            }
        }
    }

    fun add(key: String, value: NumassSet) {
        data.put(key, NumassDataCache(value))
    }

    fun remove(key: String) {
        data.remove(key)
    }

    fun clear(){
        data.clear()
    }
}
