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
import inr.numass.control.magnet.LambdaMagnet
import inr.numass.control.magnet.MagnetStateListener
import inr.numass.control.magnet.MagnetStatus
import javafx.application.Platform
import javafx.beans.value.ObservableValue
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.fxml.Initializable
import javafx.scene.control.*
import javafx.scene.layout.AnchorPane
import javafx.scene.paint.Color
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URL
import java.util.*

/**
 * FXML Controller class
 *
 * @author Alexander Nozik
 */
class MagnetControllerComponent : AnchorPane(), Initializable, MagnetStateListener {

    private var lambdaMagnet: LambdaMagnet? = null
    /**
     * @return the logger
     */
    var logger: Logger? = null
        private set

    private var showConfirmation = true

    @FXML
    private val labelI: Label? = null
    @FXML
    private val labelU: Label? = null
    @FXML
    private val targetIField: TextField? = null
    @FXML
    private val magnetName: Label? = null
    @FXML
    private val monitorButton: ToggleButton? = null
    @FXML
    private val statusLabel: Label? = null
    @FXML
    private val setButton: ToggleButton? = null
    @FXML
    private val magnetSpeedField: TextField? = null

    private val targetI: Double
        get() = java.lang.Double.parseDouble(targetIField!!.text)

    //    public MagnetControllerComponent(LambdaMagnet lambdaMagnet) {
    //        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/SingleMagnet.fxml"));
    //
    //        loader.setRoot(this);
    //        loader.setController(this);
    //
    //        try {
    //            loader.load();
    //        } catch (IOException ex) {
    //            throw new RuntimeException(ex);
    //        }
    //        setLambdaMagnet(lambdaMagnet);
    //    }
    /**
     * Initializes the controller class.
     *
     * @param url
     * @param rb
     */
    override fun initialize(url: URL, rb: ResourceBundle) {

        targetIField!!.textProperty().addListener { observable: ObservableValue<out String>, oldValue: String, newValue: String ->
            if (!newValue.matches("\\d*(\\.)?\\d*".toRegex())) {
                targetIField.text = oldValue
            }
        }

        magnetSpeedField!!.textProperty().addListener { observable: ObservableValue<out String>, oldValue: String, newValue: String ->
            if (!newValue.matches("\\d*(\\.)?\\d*".toRegex())) {
                magnetSpeedField.text = oldValue
            }
        }
    }

    fun setShowConfirmation(showConfirmation: Boolean) {
        this.showConfirmation = showConfirmation
    }

    @FXML
    private fun onOutToggle(event: ActionEvent) {
        try {
            setOutput(setButton!!.isSelected)
        } catch (ex: PortException) {
            error(this.lambdaMagnet!!.name, null, ex)
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
                    setButton!!.isSelected = false
                }
            } else {
                startCurrentChange()
            }
        } else {
            getLambdaMagnet().stopUpdateTask()
            targetIField!!.isDisable = false
            magnetSpeedField!!.isDisable = false
        }
    }

    @Throws(PortException::class)
    private fun startCurrentChange() {
        val speed = java.lang.Double.parseDouble(magnetSpeedField!!.text)
        if (speed > 0 && speed <= 7) {
            lambdaMagnet!!.speed = speed
            magnetSpeedField.isDisable = true
            getLambdaMagnet().setOutputMode(true)
            getLambdaMagnet().startUpdateTask(targetI)
        } else {
            val alert = Alert(Alert.AlertType.ERROR)
            alert.contentText = null
            alert.headerText = "Недопустимое значение скорости изменения тока"
            alert.title = "Ошибка!"
            alert.show()
            setButton!!.isSelected = false
            magnetSpeedField.text = java.lang.Double.toString(lambdaMagnet!!.speed)
        }

    }

    @FXML
    private fun onMonitorToggle(event: ActionEvent) {
        if (monitorButton!!.isSelected) {
            getLambdaMagnet().startMonitorTask()
        } else {
            getLambdaMagnet().stopMonitorTask()
            this.labelU!!.text = "----"
        }
    }

    override fun error(name: String, errorMessage: String?, throwable: Throwable) {
        Platform.runLater {
            this.statusLabel!!.text = "ERROR"
            this.statusLabel.textFill = Color.RED
        }
        this.logger!!.error("ERROR: {}", errorMessage, throwable)
        //        MagnetStateListener.super.error(address, errorMessage, throwable); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * @return the lambdaMagnet
     */
    fun getLambdaMagnet(): LambdaMagnet {
        if (lambdaMagnet == null) {
            throw RuntimeException("Magnet controller not defined")
        }
        return lambdaMagnet
    }

    /**
     * @param lambdaMagnet the lambdaMagnet to set
     */
    private fun setLambdaMagnet(lambdaMagnet: LambdaMagnet) {
        this.lambdaMagnet = lambdaMagnet
        logger = LoggerFactory.getLogger("lambda." + lambdaMagnet.name)
        lambdaMagnet.listener = this
        magnetName!!.text = lambdaMagnet.name

        magnetSpeedField!!.text = java.lang.Double.toString(this.lambdaMagnet!!.speed)

    }

    override fun acceptStatus(name: String, state: MagnetStatus) {
        Platform.runLater {
            this.labelI!!.text = java.lang.Double.toString(state.measuredCurrent)
            this.labelU!!.text = java.lang.Double.toString(state.measuredVoltage)
            outputModeChanged(name, state.isOutputOn)

            logger!!.info(String.format("%s (%s): Im = %f, Um = %f, Is = %f, Us = %f;",
                    name,
                    state.isOutputOn,
                    state.measuredCurrent,
                    state.measuredVoltage,
                    state.setCurrent,
                    state.setVoltage
            ))

        }
    }

    override fun acceptNextI(name: String, nextI: Double) {
        logger!!.debug("{}: nextI = {};", name, nextI)
    }

    override fun acceptMeasuredI(name: String, measuredI: Double) {
        logger!!.debug("{}: measuredI = {};", name, measuredI)
        Platform.runLater { this.labelI!!.text = java.lang.Double.toString(measuredI) }
    }

    override fun outputModeChanged(name: String, out: Boolean) {
        Platform.runLater {
            if (out) {
                this.statusLabel!!.text = "OK"
                this.statusLabel.textFill = Color.BLUE
            } else {
                this.statusLabel!!.text = "OFF"
                this.statusLabel.textFill = Color.BLACK
            }
        }
    }

    override fun updateTaskStateChanged(name: String, updateTaskRunning: Boolean) {
        this.setButton!!.isSelected = updateTaskRunning
        targetIField!!.isDisable = updateTaskRunning
    }

    override fun monitorTaskStateChanged(name: String, monitorTaskRunning: Boolean) {
        this.monitorButton!!.isScaleShape = monitorTaskRunning
    }

    //    /**
    //     * @param logger the logger to set
    //     */
    //    public void setLogger(PrintStream logger) {
    //        this.logger = logger;
    //    }
    override fun displayState(state: String) {
        Platform.runLater { this.statusLabel!!.text = state }
    }

    companion object {

        fun build(lambdaMagnet: LambdaMagnet): MagnetControllerComponent {
            val component = MagnetControllerComponent()
            val loader = FXMLLoader(lambdaMagnet.javaClass.getResource("/fxml/SingleMagnet.fxml"))

            loader.setRoot(component)
            loader.setController(component)

            try {
                loader.load<Any>()
            } catch (ex: Exception) {
                LoggerFactory.getLogger("FX").error("Error during fxml initialization", ex)
                throw Error(ex)
            }

            component.setLambdaMagnet(lambdaMagnet)
            return component
        }
    }
}
