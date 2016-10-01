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
package inr.numass.cryotemp;

import hep.dataforge.control.devices.Device;
import hep.dataforge.control.devices.DeviceListener;
import hep.dataforge.control.measurements.Measurement;
import hep.dataforge.control.measurements.MeasurementListener;
import hep.dataforge.exceptions.ControlException;
import hep.dataforge.fx.ConsoleFragment;
import hep.dataforge.io.MetaFileReader;
import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaUtils;
import hep.dataforge.plots.PlotUtils;
import hep.dataforge.plots.data.TimePlottable;
import hep.dataforge.plots.data.TimePlottableGroup;
import hep.dataforge.plots.data.XYPlottable;
import hep.dataforge.plots.fx.FXPlotFrame;
import hep.dataforge.plots.fx.PlotContainer;
import hep.dataforge.plots.jfreechart.JFreeChartFrame;
import hep.dataforge.values.Value;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.ResourceBundle;

/**
 * FXML Controller class
 *
 * @author darksnake
 */
public class PKT8MainViewController implements Initializable, DeviceListener, MeasurementListener<PKT8Result>, AutoCloseable {

    public static final String DEFAULT_CONFIG_LOCATION = "devices.xml";
    ConsoleFragment consoleFragment;
    private PKT8Device device;
    private FXPlotFrame<XYPlottable> plotFrame;
    private TimePlottableGroup plottables;
    @FXML
    private Button loadConfigButton;
    @FXML
    private ToggleButton startStopButton;
    @FXML
    private ToggleButton rawDataButton;
    @FXML
    private AnchorPane plotArea;
    @FXML
    private ToggleButton consoleButton;

    @Override
    public void close() throws Exception {
        if (device != null) {
            device.shutdown();
        }
    }

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupPlotFrame(null);
        this.consoleFragment = new ConsoleFragment();
        consoleFragment.bindTo(consoleButton);
        rawDataButton.selectedProperty().addListener(new InvalidationListener() {
            @Override
            public void invalidated(Observable observable) {
                if (plotFrame != null) {
                    setupPlotFrame(plotFrame.getConfig());
                    if (device != null) {
                        setupChannels();
                    }
                }
            }
        });
    }

    @FXML
    private void onLoadConfigClick(ActionEvent event) throws IOException, ParseException, ControlException {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open configuration file");
        fileChooser.setInitialFileName(DEFAULT_CONFIG_LOCATION);
//        fileChooser.setInitialDirectory(GlobalContext.instance().io().getRootDirectory());
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("xml", "*.xml", "*.XML"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("json", "*.json", "*.JSON"));
//        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("all", "*.*"));
        File cfgFile = fileChooser.showOpenDialog(loadConfigButton.getScene().getWindow());

        if (cfgFile != null) {
            setConfig(MetaFileReader.read(cfgFile));
        }
    }

    public void loadTestConfig() throws ControlException {
        try {
            Meta testConfig = MetaFileReader
                    .read(new File(getClass().getResource("/config/defaultConfig.xml").toURI()));
            setConfig(testConfig);
        } catch (URISyntaxException | IOException | ParseException ex) {
            throw new Error(ex);
        }
    }

    public String getDeviceName() {
        return "PKT8";
    }

    public void setConfig(Meta config) throws ControlException {
        if (config.hasNode("plotConfig")) {
            Meta plotConfig = MetaUtils.findNodeByValue(config, "plotConfig", "device", getDeviceName());
            if (plotConfig == null) {
                plotConfig = config.getNode("plotConfig");
            }

            setupPlotFrame(plotConfig.getNode("plotFrame", null));
        }

        if (config.hasNode("device")) {
            Meta deviceMeta = MetaUtils.findNodeByValue(config, "device", "name", Value.of(getDeviceName()));
            setupDevice(deviceMeta);
        } else {
            setupDevice(config);
        }

    }

    /**
     * Set o reset plot area
     */
    private synchronized void setupPlotFrame(Meta plotFrameMeta) {
        plottables = new TimePlottableGroup();
        plottables.setMaxItems(plotFrameMeta.getInt("maxItems",3000));
        plottables.setMaxAge(Duration.parse(plotFrameMeta.getString("maxAge","PT2H")));
        plotArea.getChildren().clear();
        plotFrame = new JFreeChartFrame(plotFrameMeta);
        PlotUtils.setXAxis(plotFrame, "timestamp", null, "time");
        PlotContainer container = PlotContainer.anchorTo(plotArea);
        container.setPlot(plotFrame);
    }

    public void setupDevice(Meta deviceMeta) throws ControlException {
        if (device != null) {
            device.stopMeasurement(true);
            device.shutdown();
        }

        this.device = new PKT8Device(deviceMeta.getString("port", "virtual"));

        device.configure(deviceMeta);

        device.addDeviceListener(this);
        consoleFragment.addLogHandler(device.getLogger());

        device.init();
    }

    private void setupChannels() {
        Collection<PKT8Channel> channels = this.device.getChanels();

        //plot config from device configuration
        //Do not use view config here, it is applyed separately
        channels.stream()
                .filter(channel -> !plottables.hasPlottable(channel.getName()))
                .forEach(channel -> {

                    //plot config from device configuration
                    Meta deviceLineMeta = channel.meta().getNode("plot", channel.meta());

                    //Do not use view config here, it is applyed separately
                    TimePlottable plottable = new TimePlottable(channel.getName());
                    plottable.configure(deviceLineMeta);
                    plottables.addPlottable(plottable);
                    plotFrame.add(plottable);
                });
        plottables.applyConfig(plotFrame.getConfig());
    }

    @Override
    public void notifyDeviceInitialized(Device device) {
        setupChannels();
        startStopButton.setDisable(false);
    }

//    public void applyViewConfig(Meta viewConfig) {
//        plottables.applyConfig(viewConfig);
//    }

    @Override
    public void notifyDeviceShutdown(Device device) {
        startStopButton.setDisable(true);
    }


//    @Override
//    public void sendMessage(Device device, int priority, Meta message) {
//        String tag = message.getString("tag", "");
//        logArea.appendText(String.format("%s > (%s) [%s] %s%n", device.getName(), Instant.now().toString(), tag, message));
//    }


    @Override
    public synchronized void onMeasurementResult(Measurement<PKT8Result> measurement, PKT8Result result, Instant time) {
        if (rawDataButton.isSelected()) {
            plottables.put(result.channel, result.rawValue);
        } else {
            plottables.put(result.channel, result.temperature);
        }
    }

    @Override
    public void onMeasurementFailed(Measurement measurement, Throwable exception) {

    }

    @Override
    public void notifyDeviceStateChanged(Device device, String name, Value state) {

    }

    @Override
    public void evaluateDeviceException(Device device, String message, Throwable exception) {

    }

    @FXML
    private void onStartStopClick(ActionEvent event) {
        if (device != null) {
            try {
                if (startStopButton.isSelected()) {
                    device.startMeasurement()
                            .addListener(this);
                } else {
                    //in case device started
                    if (device.isMeasuring()) {
                        device.getMeasurement().removeListener(this);
                        device.stopMeasurement(false);
                    }
                }
            } catch (ControlException ex) {

            }
        }
    }

}
