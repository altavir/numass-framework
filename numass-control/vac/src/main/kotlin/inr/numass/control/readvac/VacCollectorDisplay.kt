/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.control.readvac

import hep.dataforge.control.connections.Roles
import hep.dataforge.control.devices.Device
import hep.dataforge.control.devices.DeviceListener
import hep.dataforge.control.devices.Sensor
import hep.dataforge.fx.bindWindow
import hep.dataforge.fx.fragments.LogFragment
import hep.dataforge.meta.Meta
import hep.dataforge.plots.PlotGroup
import hep.dataforge.plots.data.TimePlot
import hep.dataforge.values.Value
import inr.numass.control.DeviceDisplayFX
import inr.numass.control.deviceStateToggle
import inr.numass.control.plot
import javafx.collections.FXCollections
import javafx.collections.MapChangeListener
import javafx.geometry.Orientation
import javafx.scene.control.ScrollPane
import javafx.scene.layout.Priority
import tornadofx.*

/**
 * A view controller for Vac collector

 * @author [Alexander Nozik](mailto:altavir@gmail.com)
 */
class VacCollectorDisplay : DeviceDisplayFX<VacCollectorDevice>() {

    private val table = FXCollections.observableHashMap<String, Double>()

    private val sensorConnection = object : DeviceListener {

        override fun notifyStateChanged(device: Device, name: String, state: Any) {
            if (name == Sensor.MEASUREMENT_RESULT_STATE) {
                table[device.name] = (state as Meta).getDouble(Sensor.RESULT_VALUE)
            }
        }
    }

    private val viewList = FXCollections.observableArrayList<VacDisplay>();

    override fun buildView(device: VacCollectorDevice): UIComponent {
        return VacCollectorView();
    }

    override fun open(obj: Any) {
        super.open(obj)
        device.sensors.forEach { sensor ->
            val view = VacDisplay()
            sensor.connect(view, Roles.VIEW_ROLE, Roles.DEVICE_LISTENER_ROLE)
            sensor.connect(sensorConnection, Roles.DEVICE_LISTENER_ROLE)
            viewList.add(view)
        }
    }

    inner class VacCollectorView : Fragment("Numass vacuum view") {

        private val plottables = PlotGroup.typed<TimePlot>("vac").apply {
            viewList.forEach {
                val plot = TimePlot(it.getTitle(), it.device.name)
                plot.configure(it.device.meta)
                add(plot)
            }
            configureValue("thickness", 3)
        }

//        private val logWindow = FragmentWindow(LogFragment().apply {
//            addLogHandler(device.logger)
//        })

        override val root = borderpane {
            top {
                toolbar {
                    deviceStateToggle(this@VacCollectorDisplay, Sensor.MEASURING_STATE, "Measure")
                    deviceStateToggle(this@VacCollectorDisplay, "storing", "Store")
                    pane {
                        hgrow = Priority.ALWAYS
                    }
                    separator(Orientation.VERTICAL)
                    togglebutton("Log") {
                        isSelected = false
                        LogFragment().apply {
                            addLogHandler(device.logger)
                            bindWindow(this@togglebutton, selectedProperty())
                        }
                    }
                }
            }
            plot(plottables) {
                "xAxis.type" to "time"
                node("yAxis") {
                    "type" to "log"
                    "title" to "presure"
                    "units" to "mbar"
                }
            }
            right {
                scrollpane {
                    hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
                    vbox {
                        viewList.forEach {
                            it.view?.let {
                                add(it)
                                separator(Orientation.HORIZONTAL)
                            }
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
                    val pl = plottables[change.key]
                    val value = change.valueAdded
                    (pl as? TimePlot)?.let {
                        if (value > 0) {
                            it.put(Value.of(value))
                        } else {
                            it.put(Value.NULL)
                        }
                    }
                }
            }
        }
    }
}
