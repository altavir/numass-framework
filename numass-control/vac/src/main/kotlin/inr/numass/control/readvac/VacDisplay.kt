/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.control.readvac

import hep.dataforge.control.devices.Device
import hep.dataforge.control.devices.Sensor
import hep.dataforge.control.measurements.Measurement
import hep.dataforge.control.measurements.MeasurementListener
import inr.numass.control.DeviceDisplay
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
open class VacDisplay : DeviceDisplay<Sensor>(), MeasurementListener {

    val statusProperty = SimpleStringProperty("")
    var status: String by statusProperty

    val valueProperty = SimpleStringProperty("---")
    var value: String by valueProperty

    val timeProperty = SimpleObjectProperty<Instant>()
    var time: Instant by timeProperty


    override fun buildView(device: Sensor): View {
        return VacView();
    }

    override fun evaluateDeviceException(device: Device, message: String, exception: Throwable) {
        if (!message.isEmpty()) {
            Platform.runLater {
                status = "ERROR: " + message
            }
        }
    }

    override fun onMeasurementFailed(measurement: Measurement<*>, exception: Throwable) {
        Platform.runLater {
            value = "Err"
        }
    }

    override fun onMeasurementProgress(measurement: Measurement<*>, message: String) {
        Platform.runLater {
            status = message
        }
    }

    override fun onMeasurementResult(measurement: Measurement<*>, res: Any, time: Instant) {
        val result = Number::class.java.cast(res).toDouble()
        val resString = FORMAT.format(result)
        Platform.runLater {
            value = resString
            this.time = time
            status = "OK: " + TIME_FORMAT.format(LocalDateTime.ofInstant(time, ZoneOffset.UTC));
        }
    }

    fun getTitle(): String{
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
                        label(device.name){
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
                                device.meta.optValue("color").ifPresent { colorValue -> textFill = Color.valueOf(colorValue.stringValue()) }
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
                    if (device.hasState("power")) {
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
