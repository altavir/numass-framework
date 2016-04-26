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
package inr.numass.prop.ar;

import hep.dataforge.actions.OneToOneAction;
import hep.dataforge.context.Context;
import hep.dataforge.tables.DataPoint;
import hep.dataforge.tables.ListTable;
import hep.dataforge.tables.MapPoint;
import hep.dataforge.datafitter.FitManager;
import hep.dataforge.datafitter.FitPlugin;
import hep.dataforge.datafitter.FitState;
import hep.dataforge.datafitter.ParamSet;
import hep.dataforge.datafitter.models.Model;
import hep.dataforge.datafitter.models.XYModel;
import hep.dataforge.description.TypedActionDef;
import hep.dataforge.description.ValueDef;
import hep.dataforge.io.ColumnedDataWriter;
import hep.dataforge.io.log.Log;
import hep.dataforge.io.log.Logable;
import hep.dataforge.meta.Meta;
import inr.numass.prop.PoissonAdapter;
import inr.numass.prop.SplitNormalSpectrum;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import hep.dataforge.tables.Table;

/**
 *
 * @author Darksnake
 */
@TypedActionDef(name = "fitJNA", inputType = JNAEpisode.class, outputType = Table.class, description = "Fit JNA data by apropriate model")
@ValueDef(name = "saveResult", type = "BOOLEAN", def = "true", info = "Save the results of action to a file")
@ValueDef(name = "suffix", def = "", info = "Suffix for saved file")
@ValueDef(name = "loFitChanel", type = "NUMBER", def = "600", info = "Lo chanel to filter data for fit")
@ValueDef(name = "upFitChanel", type = "NUMBER", def = "1100", info = "Up chanel to filter data for fit")
public class FitJNAData extends OneToOneAction<JNAEpisode, Table> {

    private final FitManager fm;

    public FitJNAData(Context context, Meta annotation) {
        super(context, annotation);
        
        if (context.provides("hep.dataforge:fitting")) {
            this.fm = context.provide("hep.dataforge:fitting", FitPlugin.class).getFitManager();
        } else {
            this.fm = new FitManager(context);
        }
    }

    @Override
    protected Table execute(Logable log, Meta meta, JNAEpisode input){
        List<DataPoint> res = new ArrayList<>(input.size());

        Model model = buildModel();

        for (JNASpectrum spectrum : input) {
            Log fitLog = new Log("fit", log);
            FitState state = fit(fitLog, spectrum, model);

            ParamSet pars = state.getParameters();
            double pos = pars.getValue("pos");
            double posErr = pars.getError("pos");

            double amp = pars.getValue("amp");
            double ampErr = pars.getError("amp");

            double sigma = pars.getValue("sigma");
            double sigmaErr = pars.getError("sigma");

            double dsigma = pars.getValue("dsigma");
            double dsigmaErr = pars.getError("dsigma");

            double chi2 = state.getChi2() / (state.getDataSize() - 4);

            MapPoint point = new MapPoint(
                    new String[]{"name", "time", "pos", "posErr", "amp", "ampErr", "sigma", "sigmaErr", "dsigma", "dsigmaErr", "chi2"},
                    new Object[]{spectrum.getName(), spectrum.startTime(), pos, posErr, amp, ampErr, sigma, sigmaErr, dsigma, dsigmaErr, chi2});
            if (spectrum.hasTemperature()) {
                point = point.join(spectrum.getTemperatures());
            }
            res.add(point);
        }

        Table data = new ListTable(input.getName(), input.meta(), res);

        if (meta.getBoolean("saveResult")) {
            String suffix = meta.getString("suffix");
            OutputStream out = getContext().io().out(getName(), input.getName() + suffix);
            ColumnedDataWriter.writeDataSet(out, data, "***RESULT***");
        }

        return data;
    }

    public FitState fit(Logable log, JNASpectrum spectrum, Model model) {
        Meta reader = readMeta(spectrum.meta());
        double lowerChanel = reader.getDouble("loFitChanel");
        double upperChanel = reader.getDouble("upFitChanel");
        Table data = spectrum.asDataSet().filter("chanel", lowerChanel, upperChanel);
        ParamSet params = new ParamSet()
                .setPar("amp", 2e5, 1e3)
                .setPar("pos", 800d, 1d)
                .setPar("sigma", 100d, 1d)
                .setPar("dsigma", 0d, 1e-1);

        params.updateFrom(ParamSet.fromAnnotation(meta()));//Updating parameters forma action meta
        params.updateFrom(ParamSet.fromAnnotation(spectrum.meta()));//Updating parameters from content meta

        FitState state = new FitState(data, model, params);
//        return fm.runDefaultTask(state, log);
        return fm.runTask(state, "MINUIT", "fit", log);
    }

    private Model buildModel() {
        return new XYModel("split-normal", new SplitNormalSpectrum(), new PoissonAdapter("chanel", "count"));
    }

}
