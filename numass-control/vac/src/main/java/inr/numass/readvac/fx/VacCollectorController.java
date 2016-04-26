/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.readvac.fx;

import hep.dataforge.control.connections.LoaderConnection;
import hep.dataforge.control.connections.Roles;
import hep.dataforge.control.devices.Device;
import hep.dataforge.control.devices.DeviceListener;
import hep.dataforge.control.measurements.Measurement;
import hep.dataforge.control.measurements.MeasurementListener;
import hep.dataforge.control.measurements.Sensor;
import hep.dataforge.tables.DataPoint;
import hep.dataforge.exceptions.ControlException;
import hep.dataforge.exceptions.MeasurementException;
import hep.dataforge.fx.ConsoleFragment;
import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaBuilder;
import hep.dataforge.plots.PlotFrame;
import hep.dataforge.plots.data.DynamicPlottable;
import hep.dataforge.plots.data.DynamicPlottableSet;
import hep.dataforge.plots.fx.PlotContainer;
import hep.dataforge.plots.jfreechart.JFreeChartFrame;
import hep.dataforge.tables.TableFormatBuilder;
import hep.dataforge.storage.api.PointLoader;
import hep.dataforge.storage.api.Storage;
import hep.dataforge.storage.commons.LoaderFactory;
import hep.dataforge.storage.commons.StorageManager;
import hep.dataforge.values.Value;
import hep.dataforge.values.ValueType;
import inr.numass.readvac.devices.VacCollectorDevice;
import java.io.File;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.BiFunction;
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
import javafx.stage.DirectoryChooser;
import javafx.util.Duration;
import org.controlsfx.control.Notifications;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FXML Controller class
 *
 * @author Alexander Nozik <altavir@gmail.com>
 */
public class VacCollectorController implements Initializable, DeviceListener, MeasurementListener<DataPoint> {

    private Logger logger;

    private final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final String[] intervalNames = {"1 sec", "5 sec", "10 sec", "30 sec", "1 min"};
    private final int[] intervals = {1000, 5000, 10000, 30000, 60000};

    private LoaderConnection storageConnection;

    private VacCollectorDevice device;
    private final List<VacuumeterView> views = new ArrayList<>();
    private PlotContainer plotContainer;
    private DynamicPlottableSet plottables;
    private BiFunction<VacCollectorDevice, Storage, PointLoader> loaderFactory;

    ConsoleFragment consoleWindow;

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
    @FXML
    private ToggleButton storeButton;
    @FXML
    private ToggleButton logButton;

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

        consoleWindow = new ConsoleFragment();
        consoleWindow.bindTo(logButton);
        consoleWindow.hookStd();
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
        Platform.runLater(() -> timeLabel.setText(TIME_FORMAT.format(LocalDateTime.ofInstant(time, ZoneId.systemDefault()))));
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
        plottables.setEachConfigValue("thickness", 3);
        //TODO make history length edittable
        plottables.setMaxAge(3 * 60 * 60 * 1000);
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
            if (sensor.meta().getBoolean("powerButton", false)) {
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

    public void setLoaderFactory(BiFunction<VacCollectorDevice, Storage, PointLoader> loaderFactory) {
        this.loaderFactory = loaderFactory;
    }

    @FXML
    private void onStartStopToggle(ActionEvent event) {
        if (startStopButton.isSelected() != getDevice().isMeasuring()) {
            //Starting measurement on non-UI thread
            new Thread(() -> {
                if (startStopButton.isSelected()) {
                    try {
                        startMeasurement();
                    } catch (ControlException ex) {
                        getLogger().error("Failed to start measurement", ex);
                        startStopButton.setSelected(false);
                    }
                } else {
                    stopMeasurement();
                }
            }).start();
        }
    }

    @FXML
    private void onStoreToggle(ActionEvent event) {
        if (storeButton.isSelected()) {
            //creating storage on UI thread
            if (!device.meta().hasNode("storage")) {
                getLogger().info("Storage not defined. Starting storage selection dialog");
                DirectoryChooser chooser = new DirectoryChooser();
                File storageDir = chooser.showDialog(plotHolder.getScene().getWindow());
                if (storageDir == null) {
                    storeButton.setSelected(false);
                    throw new RuntimeException("User canceled directory selection");
                }
                device.getConfig().putNode(new MetaBuilder("storage")
                        .putValue("path", storageDir.getAbsolutePath()));
            }
            Meta storageConfig = device.meta().getNode("storage");
            Storage localStorage = StorageManager.buildFrom(device.getContext())
                    .buildStorage(storageConfig);
            //Start storage creation on non-UI thread
            new Thread(() -> {
                try {

                    PointLoader loader;

                    if (loaderFactory != null) {
                        loader = loaderFactory.apply(device, localStorage);
                    } else {
                        TableFormatBuilder format = new TableFormatBuilder().setType("timestamp", ValueType.TIME);
                        device.getSensors().stream().forEach((s) -> {
                            format.setType(s.getName(), ValueType.NUMBER);
                        });

                        loader = LoaderFactory.buildPointLoder(localStorage, "vactms",
                                device.meta().getString("storage.shelf", ""), "timestamp", format.build());
                    }
                    storageConnection = new LoaderConnection(loader);
                    device.connect(storageConnection, Roles.STORAGE_ROLE);
                } catch (Exception ex) {
                    getLogger().error("Failed to start data storing", ex);
                    storeButton.setSelected(false);
                }
            }).start();
        } else if (storageConnection != null) {
            device.disconnect(storageConnection);
        }
    }

    /**
     * @return the logger
     */
    public Logger getLogger() {
        if (logger == null) {
            logger = LoggerFactory.getLogger("ValCollector");
        }
        return logger;
    }

    /**
     * @param logger the logger to set
     */
    public void setLogger(Logger logger) {
        this.logger = logger;
    }
}
