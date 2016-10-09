/* 
 * Copyright 2015 Alexander Nozik.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package inr.numass.control.msp.fx;

import hep.dataforge.context.Context;
import hep.dataforge.context.GlobalContext;
import hep.dataforge.control.connections.Roles;
import hep.dataforge.control.connections.StorageConnection;
import hep.dataforge.exceptions.ControlException;
import hep.dataforge.exceptions.PortException;
import hep.dataforge.exceptions.StorageException;
import hep.dataforge.fx.fragments.ConsoleFragment;
import hep.dataforge.fx.fragments.FragmentWindow;
import hep.dataforge.io.MetaFileReader;
import hep.dataforge.meta.ConfigChangeListener;
import hep.dataforge.meta.Configuration;
import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaBuilder;
import hep.dataforge.plots.data.TimePlottable;
import hep.dataforge.plots.data.TimePlottableGroup;
import hep.dataforge.plots.fx.PlotContainer;
import hep.dataforge.plots.jfreechart.JFreeChartFrame;
import hep.dataforge.storage.api.Storage;
import hep.dataforge.storage.commons.StorageManager;
import hep.dataforge.tables.MapPoint;
import hep.dataforge.values.Value;
import inr.numass.client.NumassClient;
import inr.numass.control.msp.MspDevice;
import inr.numass.control.msp.MspListener;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.DragEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.stage.DirectoryChooser;
import javafx.util.StringConverter;
import org.controlsfx.control.ToggleSwitch;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * FXML Controller class
 *
 * @author darksnake
 */
public class MspViewController implements Initializable, MspListener {

    public static final String MSP_DEVICE_TYPE = "msp";

    public static final String DEFAULT_CONFIG_LOCATION = "msp-config.xml";
    private final TimePlottableGroup plottables = new TimePlottableGroup();
    private final String mspName = "msp";
    private MspDevice device;
    private Configuration viewConfig;
    private JFreeChartFrame plot;
    private ConsoleFragment logArea;
    private StorageConnection connection;
    @FXML
    private Slider autoRangeSlider;
    @FXML
    private ToggleSwitch fillamentButton;
    @FXML
    private Circle fillamentIndicator;
    @FXML
    private ToggleButton plotButton;
    @FXML
    private AnchorPane plotPane;
    private final ConfigChangeListener viewConfigObserver = new ConfigChangeListener() {

        @Override
        public void notifyElementChanged(String name, List<? extends Meta> oldItem, List<? extends Meta> newItem) {
            updatePlot();
        }

        @Override
        public void notifyValueChanged(String name, Value oldItem, Value newItem) {
            updatePlot();
        }

    };
    @FXML
    private ToggleButton consoleButton;
    @FXML
    private ComboBox<Integer> fillamentSelector;
    @FXML
    private ToggleButton storeButton;

