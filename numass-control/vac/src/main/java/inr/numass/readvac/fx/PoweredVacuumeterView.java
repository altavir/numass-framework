/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.readvac.fx;

import hep.dataforge.exceptions.ControlException;
import hep.dataforge.values.Value;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import org.controlsfx.control.ToggleSwitch;

/**
 *
 * @author Alexander Nozik <altavir@gmail.com>
 */
public class PoweredVacuumeterView extends VacuumeterView {

    @FXML
    ToggleSwitch powerSwitch;
    
    
    @Override
    public Node getComponent() {
        if (node == null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/PoweredVacBox.fxml"));
                loader.setController(this);
                this.node = loader.load();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        return node;
    }    

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        unitLabel.setText(getDevice().meta().getString("units", "mbar"));
        deviceNameLabel.setText(getDevice().getName());
        powerSwitch.selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
            try {
                getDevice().command("setPower", Value.of(newValue));
            } catch (ControlException ex) {
                Logger.getLogger(PoweredVacuumeterView.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
    }
    
}
