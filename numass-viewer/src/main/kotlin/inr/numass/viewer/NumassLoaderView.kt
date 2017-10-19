package inr.numass.viewer

import hep.dataforge.context.Context
import hep.dataforge.context.Global
import hep.dataforge.io.ColumnedDataWriter
import hep.dataforge.kodex.buildMeta
import hep.dataforge.kodex.fx.plots.PlotContainer
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder
import hep.dataforge.plots.PlotGroup
import hep.dataforge.plots.XYPlotFrame
import hep.dataforge.plots.data.DataPlot
import hep.dataforge.plots.data.DataPlotUtils
import hep.dataforge.plots.data.TimePlot
import hep.dataforge.plots.jfreechart.JFreeChartFrame
import hep.dataforge.storage.commons.JSONMetaWriter
import hep.dataforge.tables.Table
import hep.dataforge.tables.ValueMap
import hep.dataforge.tables.XYAdapter
import inr.numass.data.NumassDataUtils
import inr.numass.data.analyzers.SimpleAnalyzer
import inr.numass.data.api.NumassAnalyzer
import inr.numass.data.api.NumassPoint
import inr.numass.data.api.NumassSet
import javafx.application.Platform
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.scene.control.*
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.BorderPane
import javafx.stage.FileChooser
import javafx.util.converter.NumberStringConverter
import org.controlsfx.control.RangeSlider
import org.controlsfx.validation.ValidationSupport
import org.controlsfx.validation.Validator
import org.slf4j.LoggerFactory
import tornadofx.*
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Level
import java.util.stream.Collectors


/**
 * Numass loader view
 *
 * Created by darksnake on 14-Apr-17.
 */
class NumassLoaderView : View() {
    override val root: AnchorPane by fxml("/fxml/NumassLoaderView.fxml")
//    lateinit var main: MainView

    private val detectorPlotPane: BorderPane by fxid();
    //    private val tabPane: TabPane by fxid();
    private val infoTextBox: TextArea by fxid();
    private val spectrumPlotPane: BorderPane by fxid();
    private val lowChannelField: TextField by fxid();
    private val upChannelField: TextField by fxid();
    private val channelSlider: RangeSlider by fxid();
    private val dTimeField: TextField by fxid();
    private val hvPane: BorderPane by fxid();
    private val spectrumExportButton: Button by fxid();

//    private val detectorPlot: PlotContainer = PlotContainer.centerIn(detectorPlotPane)
//    private val spectrumPlot: PlotContainer = PlotContainer.centerIn(spectrumPlotPane)
//    private val hvPlot: PlotContainer = PlotContainer.centerIn(hvPane)


    private val detectorBinningSelector: ChoiceBox<Int> = ChoiceBox(FXCollections.observableArrayList(1, 2, 5, 10, 20, 50))
    private val detectorNormalizeSwitch: CheckBox = CheckBox("Normalize")
    private val detectorDataExportButton: Button = Button("Export")

    val dataProperty = SimpleObjectProperty<NumassSet>()
    var data: NumassSet? by dataProperty

    val analyzerProperty = SimpleObjectProperty<NumassAnalyzer>(SimpleAnalyzer())
    var analyzer: NumassAnalyzer by analyzerProperty

    private val spectra = HashMap<Double, Table>();//spectra cache

    val spectrumData = DataPlot("spectrum")
    val hvPlotData = PlotGroup("hv")
    //private var points = FXCollections.observableArrayList<NumassPoint>()

    val detectorPlotFrame = JFreeChartFrame(
            MetaBuilder("frame")
                    .setValue("title", "Detector response plot")
                    .setNode(MetaBuilder("xAxis")
                            .setValue("axisTitle", "ADC")
                            .setValue("axisUnits", "channels")
                            .build())
                    .setNode(MetaBuilder("yAxis")
                            .setValue("axisTitle", "count rate")
                            .setValue("axisUnits", "Hz")
                            .build())
                    .setNode(MetaBuilder("legend")
                            .setValue("show", false))
                    .build()
    )

    val plottableConfig = MetaBuilder("plot")
            .setValue("connectionType", "step")
            .setValue("thickness", 2)
            .setValue("showLine", true)
            .setValue("showSymbol", false)
            .setValue("showErrors", false)
            .setValue("JFreeChart.cache", true)
            .build()


    private val detectorPlot: PlotContainer = PlotContainer(detectorPlotFrame);
    private val spectrumPlot: PlotContainer;
    private val hvPlot: PlotContainer;

