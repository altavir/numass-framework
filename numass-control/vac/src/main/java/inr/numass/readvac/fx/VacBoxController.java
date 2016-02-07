/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.readvac.fx;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import org.controlsfx.control.StatusBar;

/**
 * FXML Controller class
 *
 * @author Alexander Nozik
 */
public class VacBoxController implements Initializable {

    @FXML
    protected Label deviceNameLabel;
    @FXML
    protected Label valueLabel;
    @FXML
    protected Label unitLabel;
    @FXML
    protected StatusBar status;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
    }    
    
}
