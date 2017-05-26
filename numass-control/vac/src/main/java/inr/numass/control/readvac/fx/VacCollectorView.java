/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.control.readvac.fx;

import hep.dataforge.control.connections.Roles;
import hep.dataforge.control.devices.Device;
import hep.dataforge.control.measurements.Measurement;
import hep.dataforge.control.measurements.MeasurementListener;
import hep.dataforge.control.measurements.Sensor;
import hep.dataforge.exceptions.ControlException;
import hep.dataforge.exceptions.MeasurementException;
import hep.dataforge.fx.fragments.FragmentWindow;
import hep.dataforge.fx.fragments.LogFragment;
import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaBuilder;
import hep.dataforge.plots.data.TimePlottable;
import hep.dataforge.plots.data.TimePlottableGroup;
import hep.dataforge.plots.fx.FXPlotFrame;
import hep.dataforge.plots.fx.PlotContainer;
import hep.dataforge.plots.jfreechart.JFreeChartFrame;
import hep.dataforge.tables.DataPoint;
import hep.dataforge.values.Value;
import inr.numass.control.DeviceViewConnection;
import inr.numass.control.readvac.VacCollectorDevice;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.controlsfx.control.Notifications;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * A view controller for Vac collector
 *
 * @author <a href="mailto:altavir@gmail.com">Alexander Nozik</a>
 */
public class VacCollectorView extends DeviceViewConnection<VacCollectorDevice> implements Initializable, MeasurementListener {

    public static VacCollectorView build() {
        try {
            FXMLLoader loader = new FXMLLoader(VacCollectorView.class.getResource("/fxml/VacCollector.fxml"));
            loader.load();
            return loader.getController();
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    private final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final String[] intervalNames = {"1 sec", "5 sec", "10 sec", "30 sec", "1 min"};
    private final int[] intervals = {1000, 5000, 10000, 30000, 60000};
    private final List<VacuumeterView> views = new ArrayList<>();
    private TimePlottableGroup plottables;

    @FXML
    private BorderPane root;
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


    @Override
    public Node getFXNode() {
        return root;
    }

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
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

        LogFragment logFragment = new LogFragment();
        new FragmentWindow(logFragment).bindTo(logButton);
        logFragment.addRootLogHandler();
    }

    @Override
    public void evaluateDeviceException(Device device, String message, Throwable exception) {
        Notifications.create().darkStyle().hideAfter(Duration.seconds(2d)).text(message).showError();
    }

    @Override
    public void open(VacCollectorDevice device) throws Exception {
        super.open(device);
        device.getSensors().stream().map((sensor) -> {
            VacuumeterView view;
            if (sensor.hasState("power")) {
                view = new PoweredVacuumeterView();
            } else {
                view = new VacuumeterView();
            }
            sensor.connect(view, Roles.VIEW_ROLE, Roles.DEVICE_LISTENER_ROLE, Roles.MEASUREMENT_CONSUMER_ROLE);
            return view;
        }).forEach(views::add);
        setupView();
    }

    @Override
    public void notifyDeviceStateChanged(Device device, String name, Value state) {

    }

    @Override
    public void onMeasurementFailed(Measurement measurement, Throwable exception) {
        LoggerFactory.getLogger(getClass()).debug("Exception during measurement: {}", exception.getMessage());
    }

    @Override
    public void onMeasurementResult(Measurement measurement, Object res, Instant time) {
        if (plottables != null) {
            plottables.put(DataPoint.class.cast(res));
        }
        Platform.runLater(() -> timeLabel.setText(TIME_FORMAT.format(LocalDateTime.ofInstant(time, ZoneOffset.UTC))));
    }

    private void setupView() {
        vacBoxHolder.getChildren().clear();
        plottables = new TimePlottableGroup();
        views.forEach((view) -> {
            vacBoxHolder.getChildren().add(view.getComponent());
            TimePlottable plot = new TimePlottable(view.getTitle(),
                    view.getDevice().getName());
            plot.configure(view.getDevice().meta());
            plottables.add(plot);
        });
        plottables.setValue("thickness", 3);
        plottables.setMaxAge(java.time.Duration.ofHours(3));
        PlotContainer.anchorTo(plotHolder).setPlot(setupPlot(plottables));
    }

    private FXPlotFrame setupPlot(TimePlottableGroup plottables) {
        Meta plotConfig = new MetaBuilder("plotFrame")
                .setNode(new MetaBuilder("yAxis")
                        .setValue("type", "log")
                        .setValue("axisTitle", "pressure")
                        .setValue("axisUnits", "mbar")
                )
                .setValue("xAxis.type", "time");
        JFreeChartFrame frame = new JFreeChartFrame(plotConfig);
        frame.addAll(plottables);
        return frame;
    }

    private void startMeasurement() throws ControlException {
        getDevice().startMeasurement();
        startStopButton.setSelected(true);
    }

    private void stopMeasurement() {
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
    private void onStartStopToggle(ActionEvent event) {
        if (startStopButton.isSelected() != getDevice().isMeasuring()) {
            //Starting measurement on non-UI thread
            new Thread(() -> {
                if (startStopButton.isSelected()) {
                    try {
                        startMeasurement();
                    } catch (ControlException ex) {
                        getDevice().getLogger().error("Failed to start measurement", ex);
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
        getDevice().setState("storing", storeButton.isSelected());
//        if (storeButton.isSelected()) {
//            //creating storage on UI thread
//            if (!device.meta().hasMeta("storage")) {
//                getLogger().info("Storage not defined. Starting storage selection dialog");
//                DirectoryChooser chooser = new DirectoryChooser();
//                File storageDir = chooser.showDialog(plotHolder.getScene().getWindow());
//                if (storageDir == null) {
//                    storeButton.setSelected(false);
//                    throw new RuntimeException("User canceled directory selection");
//                }
//                device.getConfig().putNode(new MetaBuilder("storage")
//                        .putValue("path", storageDir.getAbsolutePath()));
//            }
//            Meta storageConfig = device.meta().getMeta("storage");
//            Storage localStorage = StorageManager.buildFrom(device.getContext())
//                    .buildStorage(storageConfig);
//            //Start storage creation on non-UI thread
//            new Thread(() -> {
//                try {
//
//                    PointLoader loader;
//
//                    if (loaderFactory != null) {
//                        loader = loaderFactory.apply(device, localStorage);
//                    } else {
//                        TableFormatBuilder format = new TableFormatBuilder().setType("timestamp", ValueType.TIME);
//                        device.getSensors().forEach((s) -> {
//                            format.setType(s.getName(), ValueType.NUMBER);
//                        });
//
//                        loader = LoaderFactory.buildPointLoder(localStorage, "vactms",
//                                device.meta().getString("storage.shelf", ""), "timestamp", format.build());
//                    }
//                    storageConnection = new LoaderConnection(loader);
//                    device.connect(storageConnection, Roles.STORAGE_ROLE);
//                } catch (Exception ex) {
//                    getLogger().error("Failed to start data storing", ex);
//                    storeButton.setSelected(false);
//                }
//            }).start();
//        } else if (storageConnection != null) {
//            device.disconnect(storageConnection);
//        }
    }


}