    init {
        //setup detector pane frame and sidebar
        val l = Label("Bin size:")
        l.padding = Insets(5.0)
        detectorBinningSelector.maxWidth = java.lang.Double.MAX_VALUE
        detectorBinningSelector.selectionModel.clearAndSelect(4)

        detectorNormalizeSwitch.isSelected = true
        detectorNormalizeSwitch.padding = Insets(5.0)

        detectorPlotPane.center = detectorPlot.root
        detectorPlot.addToSideBar(0, l, detectorBinningSelector, detectorNormalizeSwitch, Separator(Orientation.HORIZONTAL))

        detectorDataExportButton.maxWidth = java.lang.Double.MAX_VALUE
        detectorDataExportButton.onAction = EventHandler { this.onExportButtonClick(it) }
        detectorPlot.addToSideBar(detectorDataExportButton)

        detectorPlot.sideBarPoistion = 0.7
        //setup spectrum pane

        spectrumExportButton.onAction = EventHandler { this.onSpectrumExportClick(it) }

        val spectrumPlotMeta = MetaBuilder("plot")
                .setValue("xAxis.axisTitle", "U")
                .setValue("xAxis.axisUnits", "V")
                .setValue("yAxis.axisTitle", "count rate")
                .setValue("yAxis.axisUnits", "Hz")
                .setValue("legend.show", false)
        spectrumPlot = PlotContainer(JFreeChartFrame(spectrumPlotMeta).apply { add(spectrumData) })
        spectrumPlotPane.center = spectrumPlot.root

        lowChannelField.textProperty().bindBidirectional(channelSlider.lowValueProperty(), NumberStringConverter())
        upChannelField.textProperty().bindBidirectional(channelSlider.highValueProperty(), NumberStringConverter())

        channelSlider.highValue = 1900.0
        channelSlider.lowValue = 300.0

        detectorBinningSelector.selectionModel.selectedItemProperty().addListener { observable, oldValue, newValue ->
            if (data != null) {
                updateDetectorPane(data!!)
            }
        }

        detectorNormalizeSwitch.selectedProperty().addListener { observable, oldValue, newValue ->
            if (data != null) {
                updateDetectorPane(data!!)
            }
        }


        dTimeField.textProperty().addListener { _: ObservableValue<out String>, _: String, _: String ->
            if (data != null) {
                updateSpectrum(data!!)
            }
        }

        val rangeChangeListener = { _: ObservableValue<out Number>, _: Number, _: Number ->
            if (data != null) {
                updateSpectrum(data!!)
            }
        }

        channelSlider.lowValueProperty().addListener(rangeChangeListener)
        channelSlider.highValueProperty().addListener(rangeChangeListener)

        val validationSupport = ValidationSupport()
        val isNumber = { t: String ->
            try {
                java.lang.Double.parseDouble(t)
                true
            } catch (ex: Exception) {
                false
            }
        }

        validationSupport.registerValidator(dTimeField, Validator.createPredicateValidator(isNumber, "Must be number"))

        //setup HV frame
        val hvPlotMeta = MetaBuilder("plot")
                .setValue("xAxis.axisTitle", "time")
                .setValue("xAxis.type", "time")
                .setValue("yAxis.axisTitle", "HV")
        hvPlot = PlotContainer(JFreeChartFrame(hvPlotMeta))
        hvPane.center = hvPlot.root

        dataProperty.addListener { observable, oldValue, newData ->
            //clearing spectra cache
            if (oldValue != newData) {
                spectra.clear()
            }

            if (newData != null) {
                runAsync {
                    updateTitle("Load numass data (" + newData.name + ")")

                    //setup info
                    updateInfo(newData)
                    //setup hv frame
                    updateHV(newData)
                    //setup spectrum frame
                    updateSpectrum(newData)
                    //setup detector data
                    updateDetectorPane(newData)

                }
            } else {
                spectrumData.clear()
                hvPlotData.clear()
            }
        }


    }

    fun getContext(): Context {
        return Global.getDefaultContext();
    }


    fun loadData(data: NumassSet?) {
        this.data = data;
//        this.data = if (data == null) {
//            data
//        } else {
//            NumassDataCache(data)
//        }
    }

    private fun updateHV(data: NumassSet) {
        hvPlotData.clear()
        runAsync {
            data.hvData
        } ui { hvData ->
            hvData.ifPresent {
                for (dp in it) {
                    val block = dp.getString("block", "default")
                    if (!hvPlotData.has(block)) {
                        hvPlotData.add(TimePlot(block))
                    }
                    (hvPlotData.opt(block).orElseThrow{RuntimeException()} as TimePlot)
                            .put(dp.getValue("timestamp").timeValue(), dp.getValue("value"))
                }
                hvPlot.frame.add(hvPlotData)
            }
        }

    }


    private fun updateInfo(data: NumassSet) {
        val info = data.meta()
        infoTextBox.text = JSONMetaWriter().writeString(info).replace("\\r", "\r\t").replace("\\n", "\n\t")
    }

    /**
     * Get energy spectrum for a specific point
     */
    private fun getSpectrum(point: NumassPoint): Table {
        synchronized(this) {
            return spectra.computeIfAbsent(point.voltage) { analyzer.getSpectrum(point, Meta.empty()) }
        }
    }

