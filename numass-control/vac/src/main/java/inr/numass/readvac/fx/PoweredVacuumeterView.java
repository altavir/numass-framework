/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.readvac.fx;

import hep.dataforge.values.Value;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import org.controlsfx.control.ToggleSwitch;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * @author <a href="mailto:altavir@gmail.com">Alexander Nozik</a>
 */
public class PoweredVacuumeterView extends VacuumeterView {

    @FXML
    private ToggleSwitch powerSwitch;


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
            getDevice().setState("power", Value.of(newValue));
        });
    }

}
