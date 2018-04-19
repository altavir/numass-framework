/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.control.readvac

import hep.dataforge.control.devices.PortSensor.Companion.CONNECTED_STATE
import hep.dataforge.control.devices.Sensor
import inr.numass.control.DeviceDisplayFX
import inr.numass.control.switch
import javafx.application.Platform
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.layout.Priority
import javafx.scene.paint.Color
import javafx.scene.text.FontWeight
import tornadofx.*
import java.text.DecimalFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * @author [Alexander Nozik](mailto:altavir@gmail.com)
 */
open class VacDisplay : DeviceDisplayFX<Sensor>() {

    val statusProperty = SimpleStringProperty("")
    var status: String by statusProperty

    val valueProperty = SimpleStringProperty("---")
    var value: String by valueProperty

    val timeProperty = SimpleObjectProperty<Instant>()
    var time: Instant by timeProperty


    override fun buildView(device: Sensor): View {
        return VacView();
    }

    fun message(message: String) {
        Platform.runLater {
            status = message
        }
    }

    private fun onResult(res: Any, time: Instant) {
        val result = Number::class.java.cast(res).toDouble()
        val resString = FORMAT.format(result)
        Platform.runLater {
            value = resString
            this.time = time
            status = "OK: " + TIME_FORMAT.format(LocalDateTime.ofInstant(time, ZoneOffset.UTC));
        }
    }

//    override fun notifyMetaStateChanged(device: Device, name: String, state: Meta) {
//        super.notifyMetaStateChanged(device, name, state)
//
//        when (name) {
//            Sensor.MEASUREMENT_RESULT_STATE -> {
//                if(state.getBoolean(Sensor.RESULT_SUCCESS)) {
//                    val res by state.value(Sensor.RESULT_VALUE)
//                    val time by state.getTime(Sensor.RESULT_TIMESTAMP)
//                    onResult(res, time)
//                } else{
//                    Platform.runLater {
//                        value = "Err"
//                    }
//                }
//            }
//            Sensor.MEASUREMENT_ERROR_STATE -> {
//                val message by state.getString("message")
//                message(message)
//            }
//        }
//    }
//
//    override fun notifyStateChanged(device: Device, name: String, state: Any?) {
//        super.notifyStateChanged(device, name, state)
//    }




    fun getTitle(): String {
        return device.meta.getString("title", device.name);
    }

    inner class VacView : View("Numass vacuumeter ${getTitle()}") {

        override val root = borderpane {
            minWidth = 90.0
            style {

            }
            top {
                borderpane {
                    center {
                        label(device.name) {
                            style {
                                fontSize = 18.pt
                                fontWeight = FontWeight.BOLD
                            }
                        }
                        style {
                            backgroundColor = multi(Color.LAVENDER)
                        }
                    }
                    right {
                        switch {
                            bindBooleanToState(CONNECTED_STATE, selectedProperty());
                            selectedProperty().addListener { _, _, newValue ->
                                if (!newValue) {
                                    value = "---"
                                }
                            }
                        }
                    }
                }
            }
            center {
                vbox {
                    separator(Orientation.HORIZONTAL)
                    borderpane {
                        left {
                            label {
                                padding = Insets(5.0, 5.0, 5.0, 5.0)
                                prefHeight = 60.0
                                alignment = Pos.CENTER_RIGHT
                                textProperty().bind(valueProperty)
                                device.meta.optValue("color").ifPresent { colorValue -> textFill = Color.valueOf(colorValue.getString()) }
                                style {
                                    fontSize = 24.pt
                                    fontWeight = FontWeight.BOLD
                                }
                            }
                        }
                        right {
                            label {
                                padding = Insets(5.0, 5.0, 5.0, 5.0)
                                prefHeight = 60.0
                                prefWidth = 75.0
                                alignment = Pos.CENTER_LEFT
                                text = device.meta.getString("units", "mbar")
                                style {
                                    fontSize = 24.pt
                                }
                            }
                        }
                    }
                    if (device.stateNames.contains("power")) {
                        separator(Orientation.HORIZONTAL)
                        pane {
                            minHeight = 30.0
                            vgrow = Priority.ALWAYS
                            switch("Power") {
                                alignment = Pos.CENTER
                                bindBooleanToState("power", selectedProperty())
                            }
                        }
                    }
                    separator(Orientation.HORIZONTAL)
                }
            }
            bottom {
                label {
                    padding = Insets(5.0, 5.0, 5.0, 5.0)
                    textProperty().bind(statusProperty)
                }
            }
        }
    }

    companion object {
        private val FORMAT = DecimalFormat("0.###E0")
        private val TIME_FORMAT = DateTimeFormatter.ISO_LOCAL_TIME
    }
}