    private fun updateSpectrum(data: NumassSet) {
        runAsync {
            val loChannel = channelSlider.lowValue.toShort()
            val upChannel = channelSlider.highValue.toShort()
            data.points.map { point ->
                val count = NumassAnalyzer.countInWindow(getSpectrum(point), loChannel, upChannel);
                val seconds = point.length.toMillis() / 1000.0;
                runLater { spectrumPlot.progress = -1.0 }
                ValueMap.ofMap(
                        mapOf(
                                XYAdapter.X_AXIS to point.voltage,
                                XYAdapter.Y_AXIS to (count / seconds),
                                XYAdapter.Y_ERROR_KEY to Math.sqrt(count.toDouble()) / seconds
                        )
                )
            }.collect(Collectors.toList())
        } ui { points ->
            spectrumData.fillData(points)
            spectrumPlot.progress = 1.0
            spectrumExportButton.isDisable = false
        }
    }

    private val dTime: Double
        get() {
            try {
                return java.lang.Double.parseDouble(dTimeField.text) * 1e-6
            } catch (ex: NumberFormatException) {
                return 0.0
            }
        }

    /**
     * update detector pane with new data
     */
    private fun updateDetectorPane(data: NumassSet) {
        Platform.runLater { detectorPlotFrame.clear() }

        val binning = detectorBinningSelector.value

        val valueAxis = if (detectorNormalizeSwitch.isSelected) {
            NumassAnalyzer.COUNT_RATE_KEY
        } else {
            NumassAnalyzer.COUNT_KEY
        }

        runAsync {
            Platform.runLater { detectorPlot.progressProperty.bind(progressProperty()) }
            val totalCount = data.points.count();
            val index = AtomicInteger(0);
            data.points.map { point ->
                val seriesName = String.format("%d: %.2f", index.incrementAndGet(), point.voltage)
                DataPlot.plot(
                        seriesName,
                        XYAdapter(NumassAnalyzer.CHANNEL_KEY, valueAxis),
                        NumassDataUtils.spectrumWithBinning(getSpectrum(point), binning)
                ).apply {
                    configure(plottableConfig)
                }.also {
                    updateProgress(index.get().toLong(), totalCount);
                }
            }.collect(Collectors.toList())
        } ui { plots ->
            detectorPlotFrame.setAll(plots)
            detectorDataExportButton.isDisable = false
        }
    }

    private fun onSpectrumExportClick(event: ActionEvent) {
        if (data != null) {
            val fileChooser = FileChooser()
            fileChooser.title = "Choose text export destination"
            fileChooser.initialFileName = data!!.name + "_spectrum.onComplete"
            val destination = fileChooser.showSaveDialog(spectrumPlotPane.scene.window)
            if (destination != null) {
                val names = arrayOf("Uset", "Uread", "Length", "Total", "Window", "CR", "CRerr", "Timestamp")
                val loChannel = channelSlider.lowValue.toInt()
                val upChannel = channelSlider.highValue.toInt()
                val dTime = dTime
//                    val spectrumDataSet = ListTable.Builder(*names)
//
//                    for (point in points) {
//                        spectrumDataSet.row(
//                                point.voltage,
//                                point.voltage,
//                                point.length,
//                                point.totalCount,
//                                point.getCountInWindow(loChannel, upChannel),
//                                NumassDataUtils.countRateWithDeadTime(point, loChannel, upChannel, dTime),
//                                NumassDataUtils.countRateWithDeadTimeErr(point, loChannel, upChannel, dTime),
//                                point.startTime
//                        )
//                    }
                val spectrumDataSet = analyzer.analyzeSet(data, buildMeta {
                    "window.lo" to loChannel
                    "window.up" to upChannel
                })

                try {
                    val comment = String.format("Numass data viewer spectrum data export for %s%n"
                            + "Window: (%d, %d)%n"
                            + "Dead time per event: %g%n",
                            data!!.name, loChannel, upChannel, dTime)

                    ColumnedDataWriter
                            .writeTable(destination, spectrumDataSet, comment, false)
                } catch (ex: IOException) {
                    log.log(Level.SEVERE, "Destination file not found", ex)
                }

            }
        }

    }

    private fun onExportButtonClick(event: ActionEvent) {
        val fileChooser = FileChooser()
        fileChooser.title = "Choose text export destination"
        fileChooser.initialFileName = data!!.name + "_detector.out"
        val destination = fileChooser.showSaveDialog(detectorPlotPane.scene.window)
        if (destination != null) {
            val detectorData = DataPlotUtils.collectXYDataFromPlot(detectorPlot.frame as XYPlotFrame, true)
            try {
                ColumnedDataWriter.writeTable(
                        destination,
                        detectorData,
                        "Numass data viewer detector data export for " + data!!.name,
                        false
                )
            } catch (ex: IOException) {
                LoggerFactory.getLogger(javaClass).error("Destination file not found", ex)
            }

        }

    }

}
