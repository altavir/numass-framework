/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.control.readvac.fx;

import hep.dataforge.control.devices.Device;
import hep.dataforge.control.devices.Sensor;
import hep.dataforge.control.measurements.Measurement;
import hep.dataforge.control.measurements.MeasurementListener;
import inr.numass.control.DeviceViewConnection;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import org.controlsfx.control.ToggleSwitch;

import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

import static hep.dataforge.control.devices.PortSensor.CONNECTED_STATE;

/**
 * @author <a href="mailto:altavir@gmail.com">Alexander Nozik</a>
 */
public class VacuumeterView extends DeviceViewConnection<Sensor<Double>> implements MeasurementListener, Initializable {

    private static final DecimalFormat FORMAT = new DecimalFormat("0.###E0");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ISO_LOCAL_TIME;

    Node node;

    @FXML
    private BorderPane root;
    @FXML
    Label deviceNameLabel;
    @FXML
    Label unitLabel;
    @FXML
    private Label valueLabel;
    @FXML
    private Label status;
    @FXML
    private ToggleSwitch disableButton;

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
            disableButton.setSelected(getDevice().optBooleanState(CONNECTED_STATE).orElse(false));
            disableButton.selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
                getDevice().setState(CONNECTED_STATE, newValue);
                if (!newValue) {
                    valueLabel.setText("---");
                }
            });
        });
    }

    @Override
    public void onMeasurementFailed(Measurement measurement, Throwable exception) {
        Platform.runLater(() -> {
            valueLabel.setText("Err");
//            setStatus("Error: " + exception.getMessage());
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
    public void onMeasurementStarted(Measurement<?> measurement) {
        getDevice().meta().optValue("color").ifPresent(colorValue -> valueLabel.setTextFill(Color.valueOf(colorValue.stringValue())));
    }

    @Override
    public void onMeasurementResult(Measurement measurement, Object res, Instant time) {
        Double result = Double.class.cast(res);
        String resString = FORMAT.format(result);
        Platform.runLater(() -> {
            valueLabel.setText(resString);
            setStatus("OK: " + TIME_FORMAT.format(LocalDateTime.ofInstant(time, ZoneOffset.UTC)));
        });
    }


    String getTitle() {
        return getDevice().meta().getString("title", getDevice().getName());
    }

    @Override
    public Node getFXNode() {
        return root;
    }
}