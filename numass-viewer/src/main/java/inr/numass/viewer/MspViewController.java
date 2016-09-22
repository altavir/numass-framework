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

import hep.dataforge.context.Context;
import hep.dataforge.names.Name;
import hep.dataforge.plots.PlotUtils;
import hep.dataforge.plots.data.DynamicPlottable;
import hep.dataforge.plots.data.DynamicPlottableSet;
import hep.dataforge.plots.fx.PlotContainer;
import hep.dataforge.plots.jfreechart.JFreeChartFrame;
import hep.dataforge.storage.api.PointLoader;
import hep.dataforge.storage.api.Storage;
import hep.dataforge.storage.commons.StorageUtils;
import hep.dataforge.tables.DataPoint;
import hep.dataforge.tables.MapPoint;
import hep.dataforge.values.Value;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tab;
import javafx.scene.layout.AnchorPane;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * FXML Controller class
 *
 * @author darksnake
 */
public class MspViewController {

    private final Context context;
    @FXML
    private AnchorPane mspPlotPane;

    public MspViewController(Context context, Tab mspTab) {
        this.context = context;
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MspView.fxml"));
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException e) {
            throw new Error(e);
        }
        mspTab.setContent(loader.getRoot());
//        this.mspPlotPane = mspContainer;
    }

    /**
     * update detector pane with new data
     */
    private void updateMspPane(DynamicPlottableSet mspData) {
        JFreeChartFrame frame = new JFreeChartFrame();
        PlotUtils.setYAxis(frame, "partial pressure", "mbar", "log");
        frame.getConfig().setValue("yAxis.range.lower", 1e-10);
        frame.getConfig().setValue("yAxis.range.upper", 1e-3);
        PlotUtils.setXAxis(frame, "time", null, "time");

        StreamSupport.stream(mspData.spliterator(), false)
                .sorted((DynamicPlottable o1, DynamicPlottable o2)
                        -> Integer.valueOf(o1.getName()).compareTo(Integer.valueOf(o2.getName()))).forEach((pl) -> frame.add(pl));
        Platform.runLater(() -> {
            mspPlotPane.getChildren().clear();
            PlotContainer container = PlotContainer.anchorTo(mspPlotPane);
            container.setPlot(frame);
        });
    }


    public List<PointLoader> listMspLoaders(Storage rootStorage) {
        return StorageUtils.loaderStream(rootStorage)
                .filter(pair -> Name.of(pair.getKey()).getLast().toString().startsWith("msp"))
                .map(pair -> pair.getValue())
                .filter(loader -> PointLoader.POINT_LOADER_TYPE.equals(loader.getType()))
                .map(loader -> (PointLoader) loader)
                .collect(Collectors.toList());
    }

    public void plotData(List<PointLoader> loaders) {
        DynamicPlottableSet plottables = new DynamicPlottableSet();
        loaders.stream()
                .flatMap(loader -> getLoaderData(loader))
                .distinct()
                .forEach(point -> {
                            for (String name : point.names()) {
                                if (!name.equals("timestamp")) {
                                    if (!plottables.hasPlottable(name)) {
                                        plottables.addPlottable(new DynamicPlottable(name, name));
                                    }
                                }
                            }
                            plottables.put(point);
                        }
                );
        updateMspPane(plottables);
    }

    private Stream<DataPoint> getLoaderData(PointLoader loader) {
        try {
            loader.open();
            List<DataPoint> points = new ArrayList<>();
//            callback.updateMessage("Loading mass spectrometer data from " + loader.getName());

            DataPoint last = null;

            for (DataPoint dp : loader) {
                points.add(dp);
                last = dp;
            }
            if (last != null) {
                points.add(terminatorPoint(last));
            }
            return points.stream();
        } catch (Exception ex) {
            LoggerFactory.getLogger(getClass()).error("Can't read msp loader data", ex);
            return Stream.empty();
        }
    }

    public void fillMspData(Storage rootStorage) {
        plotData(listMspLoaders(rootStorage));
    }

//    public void fillMspData(Storage rootStorage) {
//        if (rootStorage != null) {
//            context.taskManager().submit("viewer.msp.fill", (ProgressCallback callback) -> {
//                //                    callback.updateTitle("Fill msp data (" + rootStorage.getName() + ")");
//
//                callback.updateTitle("Load msp data (" + rootStorage.getName() + ")");
//
//                List<DataPoint> mspData = new ArrayList<>();
//
//                StorageUtils.loaderStream(rootStorage)
//                        .filter(pair -> pair.getValue() instanceof PointLoader)
//                        .filter(pair -> Name.of(pair.getKey()).getLast().toString().startsWith("msp"))
//                        .map(pair -> pair.getValue())
//                        .filter(loader -> PointLoader.POINT_LOADER_TYPE.equals(loader.getType()))
//                        .forEach(loader -> {
//                            try {
//                                PointLoader mspLoader = (PointLoader) loader;
//                                mspLoader.open();
//                                callback.updateMessage("Loading mass spectrometer data from " + mspLoader.getName());
//                                DataPoint last = null;
//                                for (DataPoint dp : mspLoader) {
//                                    mspData.add(dp);
//                                    last = dp;
//                                }
//                                if (last != null) {
//                                    mspData.add(terminatorPoint(last));
//                                }
//                            } catch (Exception ex) {
//                                LoggerFactory.getLogger(getClass()).error("Can't read msp loader data", ex);
//                            }
//                        });
//                callback.updateMessage("Loading msp data finished");
////                    return mspData;
////                    List<DataPoint> mspData = (List<DataPoint>) loadProcess.getTask().get();
//
//                if (!mspData.isEmpty()) {
//                    DynamicPlottableSet plottables = new DynamicPlottableSet();
//
//                    for (DataPoint point : mspData) {
//                        for (String name : point.names()) {
//                            if (!name.equals("timestamp")) {
//                                if (!plottables.hasPlottable(name)) {
//                                    plottables.addPlottable(new DynamicPlottable(name, name));
//                                }
//                            }
//                        }
//                        plottables.put(point);
//                    }
//
//                    updateMspPane(plottables);
//                }
//            });
//        }
//    }

    /**
     * Create a null value point to terminate msp series
     *
     * @param last
     * @return
     */
    private DataPoint terminatorPoint(DataPoint last) {
        MapPoint.Builder p = new MapPoint.Builder();
        p.putValue("timestamp", last.getValue("timestamp").timeValue().plusMillis(10));
        for (String name : last.namesAsArray()) {
            if (!name.equals("timestamp")) {
                p.putValue(name, Value.NULL);
            }
        }
        return p.build();
    }
}
