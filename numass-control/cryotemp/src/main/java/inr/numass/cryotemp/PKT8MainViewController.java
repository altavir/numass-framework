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

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.AppenderBase;
import de.jensd.shichimifx.utils.SplitPaneDividerSlider;
import hep.dataforge.context.GlobalContext;
import hep.dataforge.control.devices.Device;
import hep.dataforge.control.devices.DeviceListener;
import hep.dataforge.control.measurements.MeasurementDevice;
import hep.dataforge.control.measurements.MeasurementListener;
import hep.dataforge.exceptions.ControlException;
import hep.dataforge.io.MetaFileReader;
import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaBuilder;
import hep.dataforge.meta.MetaUtils;
import hep.dataforge.plots.XYPlotFrame;
import hep.dataforge.plots.data.DynamicPlottable;
import hep.dataforge.plots.data.DynamicPlottableSet;
import hep.dataforge.plots.jfreechart.JFreeChartFrame;
import hep.dataforge.values.Value;
import inr.numass.cryotemp.PKT8Device.PKT8Measurement;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.time.Instant;
import java.util.Collection;
import java.util.ResourceBundle;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;

/**
 * FXML Controller class
 *
 * @author darksnake
 */
public class PKT8MainViewController implements Initializable, DeviceListener, MeasurementListener<PKT8Measurement>, AutoCloseable {

    public static final String DEFAULT_CONFIG_LOCATION = "devices.xml";
    private PKT8Device device;
    private XYPlotFrame plotFrame;
    private DynamicPlottableSet plottables;
    private Meta currentPlotConfig;

    @FXML
    private Button loadConfigButton;
    @FXML
    private SplitPane consoleSplitPane;
    @FXML
    private TextArea logArea;
    @FXML
    private ToggleButton startStopButton;
    @FXML
    private AnchorPane plotArea;

    @FXML
    private ToggleButton consoleButton;

    @Override
    public void close() throws Exception {
        if (device != null) {
            device.stop();
            device.shutdown();
        }
    }

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupPlotFrame(null);
        SplitPaneDividerSlider slider = new SplitPaneDividerSlider(consoleSplitPane, 0, SplitPaneDividerSlider.Direction.DOWN);
        consoleButton.selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
            slider.setAimContentVisible(newValue);
        });
        slider.setAimContentVisible(false);
    }

    @FXML
    private void onLoadConfigClick(ActionEvent event) throws IOException, ParseException, ControlException {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open configuration file");
        fileChooser.setInitialFileName(DEFAULT_CONFIG_LOCATION);
        fileChooser.setInitialDirectory(GlobalContext.instance().io().getRootDirectory());
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("xml", "*.xml", "*.XML"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("json", "*.json", "*.JSON"));
//        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("all", "*.*"));
        File cfgFile = fileChooser.showOpenDialog(loadConfigButton.getScene().getWindow());

        if (cfgFile != null) {
            setConfig(MetaFileReader.read(cfgFile));
        }
    }

    private void loadTestConfig() throws ControlException {
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
            currentPlotConfig = plotConfig;
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
    private void setupPlotFrame(Meta plotFrameMeta) {
        plottables = new DynamicPlottableSet();
        plotArea.getChildren().clear();
        Meta plotConfig;
        if (plotFrameMeta != null) {
            plotConfig = new MetaBuilder(plotFrameMeta)
                    .setValue("xAxis.timeAxis", true);
        } else {
            plotConfig = new MetaBuilder("plotFrame")
                    .setValue("xAxis.timeAxis", true);
        }
        plotFrame = new JFreeChartFrame("plot", plotConfig).display(plotArea);
    }

    public void setupDevice(Meta deviceMeta) throws ControlException {
        if (device != null) {
            device.stop();
            device.shutdown();
        }

        this.device = new PKT8Device("PKT8", GlobalContext.instance(), deviceMeta);
        device.addDeviceListener(this);
        device.addMeasurementListener(this);

        logArea.appendText("Starting log...\n");

        Appender<ILoggingEvent> appender = new AppenderBase<ILoggingEvent>() {
//            private final DateTimeFormatter formatter = DateTimeFormatter.ISO_TIME;
            @Override
            protected void append(ILoggingEvent e) {
                logArea.appendText(String.format("%s > (%s) [%s] %s%n",
                        e.getLoggerName(),
                        Instant.now().toString(),
                        e.getLevel(),
                        e.getFormattedMessage()));
            }
        };

        appender.start();

        device.getLogger().addAppender(appender);

        device.init();
    }

    @Override
    public void notifyDeviceInitialized(Device device) {
        Collection<PKT8Device.PKT8Channel> channels = this.device.getChanels();
        for (PKT8Device.PKT8Channel channel : channels) {
            if (!plottables.hasPlottable(channel.getName())) {

                //plot config from device configuration
                Meta deviceLineMeta = channel.meta().getNode("plot", channel.meta());

                //Do not use view config here, it is applyed separately
                DynamicPlottable plottable = new DynamicPlottable(channel.getName(), deviceLineMeta);
                plottables.addPlottable(plottable);
                plotFrame.add(plottable);
            }
        }
        startStopButton.setDisable(false);

        if (currentPlotConfig != null) {
            applyViewConfig(currentPlotConfig);
        }
    }

    public void applyViewConfig(Meta viewConfig) {
        plottables.applyConfig(viewConfig);
    }

    @Override
    public void notifyDeviceShutdown(Device device) {
        startStopButton.setDisable(true);
    }

    @Override
    public void notifyDeviceStateChanged(Device device, String name, Value oldState, Value newState) {

    }

//    @Override
//    public void sendMessage(Device device, int priority, Meta message) {
//        String tag = message.getString("tag", "");
//        logArea.appendText(String.format("%s > (%s) [%s] %s%n", device.getName(), Instant.now().toString(), tag, message));
//    }

    @Override
    public void notifyMeasurementStarted(MeasurementDevice device, Meta measurement) {

    }

    @Override
    public void notifyMeasurementStopped(MeasurementDevice device) {

    }

    @Override
    public void notifyMeasurementResult(MeasurementDevice device, Meta measurement, PKT8Measurement measurementResult) {
        plottables.put(measurementResult.channel, measurementResult.temperature);
    }

    @FXML
    private void onStartStopClick(ActionEvent event) {
        if (device != null) {
            try {
                if (startStopButton.isSelected()) {
                    device.start();
                } else {
                    //in case device started
                    device.stop();
                }
            } catch (ControlException ex) {

            }
        }
    }

}
