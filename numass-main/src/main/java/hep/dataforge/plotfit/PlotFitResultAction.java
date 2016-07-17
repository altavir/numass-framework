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
package hep.dataforge.plotfit;

import hep.dataforge.actions.OneToOneAction;
import hep.dataforge.context.Context;
import hep.dataforge.datafitter.FitState;
import hep.dataforge.datafitter.models.XYModel;
import hep.dataforge.description.NodeDef;
import hep.dataforge.description.TypedActionDef;
import hep.dataforge.description.ValueDef;
import hep.dataforge.exceptions.ContentException;
import hep.dataforge.io.reports.Reportable;
import hep.dataforge.meta.Laminate;
import hep.dataforge.meta.Meta;
import hep.dataforge.plots.PlotsPlugin;
import hep.dataforge.plots.XYPlotFrame;
import hep.dataforge.plots.data.PlottableData;
import hep.dataforge.plots.data.PlottableXYFunction;
import hep.dataforge.tables.PointSource;
import hep.dataforge.tables.XYAdapter;
import java.util.function.Function;
import java.util.stream.StreamSupport;

/**
 *
 * @author darksnake
 */
@TypedActionDef(name = "plotFit", info = "Plot fit result", inputType = FitState.class, outputType = FitState.class)
@NodeDef(name = "adapter", info = "adapter for DataSet being fitted. By default is taken from model.")
@ValueDef(name = "plotTitle", def = "", info = "The title of the plot.")
public class PlotFitResultAction extends OneToOneAction<FitState, FitState> {

    @Override
    protected FitState execute(Reportable log, String name, Laminate metaData, FitState input) {

        PointSource data = input.getDataSet();
        if (!(input.getModel() instanceof XYModel)) {
            log.reportError("The fit model should be instance of XYModel for this action. Action failed!");
            return input;
        }
        XYModel model = (XYModel) input.getModel();

        XYAdapter adapter;
        if (metaData.hasNode("adapter")) {
            adapter = new XYAdapter(metaData.getNode("adapter"));
        } else if (input.getModel() instanceof XYModel) {
            adapter = model.getAdapter();
        } else {
            throw new ContentException("No adapter defined for data interpretation");
        }

        Function<Double, Double> function = (x) -> model.getSpectrum().value(x, input.getParameters());

        XYPlotFrame frame = (XYPlotFrame) PlotsPlugin
                .buildFrom(getContext()).buildPlotFrame(getName(), name,
                metaData.getNode("plot", Meta.empty()));

        PlottableXYFunction fit = new PlottableXYFunction("fit");
        fit.setDensity(100, false);
        fit.setSmoothing(true);
        // ensuring all data points are calculated explicitly
        StreamSupport.stream(data.spliterator(), false)
                .map(dp -> adapter.getX(dp).doubleValue()).sorted().forEach(d -> fit.calculateIn(d));

        frame.add(fit);

        frame.add(PlottableData.plot("data", adapter, data));

        return input;
    }

}
