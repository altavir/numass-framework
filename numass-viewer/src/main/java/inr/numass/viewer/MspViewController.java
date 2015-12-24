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
package inr.numass.viewer;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import hep.dataforge.data.DataPoint;
import hep.dataforge.data.MapDataPoint;
import hep.dataforge.exceptions.StorageException;
import hep.dataforge.storage.api.PointLoader;
import hep.dataforge.storage.api.Storage;
import hep.dataforge.values.Value;
import static inr.numass.viewer.NumassViewerUtils.displayPlot;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.LogAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYStepRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.slf4j.LoggerFactory;

/**
 * FXML Controller class
 *
 * @author darksnake
 */
public class MspViewController implements Initializable {

    private ProgressUpdateCallback callback;

    @FXML
    private AnchorPane mspPlotPane;
    @FXML
    private VBox mspSelectorPane;

    /**
     * Initializes the controller class.
     *
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
    }

    /**
     * update detector pane with new data
     */
    private void updateMspPane(XYSeriesCollection detectorData) {
        Platform.runLater(() -> {
            if (detectorData == null) {
                throw new IllegalArgumentException("Detector data not defined");
            }

            mspSelectorPane.getChildren().clear();//removing all checkboxes
            mspPlotPane.getChildren().clear();//removing plot 

            DateAxis xAxis = new DateAxis("time");
            LogAxis yAxis = new LogAxis("partial pressure (mbar)");
            yAxis.setAutoRange(true);
            yAxis.setAutoTickUnitSelection(false);
            yAxis.setNumberFormatOverride(new DecimalFormat("0E0"));
            //NumberAxis yAxis = new NumberAxis();

            XYPlot plot = new XYPlot(detectorData, xAxis, yAxis, new XYStepRenderer());

            JFreeChart mspPlot = new JFreeChart("Mass-spectrum peak jump plot", plot);

            displayPlot(mspPlotPane, mspPlot);

            for (int i = 0; i < plot.getDatasetCount(); i++) {
                final XYDataset dataset = plot.getDataset(i);
                for (int j = 0; j < dataset.getSeriesCount(); j++) {
                    CheckBox cb = new CheckBox(dataset.getSeriesKey(j).toString());
                    cb.setSelected(true);
                    final int seriesNumber = j;
                    cb.setOnAction((ActionEvent event) -> {
                        boolean checked = cb.isSelected();
                        plot.getRendererForDataset(dataset).setSeriesVisible(seriesNumber, checked);
                    });
                    mspSelectorPane.getChildren().add(cb);
                }
            }
        });
    }

    public void fillMspData(Storage rootStorage) {
        if (rootStorage != null) {
            try {
                List<DataPoint> mspData = getMspData(rootStorage);
                Map<String, XYSeries> series = new HashMap<>();

                for (DataPoint point : mspData) {
                    for (String name : point.names()) {
                        if (!name.equals("timestamp")) {
                            if (!series.containsKey(name)) {
                                series.put(name, new XYSeries(name));
                            }
                            long time = point.getValue("timestamp").timeValue().toEpochMilli();
                            double value = point.getDouble(name);
                            if (value > 0) {
                                series.get(name).add(time, value);
                            }
                        }
                    }
                }
                XYSeriesCollection mspSeriesCollection = new XYSeriesCollection();
                List<String> names = new ArrayList<>(series.keySet());
                names.sort((String o1, String o2) -> {
                    try {
                        return Integer.valueOf(o1).compareTo(Integer.valueOf(o2));
                    } catch (Exception ex) {
                        return 0;
                    }
                });
                for (String name : names) {
                    mspSeriesCollection.addSeries(series.get(name));
                }
                updateMspPane(mspSeriesCollection);
            } catch (StorageException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private List<DataPoint> getMspData(Storage storage) throws StorageException {
        List<DataPoint> mspData = new ArrayList<>();
        DataPoint last = null;
        for (String loaderName : storage.loaders().keySet()) {
            if (loaderName.startsWith("msp")) {
                try (PointLoader mspLoader = (PointLoader) storage.getLoader(loaderName)) {
                    mspLoader.open();
                    updateProgress("Loading mass spectrometer data from " + mspLoader.getName());
                    updateProgress(-1);
                    for (DataPoint dp : mspLoader.asDataSet()) {
                        mspData.add(dp);
                        last = dp;
                    }
                    if (last != null) {
                        mspData.add(terminatorPoint(last));
                    }
                } catch (Exception ex) {
                    LoggerFactory.getLogger(getClass()).error("Can't read msp loader data", ex);
                }
            }
        }
//        for (String shelfName : storage.shelves().keySet()) {
//            mspData.addAll(getMspData(storage.getShelf(shelfName)));
//        }

        updateProgress("Loading msp data finished");
        updateProgress(0);
        return mspData;
    }

    private void updateProgress(String progress) {
        if (callback != null) {
            callback.setProgressText(progress);
        }
    }

    private void updateProgress(double progress) {
        if (callback != null) {
            callback.setProgress(progress);
        }
    }

    public void setCallback(ProgressUpdateCallback callback) {
        this.callback = callback;
    }

    /**
     * Create a null value point to terminate msp series
     *
     * @param last
     * @return
     */
    private DataPoint terminatorPoint(DataPoint last) {
        MapDataPoint p = new MapDataPoint();
        p.putValue("timestamp", last.getValue("timestamp").timeValue().plusMillis(10));
        for (String name : last.namesAsArray()) {
            if (!name.equals("timestamp")) {
                p.putValue(name, Value.NULL);
            }
        }
        return p;
    }
}
