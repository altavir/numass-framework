/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.control.readvac

import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import org.controlsfx.control.ToggleSwitch
import java.io.IOException
import java.net.URL
import java.util.*

/**
 * @author [Alexander Nozik](mailto:altavir@gmail.com)
 */
class PoweredVacuumeterViewConnection : VacuumeterViewConnection() {

    @FXML
    private val powerSwitch: ToggleSwitch? = null


    val component: Node
        get() {
            if (getNode() == null) {
                try {
                    val loader = FXMLLoader(javaClass.getResource("/fxml/PoweredVacBox.fxml"))
                    loader.setController(this)
                    this.setNode(loader.load<T>())
                } catch (ex: IOException) {
                    throw RuntimeException(ex)
                }

            }
            return getNode()
        }

    fun initialize(location: URL, resources: ResourceBundle) {
        super.initialize(location, resources)
        getUnitLabel().setText(device.meta().getString("units", "mbar"))
        getDeviceNameLabel().setText(device.name)
        bindBooleanToState("power", powerSwitch!!.selectedProperty())
    }
}
