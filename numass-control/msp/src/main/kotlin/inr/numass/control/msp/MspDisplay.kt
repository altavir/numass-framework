/* 
 * Copyright 2015 Alexander Nozik.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package inr.numass.control.msp

import hep.dataforge.connections.NamedValueListener
import hep.dataforge.control.devices.PortSensor
import hep.dataforge.control.devices.Sensor
import hep.dataforge.fx.asBooleanProperty
import hep.dataforge.fx.bindWindow
import hep.dataforge.fx.fragments.LogFragment
import hep.dataforge.fx.plots.PlotContainer
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder
import hep.dataforge.plots.PlotFrame
import hep.dataforge.plots.PlotGroup
import hep.dataforge.plots.PlotUtils
import hep.dataforge.plots.data.TimePlot
import hep.dataforge.plots.data.TimePlot.Companion.setMaxItems
import hep.dataforge.plots.data.TimePlot.Companion.setPrefItems
import hep.dataforge.plots.jfreechart.JFreeChartFrame
import hep.dataforge.states.ValueState
import hep.dataforge.values.Value
import inr.numass.control.DeviceDisplayFX
import inr.numass.control.deviceStateIndicator
import inr.numass.control.deviceStateToggle
import inr.numass.control.switch
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.collections.MapChangeListener
import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.scene.Parent
import javafx.scene.control.Alert
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.paint.Paint
import tornadofx.*

/**
 * FXML Controller class

 * @author darksnake
 */
class MspDisplay() : DeviceDisplayFX<MspDevice>(), NamedValueListener {

    private val table = FXCollections.observableHashMap<String, Value>()

    override fun getBoardView(): Parent {
        return VBox().apply {
            this += super.getBoardView()
        }
    }

    override fun buildView(device: MspDevice): View {
        return MspView()
    }

    override fun pushValue(valueName: String, value: Value) {
        table[valueName] = value
    }


    inner class MspView : View("Numass mass-spectrometer measurement") {
        private val plotFrameMeta: Meta = device.meta.getMeta("plotConfig", device.meta)

        private val plotFrame: PlotFrame by lazy {
            val basePlotConfig = MetaBuilder("plotFrame")
                    .setNode(MetaBuilder("yAxis")
                            .setValue("type", "log")
                            .setValue("title", "partial pressure")
                            .setValue("units", "mbar")
                    )
                    .setValue("xAxis.type", "time")


            JFreeChartFrame(basePlotConfig).apply {
                PlotUtils.setXAxis(this, "timestamp", "", "time")
                configure(plotFrameMeta)
            }
        }
        val plottables = PlotGroup.typed<TimePlot>("peakJump").apply {
            setMaxItems(this, 1000)
            setPrefItems(this, 400)

            if (plotFrameMeta.hasMeta("peakJump.peak")) {
                for (peakMeta in plotFrameMeta.getMetaList("peakJump.peak")) {
                    val mass = peakMeta.getString("mass")
                    get(mass) ?: TimePlot(mass, mass).also {
                        it.configureValue("titleBase", peakMeta.getString("title", mass))
                        add(it)
                    }.configure(peakMeta)
                }
            } else {
                showError("No peaks defined in config")
                throw RuntimeException()
            }
        }

//        private val logWindow = FragmentWindow(LogFragment().apply {
//            addLogHandler(device.logger)
//        })

        private val filamentProperty = SimpleObjectProperty<Int>(this, "filament", 1).apply {
            addListener { _, oldValue, newValue ->
                if (newValue != oldValue) {
                    runAsync {
                        device.filament.set(newValue)
                    }
                }
            }
        }

        override val root = borderpane {
            minHeight = 400.0
            minWidth = 600.0
            top {
                toolbar {
                    deviceStateToggle(this@MspDisplay, MspDevice.CONTROLLED_STATE, "Connect")
                    combobox(filamentProperty, listOf(1, 2)) {
                        cellFormat {
                            text = "Filament $it"
                        }
                        disableProperty().bind(booleanStateProperty(PortSensor.CONNECTED_STATE).not())
                    }
                    switch {
                        padding = Insets(5.0, 0.0, 0.0, 0.0)
                        disableProperty().bind(device.controlled.asBooleanProperty().not())
                        device.filamentOn.asBooleanProperty().bindBidirectional(selectedProperty())
                    }
                    deviceStateIndicator(this@MspDisplay, "filamentStatus", false) {
                        when (it.string) {
                            "ON" -> Paint.valueOf("red")
                            "OFF" -> Paint.valueOf("blue")
                            "WARM-UP", "COOL-DOWN" -> Paint.valueOf("yellow")
                            else -> Paint.valueOf("grey")

                        }
                    }

                    togglebutton("Measure") {
                        isSelected = false
                        disableProperty().bind(booleanStateProperty(PortSensor.CONNECTED_STATE).not())
                        device.measuring.asBooleanProperty().bindBidirectional(selectedProperty())
                    }
                    togglebutton("Store") {
                        isSelected = false
                        disableProperty().bind(booleanStateProperty(Sensor.MEASURING_STATE).not())
                        device.states.getState<ValueState>("storing")?.asBooleanProperty()?.bindBidirectional(selectedProperty())
                    }
                    separator(Orientation.VERTICAL)
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
            center = PlotContainer(plotFrame).root
        }

        init {
            table.addListener { change: MapChangeListener.Change<out String, out Value> ->
                if (change.wasAdded()) {
                    val pl = plottables[change.key] as TimePlot?
                    val value = change.valueAdded
                    if (pl != null) {
                        if (value.double > 0) {
                            pl.put(value)
                        } else {
                            pl.put(Value.NULL)
                        }
                        val titleBase = pl.config.getString("titleBase")
                        val title = String.format("%s (%.4g)", titleBase, value.double)
                        pl.configureValue("title", title)
                    }
                }
            }
        }


//        override fun evaluateDeviceException(device: Device, message: String, exception: Throwable) {
//            Platform.runLater {
//                logFragment!!.appendLine("ERROR: " + message)
//                showError(message)
//            }
//        }

        private fun showError(message: String) {
            val alert = Alert(Alert.AlertType.ERROR)
            alert.title = "Error!"
            alert.headerText = null
            alert.contentText = message

            alert.showAndWait()
        }
    }
}
