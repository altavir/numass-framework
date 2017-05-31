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
package inr.numass.control.cryotemp;

import hep.dataforge.control.measurements.Measurement;
import hep.dataforge.control.measurements.MeasurementListener;
import hep.dataforge.meta.Meta;
import hep.dataforge.plots.PlotUtils;
import hep.dataforge.plots.data.TimePlottable;
import hep.dataforge.plots.data.TimePlottableGroup;
import hep.dataforge.plots.fx.FXPlotFrame;
import hep.dataforge.plots.fx.PlotContainer;
import hep.dataforge.plots.jfreechart.JFreeChartFrame;
import inr.numass.control.DeviceViewConnection;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;

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
public class PKT8PlotView extends DeviceViewConnection<PKT8Device> implements Initializable, MeasurementListener {

    private FXPlotFrame plotFrame;
    private TimePlottableGroup plottables;

    @FXML
    private BorderPane root;
    @FXML
    private ToggleButton rawDataButton;
    @FXML
    private AnchorPane plotArea;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {

    }

    @Override
    public void open(PKT8Device device) throws Exception {
        super.open(device);
        rawDataButton.selectedProperty().addListener(observable -> {
            if (plotFrame != null) {
                setupPlotFrame(plotFrame.getConfig());
                if (device != null) {
                    setupChannels(device);
                }
            }
        });

        setupPlotFrame(device.meta().getMetaOrEmpty("plot.frame"));
        setupChannels(device);
    }

    @Override
    public void close() throws Exception {
        super.close();
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

    private void setupChannels(PKT8Device device) {
        Collection<PKT8Channel> channels = device.getChanels();

        //plot config from device configuration
        //Do not use view config here, it is applyed separately
        channels.stream()
                .filter(channel -> !plottables.has(channel.getName()))
                .forEachOrdered(channel -> {
                    //plot config from device configuration
                    TimePlottable plottable = new TimePlottable(channel.getName());
                    plottable.configure(channel.meta());
                    plottables.add(plottable);
                    plotFrame.add(plottable);
                });
        if (device.meta().hasMeta("plotConfig")) {
            plottables.applyConfig(device.meta().getMeta("plotConfig"));
            plottables.setMaxItems(1000);
            plottables.setPrefItems(400);
        }
//        getPlottables.applyConfig(plotFrame.getConfig());
    }

    @Override
    public synchronized void onMeasurementResult(Measurement measurement, Object result, Instant time) {
        PKT8Result res = PKT8Result.class.cast(result);
        //PENDING replace by connection?
        if (rawDataButton.isSelected()) {
            plottables.put(res.getChannel(), res.getRawValue());
        } else {
            plottables.put(res.getChannel(), res.getTemperature());
        }
    }

    @Override
    public void onMeasurementFailed(Measurement measurement, Throwable exception) {

    }


    @Override
    public Node getFXNode() {
        return root;
    }
}
