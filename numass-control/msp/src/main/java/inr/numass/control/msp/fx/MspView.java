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

import hep.dataforge.control.NamedValueListener;
import hep.dataforge.control.devices.Device;
import hep.dataforge.control.devices.DeviceListener;
import hep.dataforge.exceptions.ControlException;
import hep.dataforge.exceptions.PortException;
import hep.dataforge.fx.fragments.FragmentWindow;
import hep.dataforge.fx.fragments.LogFragment;
import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaBuilder;
import hep.dataforge.plots.data.TimePlottable;
import hep.dataforge.plots.data.TimePlottableGroup;
import hep.dataforge.plots.fx.PlotContainer;
import hep.dataforge.plots.jfreechart.JFreeChartFrame;
import hep.dataforge.values.Value;
import inr.numass.control.DeviceViewConnection;
import inr.numass.control.msp.MspDevice;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
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
import java.util.ResourceBundle;

/**
 * FXML Controller class
 *
 * @author darksnake
 */
public class MspView extends DeviceViewConnection<MspDevice> implements DeviceListener, Initializable, NamedValueListener {

    public static MspView build() {
        try {
            FXMLLoader loader = new FXMLLoader(MspView.class.getResource("/fxml/MspView.fxml"));
            loader.load();
            return loader.getController();
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    private final TimePlottableGroup plottables = new TimePlottableGroup();
    //    private Configuration viewConfig;
    private JFreeChartFrame plot;
    private LogFragment logFragment;

    @FXML
    private BorderPane root;
    @FXML
    private ToggleSwitch filamentButton;
    @FXML
    private Circle filamentIndicator;
    @FXML
    private ToggleButton measureButton;
    @FXML
    private BorderPane plotPane;
    @FXML
    public ToggleButton connectButton;
    @FXML
    private ToggleButton consoleButton;
    @FXML
    private ComboBox<Integer> filamentSelector;
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
        logFragment = new LogFragment();
        new FragmentWindow(logFragment).bindTo(consoleButton);
        logFragment.addRootLogHandler();
        filamentSelector.setItems(FXCollections.observableArrayList(1, 2));
        filamentSelector.setConverter(new StringConverter<Integer>() {
            @Override
            public String toString(Integer object) {
                return "Filament " + object;
            }

            @Override
            public Integer fromString(String string) {
                return Integer.parseInt(string.substring(9));
            }
        });

        filamentSelector.getSelectionModel().select(0);
        filamentButton.selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
            try {
                filamentSelector.setDisable(newValue);
                getDevice().setFilamentOn(newValue);
            } catch (PortException ex) {
                getDevice().getLogger().error("Failed to toggle filaments");
            }
        });

        filamentButton.disableProperty().bind(connectButton.selectedProperty().not());
        measureButton.disableProperty().bind(filamentButton.selectedProperty().not());
        storeButton.disableProperty().bind(measureButton.selectedProperty().not());
        getStateBinding("filamentStatus").addListener(new ChangeListener<Value>() {
            @Override
            public void changed(ObservableValue<? extends Value> observable, Value oldValue, Value newValue) {
                String filamentState = newValue.stringValue();
                Platform.runLater(() -> {
                    switch (filamentState) {
                        case "ON":
                            filamentIndicator.setFill(Paint.valueOf("red"));
                            break;
                        case "OFF":
                            filamentIndicator.setFill(Paint.valueOf("blue"));
                            break;
                        case "WARM-UP":
                        case "COOL-DOWN":
                            filamentIndicator.setFill(Paint.valueOf("yellow"));
                            break;
                    }
                });
            }
        });
    }


    private Meta getViewConfig() {
        return getDevice().meta().getMeta("plotConfig", getDevice().getMeta());
    }


    @Override
    public void open(MspDevice device) throws Exception {
        super.open(device);
        updatePlot();

        bindBooleanToState("connected", connectButton.selectedProperty());
    }

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
    }

    private void updatePlot() {
        if (plot == null) {
            initPlot();
        }
        Meta config = getViewConfig();
        if (config.hasMeta("plotFrame")) {
            this.plot.configure(config.getMeta("plotFrame"));
        }
        if (config.hasMeta("peakJump.peak")) {
            for (Meta peakMeta : config.getMetaList("peakJump.peak")) {
                String mass = peakMeta.getString("mass");
                if (!this.plottables.has(mass)) {
                    TimePlottable newPlottable = new TimePlottable(mass, mass);
                    newPlottable.configure(peakMeta);
                    newPlottable.setMaxItems(1000);
                    newPlottable.setPrefItems(400);
                    newPlottable.configureValue("titleBase",peakMeta.getString("title",mass));
                    this.plottables.add(newPlottable);
                    plot.add(newPlottable);
                } else {
                    plottables.get(mass).configure(peakMeta);
                }
            }
        } else {
            showError("No peaks defined in config");
            throw new RuntimeException();
        }
    }

    @Override
    public void evaluateDeviceException(Device device, String message, Throwable exception) {
        Platform.runLater(() -> {
            logFragment.appendLine("ERROR: " + message);
            showError(message);
        });
    }

    @FXML
    private void onPlotToggle(ActionEvent event) throws ControlException {
        if (measureButton.isSelected()) {
            getDevice().startMeasurement();
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

    @FXML
    private void onStoreButtonClick(ActionEvent event) {
        getDevice().setState("storing", storeButton.isSelected());
    }

    @Override
    public Node getFXNode() {
        return root;
    }

    @Override
    public void pushValue(String valueName, Value value) {
        TimePlottable pl = plottables.get(valueName);
        if (pl != null) {
            if (value.doubleValue() > 0) {
                pl.put(value);
            } else {
                pl.put(Value.NULL);
            }
            String titleBase = pl.getConfig().getString("titleBase");
            String title = String.format("%s (%.4g)", titleBase, value.doubleValue());
            pl.configureValue("title", title);
        }
    }
}
