/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.control.readvac

import hep.dataforge.control.Connection
import hep.dataforge.control.connections.Roles
import hep.dataforge.control.devices.Sensor
import hep.dataforge.control.measurements.Measurement
import hep.dataforge.control.measurements.MeasurementListener
import hep.dataforge.fx.fragments.FragmentWindow
import hep.dataforge.fx.fragments.LogFragment
import hep.dataforge.plots.data.TimePlottable
import hep.dataforge.plots.data.TimePlottableGroup
import hep.dataforge.values.Value
import inr.numass.control.DeviceViewConnection
import inr.numass.control.deviceStateToggle
import inr.numass.control.plot
import javafx.collections.FXCollections
import javafx.collections.MapChangeListener
import javafx.geometry.Orientation
import javafx.scene.control.ScrollPane
import javafx.scene.layout.Priority
import tornadofx.*
import java.time.Instant

/**
 * A view controller for Vac collector

 * @author [Alexander Nozik](mailto:altavir@gmail.com)
 */
class VacCollectorViewConnection : DeviceViewConnection<VacCollectorDevice>() {

    private val table = FXCollections.observableHashMap<String, Double>()

    private val sensorConnection = object : MeasurementListener, Connection{
        override fun onMeasurementResult(measurement: Measurement<*>, result: Any, time: Instant?) {
            if(result is Double){
                table.put(measurement.device.name, result);
            }
        }

        override fun onMeasurementFailed(measurement: Measurement<*>?, exception: Throwable?) {

        }
    }

    private val viewList = FXCollections.observableArrayList<VacViewConnection>();

    override fun buildView(device: VacCollectorDevice): View {
        return VacCollectorView();
    }

    override fun open(obj: Any) {
        super.open(obj)
        device.sensors.forEach { sensor ->
            val view = VacViewConnection()
            sensor.connect(view, Roles.VIEW_ROLE, Roles.DEVICE_LISTENER_ROLE)
            sensor.connect(sensorConnection, Roles.MEASUREMENT_LISTENER_ROLE);
            viewList.add(view)
        }
    }

    inner class VacCollectorView : View("Numass vacuum view") {

        private val plottables = TimePlottableGroup().apply {
            viewList.forEach {
                val plot = TimePlottable(it.getTitle(), it.device.name)
                plot.configure(it.device.meta())
                add(plot)
            }
            setValue("thickness", 3)
        }

//        private val logWindow = FragmentWindow(LogFragment().apply {
//            addLogHandler(device.logger)
//        })

        override val root = borderpane {
            top {
                toolbar {
                    deviceStateToggle(this@VacCollectorViewConnection, Sensor.MEASURING_STATE, "Measure")
                    deviceStateToggle(this@VacCollectorViewConnection, "storing", "Store")
                    pane {
                        hgrow = Priority.ALWAYS
                    }
                    separator(Orientation.VERTICAL)
                    togglebutton("Log") {
                        isSelected = false
                        FragmentWindow.build(this){LogFragment().apply {
                            addLogHandler(device.logger)
                        }}
                    }
                }
            }
            plot(plottables) {
                "xAxis.type" to "time"
                node("yAxis") {
                    "type" to "log"
                    "axisTitle" to "presure"
                    "axisUnits" to "mbar"
                }
            }
            right {
                scrollpane {
                    hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
                    vbox {
                        viewList.forEach {
                            add(it.fxNode)
                            separator(Orientation.HORIZONTAL)
                        }
                    }
                }
//                listview(viewList) {
//                    cellFormat {
//                        graphic = it.fxNode
//                    }
//                }
            }
        }


        init {
            table.addListener { change: MapChangeListener.Change<out String, out Double> ->
                if (change.wasAdded()) {
                    val pl = plottables.get(change.key)
                    val value = change.valueAdded
                    if (pl != null) {
                        if (value > 0) {
                            pl.put(Value.of(value))
                        } else {
                            pl.put(Value.NULL)
                        }
                    }
                }
            }
        }
    }
}
