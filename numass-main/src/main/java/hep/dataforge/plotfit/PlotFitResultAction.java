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
import hep.dataforge.description.NodeDef;
import hep.dataforge.description.TypedActionDef;
import hep.dataforge.description.ValueDef;
import hep.dataforge.meta.Laminate;
import hep.dataforge.meta.Meta;
import hep.dataforge.plots.PlotFrame;
import hep.dataforge.plots.PlotUtils;
import hep.dataforge.plots.data.PlotData;
import hep.dataforge.plots.data.PlotXYFunction;
import hep.dataforge.stat.fit.FitResult;
import hep.dataforge.stat.fit.FitState;
import hep.dataforge.stat.models.XYModel;
import hep.dataforge.tables.NavigablePointSource;
import hep.dataforge.tables.XYAdapter;

import java.util.function.Function;
import java.util.stream.StreamSupport;

/**
 * @author darksnake
 */
@TypedActionDef(name = "plotFit", info = "Plot fit result", inputType = FitState.class, outputType = FitState.class)
@NodeDef(name = "adapter", info = "adapter for DataSet being fitted. By default is taken from model.")
@ValueDef(name = "plotTitle", def = "", info = "The title of the plot.")
public class PlotFitResultAction extends OneToOneAction<FitResult, FitResult> {

    @Override
    protected FitResult execute(Context context, String name, FitResult input, Laminate metaData) {

        FitState state = input.optState().orElseThrow(()->new UnsupportedOperationException("Can't work with fit result not containing state, sorry! Will fix it later"));

        NavigablePointSource data = input.getData();
        if (!(state.getModel() instanceof XYModel)) {
            context.getChronicle(name).reportError("The fit model should be instance of XYModel for this action. Action failed!");
            return input;
        }
        XYModel model = (XYModel) state.getModel();

        XYAdapter adapter;
        if (metaData.hasMeta("adapter")) {
            adapter = new XYAdapter(metaData.getMeta("adapter"));
        } else if (state.getModel() instanceof XYModel) {
            adapter = model.getAdapter();
        } else {
            throw new RuntimeException("No adapter defined for data interpretation");
        }

        Function<Double, Double> function = (x) -> model.getSpectrum().value(x, input.getParameters());

        PlotFrame frame = PlotUtils.getPlotManager(context)
                .getPlotFrame(getName(), name, metaData.getMeta("plot", Meta.empty()));

        PlotXYFunction fit = new PlotXYFunction("fit");
        fit.setDensity(100, false);
        fit.setSmoothing(true);
        // ensuring all data points are calculated explicitly
        StreamSupport.stream(data.spliterator(), false)
                .map(dp -> adapter.getX(dp).doubleValue()).sorted().forEach(fit::calculateIn);

        frame.add(fit);

        frame.add(PlotData.plot("data", adapter, data));

        return input;
    }

}
