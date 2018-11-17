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
package inr.numass.control.magnet.fx

import hep.dataforge.exceptions.PortException
import inr.numass.control.DeviceDisplayFX
import inr.numass.control.magnet.LambdaMagnet
import javafx.application.Platform
import javafx.beans.value.ObservableDoubleValue
import javafx.beans.value.ObservableValue
import javafx.scene.control.*
import javafx.scene.layout.AnchorPane
import javafx.scene.paint.Color

/**
 * FXML Controller class
 *
 * @author Alexander Nozik
 */
class MagnetDisplay : DeviceDisplayFX<LambdaMagnet>() {
    override fun buildView(device: LambdaMagnet): MagnetControllerComponent? {
        return MagnetControllerComponent(device)
    }


    inner class MagnetControllerComponent(val device: LambdaMagnet) : Fragment() {

        override val root: AnchorPane by fxml("/fxml/SingleMagnet.fxml")

        var showConfirmation = true


        val current: ObservableDoubleValue = device.current.asDoubleProperty()
        val voltage: ObservableDoubleValue = device.voltage.asDoubleProperty()


        val labelI: Label by fxid()
        val labelU: Label by fxid()
        val targetIField: TextField by fxid()
        val magnetName: Label by fxid()
        val monitorButton: ToggleButton by fxid()
        val statusLabel: Label by fxid()
        val setButton: ToggleButton by fxid()
        val magnetSpeedField: TextField by fxid()


        init {
            targetIField.textProperty().addListener { _: ObservableValue<out String>, oldValue: String, newValue: String ->
                if (!newValue.matches("\\d*(\\.)?\\d*".toRegex())) {
                    targetIField.text = oldValue
                }
            }

            magnetSpeedField.textProperty().addListener { _: ObservableValue<out String>, oldValue: String, newValue: String ->
                if (!newValue.matches("\\d*(\\.)?\\d*".toRegex())) {
                    magnetSpeedField.text = oldValue
                }
            }

            magnetName.text = device.name
            magnetSpeedField.text = device.speed.toString()

            current.onChange {
                runLater {
                    labelI.text = String.format("%.2f",it)
                }
            }

            voltage.onChange {
                runLater {
                    labelU.text = String.format("%.4f",it)
                }
            }

            device.states.getState<ValueState>("status")?.onChange{
                runLater {
                    this.statusLabel.text = it.string
                }
            }

            device.output.onChange {
                Platform.runLater {
                    if (it.boolean) {
                        this.statusLabel.textFill = Color.BLUE
                    } else {
                        this.statusLabel.textFill = Color.BLACK
                    }
                }
            }

            device.updating.onChange {
                val updateTaskRunning = it.boolean
                runLater {
                    this.setButton.isSelected = updateTaskRunning
                    targetIField.isDisable = updateTaskRunning
                }
            }

            device.monitoring.onChange {
                runLater {
                    monitorButton.isScaleShape = it.boolean
                }
            }

            setButton.selectedProperty().onChange {
                try {
                    setOutputOn(it)
                } catch (ex: PortException) {
                    displayError(this.device.name, null, ex)
                }
            }

            monitorButton.selectedProperty().onChange {
                if (device.monitoring.booleanValue != it) {
                    if (it) {
                        device.monitoring.set(true)
                    } else {
                        device.monitoring.set(false)
                        this.labelU.text = "----"
                    }
                }
            }

            magnetSpeedField.text = device.speed.toString()
        }


        /**
         * Show confirmation dialog
         */
        private fun confirm(): Boolean {
            return if (showConfirmation) {
                val alert = Alert(Alert.AlertType.WARNING)
                alert.contentText = "Изменение токов в сверхпроводящих магнитах можно производить только при выключенном напряжении на спектрометре." + "\nВы уверены что напряжение выключено?"
                alert.headerText = "Проверьте напряжение на спектрометре!"
                alert.height = 150.0
                alert.title = "Внимание!"
                alert.buttonTypes.clear()
                alert.buttonTypes.addAll(ButtonType.YES, ButtonType.CANCEL)

                alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.YES
            } else {
                true
            }
        }

        private fun setOutputOn(outputOn: Boolean) {
            if (outputOn && confirm()) {
                val speed = java.lang.Double.parseDouble(magnetSpeedField.text)
                if (speed > 0 && speed <= LambdaMagnet.MAX_SPEED) {
                    device.speed = speed
                    magnetSpeedField.isDisable = true
                    device.target.set(targetIField.text.toDouble())
                    device.output.set(true)
                    device.updating.set(true)
                } else {
                    val alert = Alert(Alert.AlertType.ERROR)
                    alert.contentText = null
                    alert.headerText = "Недопустимое значение скорости изменения тока"
                    alert.title = "Ошибка!"
                    alert.show()
                    setButton.isSelected = false
                    magnetSpeedField.text = device.speed.toString()
                }
            } else {
                device.updating.set(false)
                targetIField.isDisable = false
                magnetSpeedField.isDisable = false
            }
        }

        private fun displayError(name: String, errorMessage: String?, throwable: Throwable) {
            Platform.runLater {
                this.statusLabel.text = "ERROR"
                this.statusLabel.textFill = Color.RED
            }
            device.logger.error("ERROR: {}", errorMessage, throwable)
            //        MagnetStateListener.super.error(address, errorMessage, throwable); //To change body of generated methods, choose Tools | Templates.
        }

        fun displayState(state: String) {
            Platform.runLater { this.statusLabel.text = state }
        }
    }

}

