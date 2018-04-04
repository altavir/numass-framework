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
import javafx.beans.value.ObservableValue
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.*
import javafx.scene.layout.AnchorPane
import javafx.scene.paint.Color
import tornadofx.*
import java.net.URL
import java.util.*

/**
 * FXML Controller class
 *
 * @author Alexander Nozik
 */
class MagnetDisplay : DeviceDisplayFX<LambdaMagnet>() {
    override fun buildView(device: LambdaMagnet): MagnetControllerComponent? {
        return MagnetControllerComponent(device)
    }

    val current by lazy {  valueBinding(device.voltage)}

    val voltage by lazy {  valueBinding(device.current)}

    var target by device.target.doubleDelegate

    var output by device.output.booleanDelegate

    var monitoring by device.monitoring.booleanDelegate

    var updating by device.updating.booleanDelegate


    inner class MagnetControllerComponent(val device: LambdaMagnet) : Fragment(), Initializable {

        override val root: AnchorPane by fxml("/fxml/SingleMagnet.fxml")

        private var showConfirmation = true

        val labelI: Label by fxml()
        val labelU: Label by fxml()
        val targetIField: TextField by fxml()
        val magnetName: Label by fxml()
        val monitorButton: ToggleButton by fxml()
        val statusLabel: Label by fxml()
        val setButton: ToggleButton by fxml()
        val magnetSpeedField: TextField by fxml()

        /**
         * Initializes the controller class.
         *
         * @param url
         * @param rb
         */
        override fun initialize(url: URL, rb: ResourceBundle) {

            targetIField.textProperty().addListener { observable: ObservableValue<out String>, oldValue: String, newValue: String ->
                if (!newValue.matches("\\d*(\\.)?\\d*".toRegex())) {
                    targetIField.text = oldValue
                }
            }

            magnetSpeedField.textProperty().addListener { observable: ObservableValue<out String>, oldValue: String, newValue: String ->
                if (!newValue.matches("\\d*(\\.)?\\d*".toRegex())) {
                    magnetSpeedField.text = oldValue
                }
            }

            magnetName.text = device.name
            magnetSpeedField.text = device.speed.toString()

            current.onChange {
                runLater {
                    labelI.text = it?.stringValue()
                }
            }

            voltage.onChange {
                runLater {
                    labelU.text = it?.stringValue()
                }
            }

            valueBinding(device.output).onChange{
                Platform.runLater {
                    if (it?.booleanValue() == true) {
                        this.statusLabel.text = "OK"
                        this.statusLabel.textFill = Color.BLUE
                    } else {
                        this.statusLabel.text = "OFF"
                        this.statusLabel.textFill = Color.BLACK
                    }
                }
            }
        }

        fun setShowConfirmation(showConfirmation: Boolean) {
            this.showConfirmation = showConfirmation
        }

        @FXML
        private fun onOutToggle(event: ActionEvent) {
            try {
                setOutput(setButton.isSelected)
            } catch (ex: PortException) {
                displayError(this.device.name, null, ex)
            }

        }

        @Throws(PortException::class)
        private fun setOutput(outputOn: Boolean) {
            if (outputOn) {
                if (showConfirmation) {
                    val alert = Alert(Alert.AlertType.WARNING)
                    alert.contentText = "Изменение токов в сверхпроводящих магнитах можно производить только при выключенном напряжении на спектрометре." + "\nВы уверены что напряжение выключено?"
                    alert.headerText = "Проверьте напряжение на спектрометре!"
                    alert.height = 150.0
                    alert.title = "Внимание!"
                    alert.buttonTypes.clear()
                    alert.buttonTypes.addAll(ButtonType.YES, ButtonType.CANCEL)

                    if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.YES) {
                        startCurrentChange()
                    } else {
                        setButton.isSelected = false
                    }
                } else {
                    startCurrentChange()
                }
            } else {
                device.stopUpdateTask()
                targetIField.isDisable = false
                magnetSpeedField.isDisable = false
            }
        }

        @Throws(PortException::class)
        private fun startCurrentChange() {
            val speed = java.lang.Double.parseDouble(magnetSpeedField.text)
            if (speed > 0 && speed <= 7) {
                device.speed = speed
                magnetSpeedField.isDisable = true
                target = targetIField.text.toDouble()
                output = true
                updating = true
            } else {
                val alert = Alert(Alert.AlertType.ERROR)
                alert.contentText = null
                alert.headerText = "Недопустимое значение скорости изменения тока"
                alert.title = "Ошибка!"
                alert.show()
                setButton.isSelected = false
                magnetSpeedField.text = java.lang.Double.toString(device.speed)
            }

        }

        @FXML
        private fun onMonitorToggle(event: ActionEvent) {
            device
            if (monitorButton.isSelected) {
                monitoring = true
            } else {
                monitoring = false
                this.labelU.text = "----"
            }
        }

        fun displayError(name: String, errorMessage: String?, throwable: Throwable) {
            Platform.runLater {
                this.statusLabel.text = "ERROR"
                this.statusLabel.textFill = Color.RED
            }
            device.logger.error("ERROR: {}", errorMessage, throwable)
            //        MagnetStateListener.super.error(address, errorMessage, throwable); //To change body of generated methods, choose Tools | Templates.
        }

//    /**
//     * @param lambdaMagnet the device to set
//     */
//    private fun setLambdaMagnet(lambdaMagnet: LambdaMagnet) {
//        this.device = lambdaMagnet
//        lambdaMagnet.listener = this
//        magnetName.text = lambdaMagnet.name
//
//        magnetSpeedField.text = java.lang.Double.toString(this.device.speed)
//
//    }

        override fun updateTaskStateChanged(name: String, updateTaskRunning: Boolean) {
            this.setButton.isSelected = updateTaskRunning
            targetIField.isDisable = updateTaskRunning
        }

        override fun monitorTaskStateChanged(name: String, monitorTaskRunning: Boolean) {
            this.monitorButton.isScaleShape = monitorTaskRunning
        }

        //    /**
        //     * @param logger the logger to set
        //     */
        //    public void setLogger(PrintStream logger) {
        //        this.logger = logger;
        //    }
        override fun displayState(state: String) {
            Platform.runLater { this.statusLabel.text = state }
        }

        companion object {

//        fun build(lambdaMagnet: LambdaMagnet): MagnetControllerComponent {
//            val component = MagnetControllerComponent()
//            val loader = FXMLLoader(lambdaMagnet.javaClass.getResource("/fxml/SingleMagnet.fxml"))
//
//            loader.setRoot(component)
//            loader.setController(component)
//
//            try {
//                loader.load<Any>()
//            } catch (ex: Exception) {
//                LoggerFactory.getLogger("FX").error("Error during fxml initialization", ex)
//                throw Error(ex)
//            }
//
//            component.setLambdaMagnet(lambdaMagnet)
//            return component
//        }
        }
    }

}

