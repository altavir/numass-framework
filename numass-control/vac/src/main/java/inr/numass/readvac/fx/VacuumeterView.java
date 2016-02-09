/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.readvac.fx;

import hep.dataforge.content.Named;
import hep.dataforge.control.connections.DeviceViewController;
import hep.dataforge.control.devices.Device;
import hep.dataforge.control.measurements.Measurement;
import hep.dataforge.control.measurements.MeasurementListener;
import hep.dataforge.meta.Annotated;
import hep.dataforge.meta.Meta;
import hep.dataforge.values.Value;
import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import org.controlsfx.control.StatusBar;

/**
 *
 * @author Alexander Nozik <altavir@gmail.com>
 */
public class VacuumeterView extends DeviceViewController implements MeasurementListener<Double>, Initializable, Named, Annotated {

    protected Node node;

    @FXML
    Label deviceNameLabel;

    @FXML
    StatusBar status;

    @FXML
    Label unitLabel;

    @FXML
    Label valueLabel;

    @Override
    public void accept(Device device, String measurementName, Measurement measurement) {
        measurement.addListener(this);
    }

    @Override
    public void evaluateDeviceException(Device device, String message, Throwable exception) {
        //show dialog or tooltip
    }

    public Node getComponent() {
        if (node == null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/VacBox.fxml"));
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
    }

    @Override
    public void notifyDeviceStateChanged(Device device, String name, Value state) {

    }

    @Override
    public void onMeasurementFailed(Measurement measurement, Throwable exception) {
        valueLabel.setText("Err");
    }

    @Override
    public void onMeasurementProgress(Measurement measurement, String message) {
        status.setText(message);
    }

    @Override
    public void onMeasurementProgress(Measurement measurement, double progress) {
        status.setProgress(progress);
    }

    @Override
    public void onMeasurementResult(Measurement<Double> measurement, Double result, Instant time) {
        valueLabel.setText(Double.toString(result));
    }

    @Override
    public String getName(){
        return getDevice().getName();
    }

    @Override
    public Meta meta() {
        return getDevice().meta();
    }
    
    public String getTitle(){
        return meta().getString("title", getName());
    }    
}
