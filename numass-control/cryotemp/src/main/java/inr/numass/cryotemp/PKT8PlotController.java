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

import hep.dataforge.control.measurements.Measurement;
import hep.dataforge.control.measurements.MeasurementListener;
import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaUtils;
import hep.dataforge.plots.PlotUtils;
import hep.dataforge.plots.data.TimePlottable;
import hep.dataforge.plots.data.TimePlottableGroup;
import hep.dataforge.plots.fx.FXPlotFrame;
import hep.dataforge.plots.fx.PlotContainer;
import hep.dataforge.plots.jfreechart.JFreeChartFrame;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.AnchorPane;

import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.ResourceBundle;

/**
 * FXML Controller class
 *
 * @author darksnake
 */
public class PKT8PlotController implements Initializable, MeasurementListener<PKT8Result> {

    private final PKT8Device device;
    private FXPlotFrame plotFrame;
    private TimePlottableGroup plottables;

    @FXML
    private ToggleButton rawDataButton;
    @FXML
    private AnchorPane plotArea;

    public PKT8PlotController(PKT8Device device) {
        this.device = device;
    }

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        rawDataButton.selectedProperty().addListener(observable -> {
            if (plotFrame != null) {
                setupPlotFrame(plotFrame.getConfig());
                if (device != null) {
                    setupChannels();
                }
            }
        });

        configure(device.getConfig());
        setupChannels();
    }

    public String getDeviceName() {
        return device.getName();
    }

    public void configure(Meta config) {
        if (config.hasMeta("plotConfig")) {
            Meta plotConfig = MetaUtils.findNodeByValue(config, "plotConfig", "device", getDeviceName());
            if (plotConfig == null) {
                plotConfig = config.getMeta("plotConfig");
            }

            setupPlotFrame(plotConfig.getMeta("plotFrame", Meta.empty()));
        }
    }

    /**
     * Set o reset plot area
     */
    private synchronized void setupPlotFrame(Meta plotFrameMeta) {
        plottables = new TimePlottableGroup();
        plottables.setMaxAge(Duration.parse(plotFrameMeta.getString("maxAge", "PT2H")));
        plotArea.getChildren().clear();
        plotFrame = new JFreeChartFrame(plotFrameMeta);
        PlotUtils.setXAxis(plotFrame, "timestamp", null, "time");
        PlotContainer container = PlotContainer.anchorTo(plotArea);
        container.setPlot(plotFrame);
    }

    private void setupChannels() {
        Collection<PKT8Channel> channels = this.device.getChanels();

        //plot config from device configuration
        //Do not use view config here, it is applyed separately
        channels.stream()
                .filter(channel -> !plottables.hasPlottable(channel.getName()))
                .forEach(channel -> {

                    //plot config from device configuration
                    Meta deviceLineMeta = channel.meta().getMeta("plot", channel.meta());

                    //Do not use view config here, it is applyed separately
                    TimePlottable plottable = new TimePlottable(channel.getName());
                    if (deviceLineMeta.hasMeta("plot")) {
                        plottable.configure(deviceLineMeta.getMeta("plot"));
                    }
                    plottables.addPlottable(plottable);
                    plotFrame.add(plottable);
                });
        if (device.meta().hasMeta("plotConfig")) {
            plottables.applyConfig(device.meta().getMeta("plotConfig"));
            plottables.setMaxItems(1000);
            plottables.setPrefItems(400);
        }
//        plottables.applyConfig(plotFrame.getConfig());
    }

    @Override
    public synchronized void onMeasurementResult(Measurement<PKT8Result> measurement, PKT8Result result, Instant time) {
        //PENDING replace by connection?
        if (rawDataButton.isSelected()) {
            plottables.put(result.channel, result.rawValue);
        } else {
            plottables.put(result.channel, result.temperature);
        }
    }

    @Override
    public void onMeasurementFailed(Measurement measurement, Throwable exception) {

    }


}
