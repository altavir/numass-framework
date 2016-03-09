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
import hep.dataforge.points.DataPoint;
import hep.dataforge.points.MapPoint;
import hep.dataforge.plots.PlotUtils;
import hep.dataforge.plots.data.DynamicPlottable;
import hep.dataforge.plots.data.DynamicPlottableSet;
import hep.dataforge.plots.fx.PlotContainer;
import hep.dataforge.plots.jfreechart.JFreeChartFrame;
import hep.dataforge.storage.api.PointLoader;
import hep.dataforge.storage.api.Storage;
import hep.dataforge.values.Value;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.layout.AnchorPane;
import org.slf4j.LoggerFactory;

/**
 * FXML Controller class
 *
 * @author darksnake
 */
public class MspViewController {

    private FXTaskManager callback;

    private final AnchorPane mspPlotPane;

    public MspViewController(FXTaskManager callback, AnchorPane mspPlotPane) {
        this.callback = callback;
        this.mspPlotPane = mspPlotPane;
    }

    /**
     * update detector pane with new data
     */
    private void updateMspPane(DynamicPlottableSet mspData) {
//        MetaBuilder plotMeta = new MetaBuilder("plot")
//                .setNode(new MetaBuilder("xAxis")
//                        .setValue("axisTitle", "time")
//                        .setValue("type", "time"))
//                .setNode(new MetaBuilder("yAxis")
//                        .setValue("axisTitle", "partial pressure")
//                        .setValue("axisUnits", "mbar")
//                        .setValue("type", "log")
//                );
        JFreeChartFrame frame = new JFreeChartFrame("mspData", null);
        PlotUtils.setYAxis(frame, "partial pressure", "mbar", "log");
        frame.getConfig().setValue("yAxis.range.lower", 1e-10);
        frame.getConfig().setValue("yAxis.range.upper", 1e-3);
        PlotUtils.setXAxis(frame, "time", null, "time");

        StreamSupport.stream(mspData.spliterator(), false)
                .sorted((DynamicPlottable o1, DynamicPlottable o2) -> 
                        Integer.valueOf(o1.getName()).compareTo(Integer.valueOf(o2.getName()))).forEach((pl) -> frame.add(pl));
        Platform.runLater(() -> {
            PlotContainer container = PlotContainer.anchorTo(mspPlotPane);
            container.setPlot(frame);
        });
    }

    public void fillMspData(Storage rootStorage) {
        if (rootStorage != null) {
            MspDataFillTask fillTask = new MspDataFillTask(rootStorage);
            if (callback != null) {
                callback.postTask(fillTask);
            }
            Viewer.runTask(fillTask);
        }
    }

    private class MspDataFillTask extends Task<Void> {

        private final Storage storage;

        public MspDataFillTask(Storage storage) {
            this.storage = storage;
        }

        @Override
        protected Void call() throws Exception {
            updateTitle("Fill msp data (" + storage.getName() + ")");
            MspDataLoadTask loadTask = new MspDataLoadTask(storage);
            if (callback != null) {
                callback.postTask(loadTask);
            }
            Viewer.runTask(loadTask);
            List<DataPoint> mspData = loadTask.get();

            DynamicPlottableSet plottables = new DynamicPlottableSet();

            for (DataPoint point : mspData) {
                for (String name : point.names()) {
                    if (!name.equals("timestamp")) {
                        if (!plottables.hasPlottable(name)) {
                            plottables.addPlottable(new DynamicPlottable(name, name));
                        }
                    }
                }
                plottables.put(point);
            }

            updateMspPane(plottables);
            return null;
        }

    }

    private class MspDataLoadTask extends Task<List<DataPoint>> {

        private final Storage storage;

        public MspDataLoadTask(Storage storage) {
            this.storage = storage;
        }

        @Override
        protected List<DataPoint> call() throws Exception {
            updateTitle("Load msp data (" + storage.getName() + ")");
            List<DataPoint> mspData = new ArrayList<>();
            DataPoint last = null;
            for (String loaderName : storage.loaders().keySet()) {
                if (loaderName.startsWith("msp")) {
                    try (PointLoader mspLoader = (PointLoader) storage.getLoader(loaderName)) {
                        mspLoader.open();
                        updateMessage("Loading mass spectrometer data from " + mspLoader.getName());
                        updateProgress(-1, 1);
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

            updateMessage("Loading msp data finished");
            updateProgress(0, 1);
            return mspData;
        }

    }

    public void setCallback(FXTaskManager callback) {
        this.callback = callback;
    }

    /**
     * Create a null value point to terminate msp series
     *
     * @param last
     * @return
     */
    private DataPoint terminatorPoint(DataPoint last) {
        MapPoint p = new MapPoint();
        p.putValue("timestamp", last.getValue("timestamp").timeValue().plusMillis(10));
        for (String name : last.namesAsArray()) {
            if (!name.equals("timestamp")) {
                p.putValue(name, Value.NULL);
            }
        }
        return p;
    }
}
