package inr.numass.viewer

import hep.dataforge.context.Context
import hep.dataforge.context.Global
import hep.dataforge.data.Data
import hep.dataforge.fx.work.WorkManager
import hep.dataforge.io.ColumnedDataWriter
import hep.dataforge.meta.MetaBuilder
import hep.dataforge.plots.XYPlotFrame
import hep.dataforge.plots.data.PlotDataUtils
import hep.dataforge.plots.data.PlottableData
import hep.dataforge.plots.data.PlottableGroup
import hep.dataforge.plots.data.TimePlottable
import hep.dataforge.plots.fx.FXPlotFrame
import hep.dataforge.plots.fx.PlotContainer
import hep.dataforge.plots.jfreechart.JFreeChartFrame
import hep.dataforge.storage.commons.JSONMetaWriter
import hep.dataforge.tables.*
import inr.numass.data.NumassData
import inr.numass.data.NumassDataUtils
import inr.numass.data.NumassPoint
import javafx.application.Platform
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
import java.util.logging.Level
import java.util.stream.Collectors

/**
 * Numass loader view
 *
 * Created by darksnake on 14-Apr-17.
 */
class NumassLoaderView : View() {
    override val root: AnchorPane by fxml("/fxml/NumassLoaderView.fxml")
    lateinit var main: MainView

    private val detectorPlotPane: BorderPane by fxid();
    private val tabPane: TabPane by fxid();
    private val infoTextBox: TextArea by fxid();
    private val spectrumPlotPane: BorderPane by fxid();
    private val lowChannelField: TextField by fxid();
    private val upChannelField: TextField by fxid();
    private val channelSlider: RangeSlider by fxid();
    private val dTimeField: TextField by fxid();
    private val hvPane: BorderPane by fxid();
    private val spectrumExportButton: Button by fxid();

    private val detectorPlot: PlotContainer = PlotContainer.centerIn(detectorPlotPane)
    private val spectrumPlot: PlotContainer = PlotContainer.centerIn(spectrumPlotPane)
    private val hvPlot: PlotContainer = PlotContainer.centerIn(hvPane)
    private val detectorBinningSelector: ChoiceBox<Int> = ChoiceBox(FXCollections.observableArrayList(1, 2, 5, 10, 20, 50))
    private val detectorNormalizeSwitch: CheckBox = CheckBox("Normailize")
    private val detectorDataExportButton: Button = Button("Export")

    //plots data
    var data: NumassData? = null
    val spectrumData = PlottableData("spectrum")
    val hvPlotData = PlottableGroup<TimePlottable>()
    private var points = FXCollections.observableArrayList<NumassPoint>()


    init {
        //setup detector pane plot and sidebar
        val l = Label("Bin size:")
        l.padding = Insets(5.0)
        detectorBinningSelector.maxWidth = java.lang.Double.MAX_VALUE
        detectorBinningSelector.selectionModel.clearAndSelect(4)

        detectorNormalizeSwitch.isSelected = true
        detectorNormalizeSwitch.padding = Insets(5.0)

        detectorPlot.addToSideBar(0, l, detectorBinningSelector, detectorNormalizeSwitch, Separator(Orientation.HORIZONTAL))

        detectorDataExportButton.maxWidth = java.lang.Double.MAX_VALUE
        detectorDataExportButton.onAction = EventHandler { this.onExportButtonClick(it) }
        detectorPlot.addToSideBar(detectorDataExportButton)

        detectorPlot.setSideBarPosition(0.7)
        //setup spectrum pane

        spectrumExportButton.onAction = EventHandler { this.onSpectrumExportClick(it) }
        val spectrumPlotMeta = MetaBuilder("plot")
                .setValue("xAxis.axisTitle", "U")
                .setValue("xAxis.axisUnits", "V")
                .setValue("yAxis.axisTitle", "count rate")
                .setValue("yAxis.axisUnits", "Hz")
                .setValue("legend.show", false)
        spectrumPlot.plot = JFreeChartFrame(spectrumPlotMeta)

        lowChannelField.textProperty().bindBidirectional(channelSlider.lowValueProperty(), NumberStringConverter())
        upChannelField.textProperty().bindBidirectional(channelSlider.highValueProperty(), NumberStringConverter())

        channelSlider.highValue = 1900.0
        channelSlider.lowValue = 300.0

        val rangeChangeListener = { _: ObservableValue<out Number>, _: Number, _: Number -> setupSpectrumPane(points) }

        dTimeField.textProperty().addListener { _: ObservableValue<out String>, _: String, _: String -> setupSpectrumPane(points) }

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

        //setup HV plot
        val hvPlotMeta = MetaBuilder("plot")
                .setValue("xAxis.axisTitle", "time")
                .setValue("xAxis.type", "time")
                .setValue("yAxis.axisTitle", "HV")
        hvPlot.plot = JFreeChartFrame(hvPlotMeta)

    }

    fun getContext(): Context {
        return Global.getDefaultContext();
    }

    fun getWorkManager(): WorkManager {
        return getContext().getFeature(WorkManager::class.java);
    }

    fun loadData(data: NumassData?) {
        synchronized(this) {
            this.data = data
            if (data != null) {
                getWorkManager().startWork("viewer.numass.load") { work ->
                    work.title = "Load numass data (" + data.name + ")"
                    points.setAll(data.nmPoints)

                    Platform.runLater {
                        //setup detector data
                        setupDetectorPane(points)
                        //setup spectrum plot
                        setupSpectrumPane(points)
                    }
                }

                //setup hv plot
                val hvData = data.hvData
                if (hvData != null) {
                    setupHVPane(hvData)
                }
                setupInfo(data)

            } else {
                log.severe("The data model is null")
            }
//            tabPane.selectionModel.select(1)
        }
    }

