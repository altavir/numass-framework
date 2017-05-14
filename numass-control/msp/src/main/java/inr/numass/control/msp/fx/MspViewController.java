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

import hep.dataforge.control.devices.DeviceListener;
import hep.dataforge.exceptions.ControlException;
import hep.dataforge.exceptions.PortException;
import hep.dataforge.fx.fragments.FragmentWindow;
import hep.dataforge.fx.fragments.LogFragment;
import hep.dataforge.meta.ConfigChangeListener;
import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaBuilder;
import hep.dataforge.plots.data.TimePlottable;
import hep.dataforge.plots.data.TimePlottableGroup;
import hep.dataforge.plots.fx.PlotContainer;
import hep.dataforge.plots.jfreechart.JFreeChartFrame;
import hep.dataforge.values.Value;
import inr.numass.control.DeviceViewConnection;
import inr.numass.control.msp.MspDevice;
import inr.numass.control.msp.MspListener;
import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.util.StringConverter;
import org.controlsfx.control.ToggleSwitch;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * FXML Controller class
 *
 * @author darksnake
 */
public class MspViewController extends DeviceViewConnection<MspDevice> implements DeviceListener, Initializable, MspListener {

    public static MspViewController build() {
        try {
            FXMLLoader loader = new FXMLLoader(MspViewController.class.getResource("/fxml/MspView.fxml"));
            loader.load();
            return loader.getController();
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    private final TimePlottableGroup plottables = new TimePlottableGroup();
    //    private Configuration viewConfig;
    private JFreeChartFrame plot;
    private LogFragment logArea;

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
    private BorderPane root;
    @FXML
    private ToggleSwitch fillamentButton;
    @FXML
    private Circle fillamentIndicator;
    @FXML
    private ToggleButton measureButton;
    @FXML
    private BorderPane plotPane;
    @FXML
    public ToggleButton connectButton;
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
        logArea = new LogFragment();
        new FragmentWindow(logArea).bindTo(consoleButton);
        fillamentSelector.setItems(FXCollections.observableArrayList(1, 2));
        fillamentSelector.setConverter(new StringConverter<Integer>() {
            @Override
            public String toString(Integer object) {
                return "Filament " + object;
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
                getDevice().getLogger().error("Failed to toggle filaments");
            }
        });

        BooleanBinding disabled = connectButton.selectedProperty().not();
        fillamentButton.disableProperty().bind(disabled);
        measureButton.disableProperty().bind(disabled);
        storeButton.disableProperty().bind(disabled);

    }


    public Meta getViewConfig() {
        return getDevice().meta().getMeta("plotConfig", getDevice().getMeta());
    }


    @Override
    public void open(MspDevice device) throws Exception {
        super.open(device);
        getDevice().setMspListener(this);
        updatePlot();

        bindBooleanToState("connected", connectButton.selectedProperty());
    }

//    public void setDeviceConfig(Context context, File cfgFile) {
//        try {
//            Meta deviceConfig = MetaFileReader.instance().read(context, cfgFile, null);
//            setDeviceConfig(context, deviceConfig);
//        } catch (IOException | ParseException ex) {
//            showError("Can't load configuration file");
//        }
//    }

    private void initPlot() {
        Meta plotConfig = new MetaBuilder("plotFrame")
                .setNode(new MetaBuilder("yAxis")
                        .setValue("type", "log")
                        .setValue("axisTitle", "partial pressure")
                        .setValue("axisUnits", "mbar")
                )
                .setValue("xAxis.type", "time");

        this.plot = new JFreeChartFrame(plotConfig);
        PlotContainer container = PlotContainer.centerIn(plotPane);
        container.setPlot(plot);
//        updatePlot();
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
        if (config.hasMeta("peakJump.peak")) {
            for (Meta an : config.getMetaList("peakJump.peak")) {
                String mass = an.getString("mass");
                if (!this.plottables.has(mass)) {
                    TimePlottable newPlottable = new TimePlottable(mass, mass);
                    newPlottable.configure(an);
                    newPlottable.setMaxItems(1000);
                    newPlottable.setPrefItems(400);
                    this.plottables.add(newPlottable);
                    plot.add(newPlottable);
                } else {
                    plottables.get(mass).configure(an);
                }
            }
        } else {
            showError("No peaks defined in config");
            throw new RuntimeException();
        }
    }

    @Override
    public void acceptScan(Map<Integer, Double> measurement) {
//        MapPoint.Builder point = new MapPoint.Builder();
        for (Map.Entry<Integer, Double> entry : measurement.entrySet()) {
            Double val = entry.getValue();
            if (val <= 0) {
                val = Double.NaN;
            }
            TimePlottable pl = plottables.get(Integer.toString(entry.getKey()));
            if (pl != null) {
                pl.put(Value.of(val));
            }
        }
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
    private void onPlotToggle(ActionEvent event) throws ControlException {
        if (measureButton.isSelected()) {
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

    @Override
    public void acceptFilamentStateChange(String fillamentState) {
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
        getDevice().setState("storing", storeButton.isSelected());
//        if (storeButton.isSelected()) {
//
//            if (!getDevice().meta().hasMeta("storage")) {
//                getDevice().getLogger().info("Storage not defined. Starting storage selection dialog");
//                DirectoryChooser chooser = new DirectoryChooser();
//                File storageDir = chooser.showDialog(this.plotPane.getScene().getWindow());
//                if (storageDir == null) {
//                    storeButton.setSelected(false);
//                    throw new RuntimeException("User canceled directory selection");
//                }
//                getDevice().getConfig().putNode(new MetaBuilder("storage")
//                        .putValue("path", storageDir.getAbsolutePath()));
//            }
//            Meta storageConfig = getDevice().meta().getMeta("storage");
//            Storage localStorage = StorageManager.buildFrom(getDevice().getContext())
//                    .buildStorage(storageConfig);
//
//            String runName = getDevice().meta().getString("numass.run", "");
//            Meta meta = getDevice().meta();
//            if (meta.hasMeta("numass")) {
//                try {
//                    getDevice().getLogger().info("Obtaining run information from cetral server...");
//                    NumassClient client = new NumassClient(meta.getString("numass.ip", "192.168.111.1"),
//                            meta.getInt("numass.port", 8335));
//                    runName = client.getCurrentRun().getString("path", "");
//                    getDevice().getLogger().info("Run name is '{}'", runName);
//                } catch (Exception ex) {
//                    getDevice().getLogger().warn("Failed to download current run information", ex);
//                }
//            }
//
//            if (!runName.isEmpty()) {
//                try {
//                    localStorage = localStorage.buildShelf(runName, null);
//                } catch (StorageException ex) {
//                    getDevice().getLogger().error("Failed to create storage shelf. Using root storage instead");
//                }
//            }
//
//            connection = new StorageConnection(localStorage);
//            getDevice().connect(connection, Roles.STORAGE_ROLE);
//        } else if (connection != null) {
//            getDevice().disconnect(connection);
//        }
    }


    @Override
    public Node getFXNode() {
        return root;
    }
}
