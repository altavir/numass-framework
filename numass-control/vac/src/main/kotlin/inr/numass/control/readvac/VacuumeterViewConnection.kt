/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.control.readvac

import hep.dataforge.control.devices.Device
import hep.dataforge.control.devices.PortSensor.CONNECTED_STATE
import hep.dataforge.control.devices.Sensor
import hep.dataforge.control.measurements.Measurement
import hep.dataforge.control.measurements.MeasurementListener
import inr.numass.control.DeviceViewConnection
import javafx.application.Platform
import javafx.beans.property.SimpleStringProperty
import javafx.scene.control.Label
import javafx.scene.layout.BorderPane
import javafx.scene.paint.Color
import org.controlsfx.control.ToggleSwitch
import tornadofx.*
import java.text.DecimalFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * @author [Alexander Nozik](mailto:altavir@gmail.com)
 */
open class VacuumeterViewConnection : DeviceViewConnection<Sensor<Double>>(), MeasurementListener {

    val statusProperty = SimpleStringProperty()
    var status: String by statusProperty

    val valueProperty = SimpleStringProperty()
    var value: String by valueProperty


    override fun buildView(): View {
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
        val result = Double::class.java.cast(res)
        val resString = FORMAT.format(result)
        Platform.runLater {
            value = resString
            status = "OK: " + TIME_FORMAT.format(LocalDateTime.ofInstant(time, ZoneOffset.UTC));
        }
    }

    inner class VacView : View("Numass vacuumeter ${device.meta().getString("title", device.name)}") {

        override val root: BorderPane by fxml()

        val deviceNameLabel: Label by fxid()
        val unitLabel: Label by fxid()
        val valueLabel: Label by fxid()
        val status: Label by fxid()
        val disableButton: ToggleSwitch by fxid()

        init {
            status.textProperty().bind(statusProperty)
            valueLabel.textProperty().bind(valueProperty)
            unitLabel.text = device.meta().getString("units", "mbar")
            deviceNameLabel.text = device.name
            bindBooleanToState(CONNECTED_STATE, disableButton.selectedProperty());
            disableButton.selectedProperty().addListener { _, _, newValue ->
                if (!newValue) {
                    valueLabel.text = "---"
                }
            }
            device.meta().optValue("color").ifPresent { colorValue -> valueLabel.textFill = Color.valueOf(colorValue.stringValue()) }
        }
    }

    companion object {
        private val FORMAT = DecimalFormat("0.###E0")
        private val TIME_FORMAT = DateTimeFormatter.ISO_LOCAL_TIME
    }
}