    private fun setupHVPane(hvData: Data<Table>) {
        runAsync {
            hvData.get()
        } ui {
            for (pl in hvPlotData) {
                pl.clear()
            }
            for (dp in it) {
                val block = dp.getString("block", "default")
                if (!hvPlotData.has(block)) {
                    hvPlotData.add(TimePlottable(block))
                }
                hvPlotData.get(block).put(dp.getValue("timestamp").timeValue(), dp.getValue("value"))
            }
            hvPlot.plot.addAll(hvPlotData)
        }
    }

    /**
     * setup detector pane

     * @param points
     */
    private fun setupDetectorPane(points: List<NumassPoint>) {
        val normalize = detectorNormalizeSwitch.isSelected
        val binning = detectorBinningSelector.value
        updateDetectorPane(points, binning, normalize)
        detectorBinningSelector.selectionModel.selectedItemProperty()
                .addListener { observable: ObservableValue<out Int>, oldValue: Int, newValue: Int ->
                    val norm = detectorNormalizeSwitch.isSelected
                    updateDetectorPane(points, newValue, norm)
                }
        detectorNormalizeSwitch.selectedProperty().addListener { observable: ObservableValue<out Boolean>, oldValue: Boolean, newValue: Boolean ->
            val bin = detectorBinningSelector.value
            updateDetectorPane(points, bin, newValue)
        }
        detectorDataExportButton.isDisable = false
    }

    private fun setupInfo(loader: NumassData) {
        val info = loader.meta()
        infoTextBox.text = JSONMetaWriter().writeString(info).replace("\\r", "\r\t").replace("\\n", "\n\t")
    }

    private fun setupSpectrumPane(points: List<NumassPoint>) {
        spectrumPlot.plot.add(spectrumData)

        val lowChannel = channelSlider.lowValue.toInt()
        val highChannel = channelSlider.highValue.toInt()

        if (points.isEmpty()) {
            spectrumData.clear()
        } else {
            spectrumData.fillData(points.stream()
                    .map { point: NumassPoint -> getSpectrumPoint(point, lowChannel, highChannel, dTime) }
                    .collect(Collectors.toList<DataPoint>()))
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

    private fun getSpectrumPoint(point: NumassPoint, lowChannel: Int, upChannel: Int, dTime: Double): DataPoint {
        val u = point.voltage
        return MapPoint(arrayOf(XYAdapter.X_VALUE_KEY, XYAdapter.Y_VALUE_KEY, XYAdapter.Y_ERROR_KEY), u,
                NumassDataUtils.countRateWithDeadTime(point, lowChannel, upChannel, dTime),
                NumassDataUtils.countRateWithDeadTimeErr(point, lowChannel, upChannel, dTime))
    }

    /**
     * update detector pane with new data
     */
    private fun updateDetectorPane(points: List<NumassPoint>, binning: Int, normalize: Boolean) {
        val detectorPlotFrame: FXPlotFrame
        if (detectorPlot.plot == null) {
            val frameMeta = MetaBuilder("frame")
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
            detectorPlotFrame = JFreeChartFrame(frameMeta)
        } else {
            detectorPlotFrame = detectorPlot.plot
        }

        val work = getWorkManager().getWork("viewer.numass.load.detector")

        val plottableConfig = MetaBuilder("plot")
                .setValue("connectionType", "step")
                .setValue("thickness", 2)
                .setValue("showLine", true)
                .setValue("showSymbol", false)
                .setValue("showErrors", false)
                .setValue("JFreeChart.cache", true)
                .build()

        work.maxProgress = points.size.toDouble()
        work.progress = 0.0

        runAsync {
            points.map { point ->
                val seriesName = String.format("%d: %.2f", points.indexOf(point), point.voltage)
                val datum = PlottableData.plot(seriesName, XYAdapter("chanel", "count"), point.getData(binning, normalize))
                datum.configure(plottableConfig)
                work.increaseProgress(1.0)
                datum;
            }
        } ui {
            detectorPlotFrame.setAll(it)
        }

        detectorPlot.plot = detectorPlotFrame
        work.setProgressToMax()
    }

    private fun onSpectrumExportClick(event: ActionEvent) {
        if (points.isNotEmpty()) {
            val fileChooser = FileChooser()
            fileChooser.title = "Choose text export destination"
            fileChooser.initialFileName = data!!.name + "_spectrum.onComplete"
            val destination = fileChooser.showSaveDialog(spectrumPlotPane.scene.window)
            if (destination != null) {
                val names = arrayOf("Uset", "Uread", "Length", "Total", "Window", "CR", "CRerr", "Timestamp")
                val loChannel = channelSlider.lowValue.toInt()
                val upChannel = channelSlider.highValue.toInt()
                val dTime = dTime
                val spectrumDataSet = ListTable.Builder(*names)

                for (point in points) {
                    spectrumDataSet.row(
                            point.voltage,
                            point.voltage,
                            point.length,
                            point.totalCount,
                            point.getCountInWindow(loChannel, upChannel),
                            NumassDataUtils.countRateWithDeadTime(point, loChannel, upChannel, dTime),
                            NumassDataUtils.countRateWithDeadTimeErr(point, loChannel, upChannel, dTime),
                            point.startTime
                    )
                }

                try {
                    val comment = String.format("Numass data viewer spectrum data export for %s%n"
                            + "Window: (%d, %d)%n"
                            + "Dead time per event: %g%n",
                            data!!.name, loChannel, upChannel, dTime)

                    ColumnedDataWriter
                            .writeTable(destination, spectrumDataSet.build(), comment, false)
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
            val detectorData = PlotDataUtils.collectXYDataFromPlot(detectorPlot.plot as XYPlotFrame, true)
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
