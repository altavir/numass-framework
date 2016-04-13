/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.readvac.fx;

import hep.dataforge.names.Named;
import hep.dataforge.control.connections.DeviceViewController;
import hep.dataforge.control.devices.Device;
import hep.dataforge.control.measurements.Measurement;
import hep.dataforge.control.measurements.MeasurementListener;
import hep.dataforge.meta.Annotated;
import hep.dataforge.meta.Meta;
import hep.dataforge.values.Value;
import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;

/**
 *
 * @author Alexander Nozik <altavir@gmail.com>
 */
public class VacuumeterView extends DeviceViewController implements MeasurementListener<Double>, Initializable, Named, Annotated {

    private static final DecimalFormat FORMAT = new DecimalFormat("0.###E0");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ISO_LOCAL_TIME;

    protected Node node;

    @FXML
    Label deviceNameLabel;

    @FXML
    Label unitLabel;

    @FXML
    Label valueLabel;
    
    @FXML
    Label status;

    @Override
    @SuppressWarnings("unchecked")
    public void accept(Device device, String measurementName, Measurement measurement) {
        measurement.addListener(this);
        if (device.meta().hasValue("color")) {
            valueLabel.setTextFill(Color.valueOf(device.meta().getString("color")));
        }
    }

    @Override
    public void evaluateDeviceException(Device device, String message, Throwable exception) {
        Platform.runLater(() -> setStatus("ERROR: " + message));
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
        Platform.runLater(() -> {
            unitLabel.setText(getDevice().meta().getString("units", "mbar"));
            deviceNameLabel.setText(getDevice().getName());
        });
    }

    @Override
    public void notifyDeviceStateChanged(Device device, String name, Value state) {

    }

    @Override
    public void onMeasurementFailed(Measurement measurement, Throwable exception) {
        Platform.runLater(() -> {
            valueLabel.setText("Err");
            setStatus("Error: " + exception.getMessage());
        });
    }

    private void setStatus(String text) {
        status.setText(text);
    }

    @Override
    public void onMeasurementProgress(Measurement measurement, String message) {
        Platform.runLater(() -> status.setText(message));
    }

    @Override
    public void onMeasurementProgress(Measurement measurement, double progress) {
//        Platform.runLater(() -> status.setProgress(progress));
    }

    @Override
    public void onMeasurementResult(Measurement<Double> measurement, Double result, Instant time) {
        String resString = FORMAT.format(result);
        Platform.runLater(() -> {
            valueLabel.setText(resString);
            setStatus("OK: " + TIME_FORMAT.format(LocalDateTime.ofInstant(time, ZoneId.systemDefault())));
        });
    }

    @Override
    public String getName() {
        return getDevice().getName();
    }

    @Override
    public Meta meta() {
        return getDevice().meta();
    }

    public String getTitle() {
        return meta().getString("title", getName());
    }
}
