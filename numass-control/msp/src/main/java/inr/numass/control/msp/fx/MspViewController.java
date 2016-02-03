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

import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaBuilder;
import hep.dataforge.meta.ConfigChangeListener;
import hep.dataforge.meta.Configuration;
import hep.dataforge.context.Context;
import hep.dataforge.context.GlobalContext;
import hep.dataforge.data.MapDataPoint;
import hep.dataforge.exceptions.ControlException;
import hep.dataforge.exceptions.PortException;
import hep.dataforge.io.MetaFileReader;
import hep.dataforge.plots.data.DynamicPlottable;
import hep.dataforge.plots.data.DynamicPlottableSet;
import hep.dataforge.plots.jfreechart.JFreeChartFrame;
import hep.dataforge.values.Value;
import inr.numass.control.msp.MspDevice;
import inr.numass.control.msp.MspListener;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.DragEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import org.slf4j.LoggerFactory;

/**
 * FXML Controller class
 *
 * @author darksnake
 */
public class MspViewController implements Initializable, MspListener {

    public static final String MSP_DEVICE_TYPE = "msp";

    public static final String DEFAULT_CONFIG_LOCATION = "msp-config.xml";

    private MspDevice device;

    private Configuration viewConfig;

    private JFreeChartFrame plotFrame;

    private final DynamicPlottableSet plottables = new DynamicPlottableSet();

    private final String mspName = "msp";

    @FXML
    private Slider autoRangeSlider;
    @FXML
    private ToggleButton fillamentButton;
    @FXML
    private Circle fillamentIndicator;
    @FXML
    private TextArea logArea;
    @FXML
    private ToggleButton plotButton;
    @FXML
    private AnchorPane plotPane;
    @FXML
    private Button loadConfigButton;

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

    public MspViewController() {
    }

    public Configuration getViewConfig() {
        if (viewConfig == null) {
            viewConfig = new Configuration(getDevice().meta().getNode("peakJump"));
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
        if (config.hasNode("device")) {
            for (Meta d : config.getNodes("device")) {
                if (d.getString("type", "unknown").equals(MSP_DEVICE_TYPE)
                        && d.getString("name", "msp").equals(this.mspName)) {
                    mspConfig = d;
                }
            }
        } else if (config.hasNode("peakJump")) {
            mspConfig = config;
        }

        if (mspConfig != null) {
            this.device = new MspDevice(mspName, context, mspConfig);
            try {
                getDevice().setListener(this);
                getDevice().init();
                getDevice().startMeasurement("peakJump");
            } catch (ControlException ex) {
                showError(String.format("Can't connect to %s:%d. The port is either busy or not the MKS mass-spectrometer port",
                        config.getString("connection.ip", "127.0.0.1"),
                        config.getInt("connection.port", 10014)));
                throw new RuntimeException("Can't connect to device");
            }
        } else {
            showError("Can't find device description in given confgiuration");
            throw new RuntimeException();
        }

        if (config.hasNode("plots.msp")) {
            setViewConfig(config.getNode("plots.msp"));
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
                        .setValue("logAxis", true)
                        .setValue("axisTitle", "partial pressure")
                        .setValue("axisUnits", "mbar")
                )
                .setValue("xAxis.timeAxis", true);
        this.plotFrame = new JFreeChartFrame(mspName, plotConfig).display(plotPane);
        updatePlot();
//        this.plot = DynamicPlot.attachToFX(plotPane, new AnnotationBuilder("plot-config").putValue("logY", true).build());
//        plot.setAutoRange(30 * 60);
    }

    public void updatePlot() {
        if (plotFrame == null) {
            initPlot();
        }
        Meta config = getViewConfig();
        if (config.hasNode("plotFrame")) {
            this.plotFrame.configure(config.getNode("plotFrame"));
        }
        if (config.hasNode("peakJump.line")) {
            for (Meta an : config.getNodes("peakJump.line")) {
                String mass = an.getString("mass");

                if (!this.plottables.hasPlottable(mass)) {
                    DynamicPlottable newPlottable = new DynamicPlottable(mass, an, mass);
                    this.plottables.addPlottable(newPlottable);
                    plotFrame.add(newPlottable);
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
        MapDataPoint point = new MapDataPoint();
        for (Map.Entry<Integer, Double> entry : measurement.entrySet()) {
            Double val = entry.getValue();
            if (val <= 0) {
                val = Double.NaN;
            }
            point.putValue(Integer.toString(entry.getKey()), val);
        }
        plottables.put(point);
    }

    @Override
    public void acceptMessage(String message) {
        Platform.runLater(() -> {
            logArea.appendText("RECIEVE: " + message + "\r\n");
        });
    }

    @Override
    public void acceptRequest(String message) {
        Platform.runLater(() -> {
            logArea.appendText("SEND: " + message + "\r\n");
        });
    }

    @Override
    public void error(String errorMessage, Throwable error) {
        Platform.runLater(() -> {
            logArea.appendText("ERROR: " + errorMessage + "\r\n");
            showError(errorMessage);
        });

    }

    /**
     * Initializes the controller class.
     *
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
    }

    @FXML
    private void onAutoRangeChange(DragEvent event) {
        plottables.setMaxAge((int) (this.autoRangeSlider.getValue() * 60 * 1000));
    }

    @FXML
    private void onFillamentToggle(ActionEvent event) throws PortException {
        getDevice().setFileamentOn(fillamentButton.isSelected());
    }

    @FXML
    private void onLoadConfig(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Resource File");
        fileChooser.setInitialFileName(DEFAULT_CONFIG_LOCATION);
        fileChooser.setInitialDirectory(GlobalContext.instance().io().getRootDirectory());
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("xml", "*.xml", "*.XML"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("all", "*.*"));
        File cfgFile = fileChooser.showOpenDialog(loadConfigButton.getScene().getWindow());
        if (cfgFile != null) {
            setDeviceConfig(GlobalContext.instance(), cfgFile);
        }
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

    public void disconnect() throws IOException, PortException, ControlException {
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

}
