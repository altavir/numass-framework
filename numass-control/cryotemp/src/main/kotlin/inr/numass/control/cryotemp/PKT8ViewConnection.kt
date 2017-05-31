package inr.numass.control.cryotemp

import hep.dataforge.control.devices.Sensor
import hep.dataforge.control.measurements.Measurement
import hep.dataforge.control.measurements.MeasurementListener
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
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.transformation.SortedList
import javafx.geometry.Orientation
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.ToggleButton
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.text.Font
import tornadofx.*
import java.time.Duration
import java.time.Instant

/**
 * Created by darksnake on 30-May-17.
 */
class PKT8ViewConnection : DeviceViewConnection<PKT8Device>(), MeasurementListener {
    private val view by lazy { CryoView() }
    internal val table = SortedList(FXCollections.observableArrayList<PKT8Result>()) { r1, r2 ->
        r1.channel.compareTo(r2.channel)
    }

    val lastUpdateProperty = SimpleObjectProperty<String>("NEVER")


    override fun getBoardView(): Parent {
        return VBox().apply {
            this += super.getBoardView()
        }
    }

    override fun getFXNode(): Node {
        return view.root;
    }

    override fun onMeasurementFailed(measurement: Measurement<*>, exception: Throwable) {
        throw exception;
    }

    override fun onMeasurementResult(measurement: Measurement<*>, result: Any, time: Instant) {
        if (result is PKT8Result) {
            Platform.runLater {
                lastUpdateProperty.set(time.toString())
                val item = table.find { it.channel == result.channel };
                if (item == null) {
                    table.add(result);
                } else {
                    table[table.indexOf(item)] = result
                }
            }
        }
    }

    inner class CryoView() : View() {
        override val root = borderpane {
            top {
                toolbar {
                    togglebutton("Measure") {
                        bindBooleanToState(Sensor.MEASURING_STATE, selectedProperty())
                    }
                    togglebutton("Store") {
                        bindBooleanToState("storing", selectedProperty())
                    }
                    separator(Orientation.VERTICAL)
                    pane {
                        hgrow = Priority.ALWAYS
                    }
                    separator(Orientation.VERTICAL)
                    togglebutton("Plot") {
                        FragmentWindow(CryoPlotView().root).bindTo(this)
                    }
                    togglebutton("Log") {
                        FragmentWindow(LogFragment().apply {
                            addLogHandler(device.logger)
                        }).bindTo(this)
                    }
                }
            }
            center {
                tableview(table) {
                    column("Sensor", PKT8Result::channel);
                    column("Resistance", PKT8Result::rawValue).cellFormat {
                        text = String.format("%.2f", it)
                    }
                    column("Resistance", PKT8Result::temperature).cellFormat {
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

    inner class CryoPlotView : View() {
        val plotFrameMeta: Meta = device.meta.getMetaOrEmpty("plotConfig")

        val plotFrame: FXPlotFrame by lazy {
            JFreeChartFrame(plotFrameMeta).apply {
                PlotUtils.setXAxis(this, "timestamp", null, "time")
            }
        }

        var rawDataButton: ToggleButton by singleAssign()

        val plottables: TimePlottableGroup by lazy {
            TimePlottableGroup().apply {
                setMaxAge(Duration.parse(plotFrameMeta.getString("maxAge", "PT2H")))
                table.addListener(ListChangeListener { change ->
                    while (change.next()) {
                        change.addedSubList.forEach {
                            if (rawDataButton.isSelected()) {
                                plottables.put(it.channel, it.rawValue)
                            } else {
                                plottables.put(it.channel, it.temperature)
                            }
                        }
                    }
                })
            }
        }

        override val root: Parent = borderpane {
            PlotContainer.centerIn(this).plot = plotFrame
            bottom {
                rawDataButton = togglebutton("Raw data") {
                    action {
                        plottables.forEach {
                            it.clear()
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
                plottables.applyConfig(device.meta().getMeta("plotConfig"))
                plottables.setMaxItems(1000)
                plottables.setPrefItems(400)
            }
        }
    }
}

