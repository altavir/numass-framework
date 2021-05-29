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
package hep.dataforge.plots.demo;

import hep.dataforge.meta.MetaBuilder;
import hep.dataforge.plots.data.DataPlot;
import hep.dataforge.plots.data.XYFunctionPlot;
import hep.dataforge.plots.jfreechart.JFreeChartFrame;
import hep.dataforge.tables.Adapters;
import hep.dataforge.tables.Table;
import hep.dataforge.tables.Tables;
import hep.dataforge.values.ValueMap;
import hep.dataforge.values.Values;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Alexander Nozik
 */
public class JFreeFXTest extends Application {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();

        JFreeChartFrame frame = new JFreeChartFrame();
        root.setCenter(frame.getFxNode());

        XYFunctionPlot funcPlot = XYFunctionPlot.Companion.plot("func", 0.1, 4, 200, (x1) -> x1 * x1);

        frame.add(funcPlot);

        String[] names = {"myX", "myY", "myXErr", "myYErr"};

        List<Values> data = new ArrayList<>();
        data.add(ValueMap.of(names, 0.5d, 0.2, 0.1, 0.1));
        data.add(ValueMap.of(names, 1d, 1d, 0.2, 0.5));
        data.add(ValueMap.of(names, 3d, 7d, 0, 0.5));
        Table ds = Tables.infer(data);

        DataPlot dataPlot = DataPlot.plot("dataPlot", ds, Adapters.buildXYAdapter("myX", "myY", "myXErr", "myYErr"));

        frame.getConfig().putNode(new MetaBuilder("yAxis").putValue("logScale", true));

        frame.add(dataPlot);

        Scene scene = new Scene(root, 800, 600);

        stage.setTitle("my frame");
        stage.setScene(scene);
        stage.show();
    }

}