    /**
     * Initializes the controller class.
     *
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        logArea = new ConsoleFragment();
        new FragmentWindow(logArea).bindTo(consoleButton);
        fillamentSelector.setItems(FXCollections.observableArrayList(1, 2));
        fillamentSelector.setConverter(new StringConverter<Integer>() {
            @Override
            public String toString(Integer object) {
                return "Fillament " + object;
            }

            @Override
            public Integer fromString(String string) {
                return Integer.parseInt(string.substring(9));
            }
        });

        fillamentSelector.getSelectionModel().select(0);
        fillamentButton.selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
            try {
                fillamentSelector.setDisable(newValue);
                getDevice().setFileamentOn(newValue);
            } catch (PortException ex) {
                device.getLogger().error("Failed to toggle fillaments");
            }
        });
    }

    public Configuration getViewConfig() {
        if (viewConfig == null) {
            viewConfig = new Configuration(getDevice().meta().getMeta("peakJump"));
            viewConfig.addObserver(viewConfigObserver);
            LoggerFactory.getLogger(getClass()).warn("Could not find view configuration. Using default view configuration instead.");
        }
        return viewConfig;
    }

    public void setViewConfig(Meta viewConfig) {
        this.viewConfig = new Configuration(viewConfig);
        this.viewConfig.addObserver(viewConfigObserver);
    }

    private MspDevice getDevice() {
        if (this.device == null) {
            showError("Device configuration not found. Using default configuration.");
            Meta defaultDeviceConfig;
            try {
                defaultDeviceConfig = MetaFileReader
                        .read(new File(getClass().getResource("/config/msp-config.xml").toURI()));
            } catch (IOException | URISyntaxException | ParseException ex) {
                throw new Error(ex);
            }
            setDeviceConfig(GlobalContext.instance(), defaultDeviceConfig);
        }
        return device;
    }

    public void setDeviceConfig(Context context, Meta config) {
        Meta mspConfig = null;
        if (config.hasMeta("device")) {
            for (Meta d : config.getMetaList("device")) {
                if (d.getString("type", "unknown").equals(MSP_DEVICE_TYPE)
                        && d.getString("name", "msp").equals(this.mspName)) {
                    mspConfig = d;
                }
            }
        } else if (config.hasMeta("peakJump")) {
            mspConfig = config;
        }

        if (mspConfig != null) {
            this.device = new MspDevice();
            device.setName(mspName);
            device.setContext(context);
            device.setMeta(mspConfig);

            try {
                getDevice().setListener(this);
                getDevice().init();
//                getDevice().startMeasurement("peakJump");
            } catch (ControlException ex) {
                showError(String.format("Can't connect to %s:%d. The port is either busy or not the MKS mass-spectrometer port",
                        device.meta().getString("connection.ip", "127.0.0.1"),
                        device.meta().getInt("connection.port", 10014)));
                throw new RuntimeException("Can't connect to device");
            }
        } else {
            showError("Can't find device description in given confgiuration");
            throw new RuntimeException();
        }

        if (config.hasMeta("plots.msp")) {
            setViewConfig(config.getMeta("plots.msp"));
        }

        updatePlot();
    }

    public void setDeviceConfig(Context context, File cfgFile) {
        try {
            Meta deviceConfig = MetaFileReader.instance().read(context, cfgFile, null);
            setDeviceConfig(context, deviceConfig);
        } catch (IOException | ParseException ex) {
            showError("Can't load configuration file");
        }
    }

    public void initPlot() {
        Meta plotConfig = new MetaBuilder("plotFrame")
                .setNode(new MetaBuilder("yAxis")
                        .setValue("type", "log")
                        .setValue("axisTitle", "partial pressure")
                        .setValue("axisUnits", "mbar")
                )
                .setValue("xAxis.type", "time");

        this.plot = new JFreeChartFrame(plotConfig);
        PlotContainer container = PlotContainer.anchorTo(plotPane);
        container.setPlot(plot);
        updatePlot();
//        this.plot = DynamicPlot.attachToFX(plotPane, new AnnotationBuilder("plot-config").putValue("logY", true).build());
//        plot.setAutoRange(30 * 60);
    }

    public void updatePlot() {
        if (plot == null) {
            initPlot();
        }
        Meta config = getViewConfig();
        if (config.hasMeta("plotFrame")) {
            this.plot.configure(config.getMeta("plotFrame"));
        }
        if (config.hasMeta("peakJump.line")) {
            for (Meta an : config.getMetaList("peakJump.line")) {
                String mass = an.getString("mass");

                if (!this.plottables.hasPlottable(mass)) {
                    TimePlottable newPlottable = new TimePlottable(mass, mass);
                    newPlottable.configure(an);
                    this.plottables.addPlottable(newPlottable);
                    plot.add(newPlottable);
                } else {
                    plottables.getPlottable(mass).configure(an);
                }
            }
        } else {
            showError("No peaks defined in config");
            throw new RuntimeException();
        }
    }

    @Override
    public void acceptScan(Map<Integer, Double> measurement) {
        MapPoint.Builder point = new MapPoint.Builder();
        for (Map.Entry<Integer, Double> entry : measurement.entrySet()) {
            Double val = entry.getValue();
            if (val <= 0) {
                val = Double.NaN;
            }
            point.putValue(Integer.toString(entry.getKey()), val);
        }
        plottables.put(point.build());
    }

    @Override
    public void acceptMessage(String message) {
        Platform.runLater(() -> {
            logArea.appendLine("RECIEVE: " + message);
        });
    }

    @Override
    public void acceptRequest(String message) {
        Platform.runLater(() -> {
            logArea.appendLine("SEND: " + message);
        });
    }

    @Override
    public void error(String errorMessage, Throwable error) {
        Platform.runLater(() -> {
            logArea.appendLine("ERROR: " + errorMessage);
            showError(errorMessage);
        });

    }

    @FXML
    private void onAutoRangeChange(DragEvent event) {
        plottables.setMaxAge(Duration.ofMinutes((long) this.autoRangeSlider.getValue()));
    }

    @FXML
    private void onPlotToggle(ActionEvent event) throws ControlException {
        if (plotButton.isSelected()) {
            getDevice().startMeasurement("peakJump");
        } else {
            getDevice().stopMeasurement(false);
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error!");
        alert.setHeaderText(null);
        alert.setContentText(message);

        alert.showAndWait();
    }

    private void showInfo(String message) {

    }

    public void shutdown() throws IOException, PortException, ControlException {
        getDevice().shutdown();
    }

    @Override
    public void acceptFillamentStateChange(String fillamentState) {
        Platform.runLater(() -> {
            switch (fillamentState) {
                case "ON":
                    this.fillamentIndicator.setFill(Paint.valueOf("red"));
                    break;
                case "OFF":
                    this.fillamentIndicator.setFill(Paint.valueOf("blue"));
                    break;
                case "WARM-UP":
                case "COOL-DOWN":
                    this.fillamentIndicator.setFill(Paint.valueOf("yellow"));
                    break;

            }
        });
    }

    @FXML
    private void onStoreButtonClick(ActionEvent event) {
        if (storeButton.isSelected()) {

            if (!device.meta().hasMeta("storage")) {
                device.getLogger().info("Storage not defined. Starting storage selection dialog");
                DirectoryChooser chooser = new DirectoryChooser();
                File storageDir = chooser.showDialog(this.plotPane.getScene().getWindow());
                if (storageDir == null) {
                    storeButton.setSelected(false);
                    throw new RuntimeException("User canceled directory selection");
                }
                device.getConfig().putNode(new MetaBuilder("storage")
                        .putValue("path", storageDir.getAbsolutePath()));
            }
            Meta storageConfig = device.meta().getMeta("storage");
            Storage localStorage = StorageManager.buildFrom(device.getContext())
                    .buildStorage(storageConfig);

            String runName = device.meta().getString("numass.run", "");
            Meta meta = device.meta();
            if (meta.hasMeta("numass")) {
                try {
                    device.getLogger().info("Obtaining run information from cetral server...");
                    NumassClient client = new NumassClient(meta.getString("numass.ip", "192.168.111.1"),
                            meta.getInt("numass.port", 8335));
                    runName = client.getCurrentRun().getString("path", "");
                    device.getLogger().info("Run name is '{}'", runName);
                } catch (Exception ex) {
                    device.getLogger().warn("Failed to download current run information", ex);
                }
            }

            if (!runName.isEmpty()) {
                try {
                    localStorage = localStorage.buildShelf(runName, null);
                } catch (StorageException ex) {
                    device.getLogger().error("Failed to create storage shelf. Using root storage instead");
                }
            }

            connection = new StorageConnection(localStorage);
            device.connect(connection, Roles.STORAGE_ROLE);
        } else if (connection != null) {
            device.disconnect(connection);
        }
    }

}
