/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.readvac.fx;

import hep.dataforge.control.connections.Roles;
import hep.dataforge.control.devices.Device;
import hep.dataforge.control.devices.DeviceListener;
import hep.dataforge.control.measurements.Measurement;
import hep.dataforge.control.measurements.MeasurementListener;
import hep.dataforge.control.measurements.Sensor;
import hep.dataforge.data.DataPoint;
import hep.dataforge.exceptions.ControlException;
import hep.dataforge.exceptions.MeasurementException;
import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaBuilder;
import hep.dataforge.plots.PlotFrame;
import hep.dataforge.plots.data.DynamicPlottable;
import hep.dataforge.plots.data.DynamicPlottableSet;
import hep.dataforge.plots.fx.PlotContainer;
import hep.dataforge.plots.jfreechart.JFreeChartFrame;
import hep.dataforge.values.Value;
import inr.numass.readvac.devices.VacCollectorDevice;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.controlsfx.control.Notifications;
import org.slf4j.LoggerFactory;

/**
 * FXML Controller class
 *
 * @author Alexander Nozik <altavir@gmail.com>
 */
public class VacCollectorController implements Initializable, DeviceListener, MeasurementListener<DataPoint> {

    private final String[] intervalNames = {"1 sec", "5 sec", "10 sec", "30 sec", "1 min"};
    private final int[] intervals = {1000, 5000, 10000, 30000, 60000};

    private VacCollectorDevice device;
    private final List<VacuumeterView> views = new ArrayList<>();
    private PlotContainer plotContainer;
    private DynamicPlottableSet plottables;

    @FXML
    private AnchorPane plotHolder;
    @FXML
    private VBox vacBoxHolder;
    @FXML
    private Label timeLabel;
    @FXML
    private ChoiceBox<String> intervalSelector;
    @FXML
    private ToggleButton startStopButton;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        plotContainer = PlotContainer.anchorTo(plotHolder);
        intervalSelector.setItems(FXCollections.observableArrayList(intervalNames));
        intervalSelector.getSelectionModel().select(1);
        intervalSelector.getSelectionModel().selectedIndexProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
            if (getDevice() != null) {
                try {
                    getDevice().setDelay(intervals[newValue.intValue()]);
                } catch (MeasurementException ex) {
                    evaluateDeviceException(getDevice(), "Failed to restart measurement", null);
                }
            }
        });
    }

    @Override
    public void evaluateDeviceException(Device device, String message, Throwable exception) {
        Notifications.create().darkStyle().hideAfter(Duration.seconds(2d)).text(message).showError();
    }

    public VacCollectorDevice getDevice() {
        return device;
    }

    @Override
    public void notifyDeviceStateChanged(Device device, String name, Value state) {

    }

    @Override
    public void onMeasurementFailed(Measurement measurement, Throwable exception) {
        LoggerFactory.getLogger(getClass()).debug("Exception during measurement: {}", exception.getMessage());
    }

    @Override
    public void onMeasurementResult(Measurement<DataPoint> measurement, DataPoint result, Instant time) {
        if (plottables != null) {
            plottables.put(result);
        }
        Platform.runLater(() -> timeLabel.setText(time.toString()));
    }

    private void setupView() {
        vacBoxHolder.getChildren().clear();
        plottables = new DynamicPlottableSet();
        views.stream().forEach((controller) -> {
            vacBoxHolder.getChildren().add(controller.getComponent());
            DynamicPlottable plot = new DynamicPlottable(controller.getTitle(),
                    controller.getName());
            plot.configure(controller.meta());
            plottables.addPlottable(plot);
        });
        plotContainer.setPlot(setupPlot(plottables));
    }

    private PlotFrame setupPlot(DynamicPlottableSet plottables) {
        Meta plotConfig = new MetaBuilder("plotFrame")
                .setNode(new MetaBuilder("yAxis")
                        .setValue("type", "log")
                        .setValue("axisTitle", "pressure")
                        .setValue("axisUnits", "mbar")
                )
                .setValue("xAxis.type", "time");
        JFreeChartFrame frame = new JFreeChartFrame("pressure", plotConfig);
        frame.addAll(plottables);
        return frame;
    }

    public void setDevice(VacCollectorDevice device) {
        this.device = device;
        device.getSensors().stream().map((sensor) -> {
            VacuumeterView controller;
            if (sensor.hasState("power")) {
                controller = new PoweredVacuumeterView();
            } else {
                controller = new VacuumeterView();
            }
            sensor.connect(controller, Roles.DEVICE_LISTENER_ROLE, Roles.MEASUREMENT_CONSUMER_ROLE);
            return controller;
        }).forEach((controller) -> {
            views.add(controller);
        });
        setupView();
    }

    public void startMeasurement() throws ControlException {
        getDevice().startMeasurement().addListener(this);
        startStopButton.setSelected(true);
    }

    public void stopMeasurement() {
        try {
            getDevice().stopMeasurement(false);
            for (Sensor sensor : getDevice().getSensors()) {
                sensor.stopMeasurement(false);
            }            
        } catch (ControlException ex) {
            throw new RuntimeException(ex);
        }
    }

    @FXML
    private void onStartStopToggle(ActionEvent event) throws ControlException {
        if (startStopButton.isSelected() != getDevice().isMeasuring()) {
            if (startStopButton.isSelected()) {
                startMeasurement();
            } else {
                stopMeasurement();
            }
        }
    }

}
