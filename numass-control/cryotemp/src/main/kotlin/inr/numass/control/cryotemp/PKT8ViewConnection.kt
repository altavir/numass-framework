package inr.numass.control.cryotemp

import hep.dataforge.control.devices.Sensor
import hep.dataforge.control.measurements.Measurement
import hep.dataforge.control.measurements.MeasurementListener
import hep.dataforge.fx.fragments.FXFragment
import hep.dataforge.fx.fragments.FragmentWindow
import hep.dataforge.fx.fragments.LogFragment
import hep.dataforge.meta.Meta
import hep.dataforge.plots.PlotUtils
import hep.dataforge.plots.data.TimePlottable
import hep.dataforge.plots.data.TimePlottableGroup
import hep.dataforge.plots.fx.FXPlotFrame
import hep.dataforge.plots.fx.PlotContainer
import hep.dataforge.plots.jfreechart.JFreeChartFrame
import inr.numass.control.DeviceViewConnection
import javafx.application.Platform
import javafx.beans.binding.ListBinding
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.collections.MapChangeListener
import javafx.collections.ObservableList
import javafx.geometry.Orientation
import javafx.scene.Parent
import javafx.scene.control.ToggleButton
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.text.Font
import tornadofx.*
import java.time.Instant

/**
 * Created by darksnake on 30-May-17.
 */
class PKT8ViewConnection : DeviceViewConnection<PKT8Device>(), MeasurementListener {

    override fun buildView(device: PKT8Device): View {
        return CryoView()
    }

    internal val table = FXCollections.observableHashMap<String, PKT8Result>()
    val lastUpdateProperty = SimpleObjectProperty<String>("NEVER")


    override fun getBoardView(): Parent {
        return VBox().apply {
            this += super.getBoardView()
        }
    }

    override fun onMeasurementFailed(measurement: Measurement<*>, exception: Throwable) {

    }

    override fun onMeasurementResult(measurement: Measurement<*>, result: Any, time: Instant) {
        if (result is PKT8Result) {
            Platform.runLater {
                lastUpdateProperty.set(time.toString())
                table.put(result.channel, result);
            }
        }
    }

    inner class CryoView : View() {
        private var plotButton: ToggleButton by singleAssign()
        private var logButton: ToggleButton by singleAssign()

//        private val logWindow = FragmentWindow(LogFragment().apply {
//            addLogHandler(device.logger)
//        })

        // need those to have strong references to listeners
        private val plotView = CryoPlotView();
//        private val plotWindow = FragmentWindow(FXFragment.buildFromNode(plotView.title) { plotView.root })

        override val root = borderpane {
            top {
                toolbar {
                    togglebutton("Measure") {
                        isSelected = false
                        bindBooleanToState(Sensor.MEASURING_STATE, selectedProperty())
                    }
                    togglebutton("Store") {
                        isSelected = false
                        bindBooleanToState("storing", selectedProperty())
                    }
                    separator(Orientation.VERTICAL)
                    pane {
                        hgrow = Priority.ALWAYS
                    }
                    separator(Orientation.VERTICAL)

                    plotButton = togglebutton("Plot") {
                        isSelected = false
                        FragmentWindow.build(this) { FXFragment.buildFromNode(plotView.title) { plotView.root } }
                    }

                    logButton = togglebutton("Log") {
                        isSelected = false
                        FragmentWindow.build(this) {
                            LogFragment().apply {
                                addLogHandler(device.logger)
                            }
                        }
                    }
                }
            }
            center {
                tableview<PKT8Result> {
                    items = object : ListBinding<PKT8Result>() {
                        init {
                            bind(table)
                        }

                        override fun computeValue(): ObservableList<PKT8Result> {
                            return FXCollections.observableArrayList(table.values).apply {
                                sortBy { it.channel }
                            }
                        }

                    }
                    column("Sensor", PKT8Result::channel);
                    column("Resistance", PKT8Result::rawValue).cellFormat {
                        text = String.format("%.2f", it)
                    }
                    column("Temperature", PKT8Result::temperature).cellFormat {
                        text = String.format("%.2f", it)
                    }
                }
            }
            bottom {
                toolbar {
                    label("Last update: ")
                    label(lastUpdateProperty) {
                        font = Font.font("System Bold")
                    }
                }
            }
        }
    }

    inner class CryoPlotView : View("PKT8 temperature plot") {
        val plotFrameMeta: Meta = device.meta.getMetaOrEmpty("plotConfig")

        val plotFrame: FXPlotFrame by lazy {
            JFreeChartFrame(plotFrameMeta).apply {
                PlotUtils.setXAxis(this, "timestamp", null, "time")
            }
        }

        var rawDataButton: ToggleButton by singleAssign()

        val plottables: TimePlottableGroup = TimePlottableGroup()

        override val root: Parent = borderpane {
            prefWidth = 800.0
            prefHeight = 600.0
            PlotContainer.centerIn(this).plot = plotFrame
            top {
                toolbar {
                    rawDataButton = togglebutton("Raw data") {
                        isSelected = false
                        action {
                            clearPlot()
                        }
                    }
                    button("Reset") {
                        action {
                            clearPlot()
                        }
                    }
                }
            }
        }

        init {
            val channels = device.chanels

            //plot config from device configuration
            //Do not use view config here, it is applyed separately
            channels.stream()
                    .filter { channel -> !plottables.has(channel.name) }
                    .forEachOrdered { channel ->
                        //plot config from device configuration
                        val plottable = TimePlottable(channel.name)
                        plottable.configure(channel.meta())
                        plottables.add(plottable)
                        plotFrame.add(plottable)
                    }
            if (device.meta().hasMeta("plotConfig")) {
                plottables.configure(device.meta().getMeta("plotConfig"))
                plottables.setMaxItems(1000)
                plottables.setPrefItems(400)
            }
            table.addListener(MapChangeListener { change ->
                if (change.wasAdded()) {
                    change.valueAdded.apply {
                        if (rawDataButton.isSelected()) {
                            plottables.put(this.channel, this.rawValue)
                        } else {
                            plottables.put(this.channel, this.temperature)
                        }
                    }
                }
            })
        }

        fun clearPlot() {
            plottables.forEach {
                it.clear()
            }
        }
    }
}

