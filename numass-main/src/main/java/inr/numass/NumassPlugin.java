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
package inr.numass;

import hep.dataforge.actions.ActionManager;
import hep.dataforge.context.BasicPlugin;
import hep.dataforge.context.Context;
import hep.dataforge.context.PluginDef;
import hep.dataforge.maths.MathPlugin;
import hep.dataforge.meta.Meta;
import hep.dataforge.plotfit.PlotFitResultAction;
import hep.dataforge.plots.PlotDataAction;
import hep.dataforge.plots.fx.FXPlotUtils;
import hep.dataforge.plots.fx.PlotContainer;
import hep.dataforge.plots.jfreechart.JFreeChartFrame;
import hep.dataforge.stat.fit.FitManager;
import hep.dataforge.stat.models.ModelManager;
import hep.dataforge.stat.models.WeightedXYModel;
import hep.dataforge.stat.models.XYModel;
import hep.dataforge.tables.PointAdapter;
import hep.dataforge.tables.XYAdapter;
import inr.numass.actions.*;
import inr.numass.models.*;
import inr.numass.models.sterile.SterileNeutrinoSpectrum;
import inr.numass.tasks.*;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.util.FastMath;

/**
 * @author Alexander Nozik
 */
@PluginDef(group = "inr.numass", name = "numass",
        dependsOn = {"hep.dataforge:math", "hep.dataforge:MINUIT"},
        info = "Numass data analysis tools")
public class NumassPlugin extends BasicPlugin {

    /**
     * Display a JFreeChart plot frame in a separate stage window
     *
     * @param title
     * @param width
     * @param height
     * @return
     */
    public static JFreeChartFrame displayJFreeChart(String title, double width, double height, Meta meta) {
        PlotContainer container = FXPlotUtils.displayContainer(title, width, height);
        JFreeChartFrame frame = new JFreeChartFrame(meta);
        frame.configureValue("title", title);
        container.setPlot(frame);
        return frame;
    }

    public static JFreeChartFrame displayJFreeChart(String title, Meta meta) {
        return displayJFreeChart(title, 800, 600, meta);
    }


    @Override
    public void attach(Context context) {
//        StorageManager.buildFrom(context);
        super.attach(context);
        context.pluginManager().load(new NumassIO());
        FitManager fm = context.getFeature(FitManager.class);
        loadModels(fm.getModelManager());
        loadMath(MathPlugin.buildFrom(context));

        ActionManager actions = context.pluginManager().getOrLoad(ActionManager.class);
        actions.attach(context);

        actions.putAction(MergeDataAction.class);
        actions.putAction(MonitorCorrectAction.class);
        actions.putAction(SummaryAction.class);
        actions.putAction(PlotDataAction.class);
        actions.putAction(PlotFitResultAction.class);
        actions.putAction(AdjustErrorsAction.class);
        actions.putAction(SubstractSpectrumAction.class);

        actions.putTask(NumassPrepareTask.class);
        actions.putTask(NumassTableFilterTask.class);
        actions.putTask(NumassFitScanTask.class);
        actions.putTask(NumassSubstractEmptySourceTask.class);
        actions.putTask(NumassFitScanSummaryTask.class);
        actions.putTask(NumassFitTask.class);
        actions.putTask(NumassFitSummaryTask.class);
    }

    @Override
    public void detach() {
        //TODO clean up
        super.detach();
    }

    private void loadMath(MathPlugin math) {
        math.registerBivariate("numass.trap.lowFields", (Ei, Ef) -> {
            return 3.92e-5 * FastMath.exp(-(Ei - Ef) / 300d) + 1.97e-4 - 6.818e-9 * Ei;
        });

        math.registerBivariate("numass.trap.nominal", (Ei, Ef) -> {
            //return 1.64e-5 * FastMath.exp(-(Ei - Ef) / 300d) + 1.1e-4 - 4e-9 * Ei;
            return 1.2e-4 - 4.5e-9 * Ei;
        });

        math.registerBivariate("numass.resolutionTail", meta -> {
            double alpha = meta.getDouble("tailAlpha", 0);
            double beta = meta.getDouble("tailBeta", 0);
            return (double E, double U) -> 1 - (E - U) * (alpha + E / 1000d * beta) / 1000d;
        });

        math.registerBivariate("numass.resolutionTail.2017", meta ->
                (double E, double U) -> {
                    double D = E - U;
                    return 0.99797 - 3.05346E-7 * D - 5.45738E-10 * Math.pow(D, 2) - 6.36105E-14 * Math.pow(D, 3);
                });

        math.registerBivariate("numass.resolutionTail.2017.mod", meta ->
                (double E, double U) -> {
                    double D = E - U;
                    return (0.99797 - 3.05346E-7 * D - 5.45738E-10 * Math.pow(D, 2) - 6.36105E-14 * Math.pow(D, 3)) * (1 - 5e-3 * Math.sqrt(E / 1000));
                });
    }

    /**
     * Load all numass model factories
     *
     * @param manager
     */
    private void loadModels(ModelManager manager) {

//        manager.addModel("modularbeta", (context, an) -> {
//            double A = an.getDouble("resolution", 8.3e-5);//8.3e-5
//            double from = an.getDouble("from", 14400d);
//            double to = an.getDouble("to", 19010d);
//            RangedNamedSetSpectrum beta = new BetaSpectrum(getClass().getResourceAsStream("/data/FS.txt"));
//            ModularSpectrum sp = new ModularSpectrum(beta, A, from, to);
//            NBkgSpectrum spectrum = new NBkgSpectrum(sp);
//
//            return new XYModel(spectrum, getAdapter(an));
//        });

        manager.addModel("scatter", (context, an) -> {
            double A = an.getDouble("resolution", 8.3e-5);//8.3e-5
            double from = an.getDouble("from", 0);
            double to = an.getDouble("to", 0);

            ModularSpectrum sp;
            if (from == to) {
                sp = new ModularSpectrum(new GaussSourceSpectrum(), A);
            } else {
                sp = new ModularSpectrum(new GaussSourceSpectrum(), A, from, to);
            }

            NBkgSpectrum spectrum = new NBkgSpectrum(sp);

            return new XYModel(spectrum, getAdapter(an));
        });

        manager.addModel("scatter-empiric", (context, an) -> {
            double eGun = an.getDouble("eGun", 19005d);

            TransmissionInterpolator interpolator = buildInterpolator(context, an, eGun);

            EmpiricalLossSpectrum loss = new EmpiricalLossSpectrum(interpolator, eGun + 5);
            NBkgSpectrum spectrum = new NBkgSpectrum(loss);

            double weightReductionFactor = an.getDouble("weightReductionFactor", 2.0);

            return new WeightedXYModel(spectrum, getAdapter(an), (dp) -> weightReductionFactor);
        });

        manager.addModel("scatter-empiric-variable", (context, an) -> {
            double eGun = an.getDouble("eGun", 19005d);

            //build transmisssion with given data, annotation and smoothing
            UnivariateFunction interpolator = buildInterpolator(context, an, eGun);

            VariableLossSpectrum loss = VariableLossSpectrum.withData(interpolator, eGun + 5);

            double tritiumBackground = an.getDouble("tritiumBkg", 0);

            NBkgSpectrum spectrum;
            if (tritiumBackground == 0) {
                spectrum = new NBkgSpectrum(loss);
            } else {
                spectrum = CustomNBkgSpectrum.tritiumBkgSpectrum(loss, tritiumBackground);
            }

            double weightReductionFactor = an.getDouble("weightReductionFactor", 2.0);

            WeightedXYModel res = new WeightedXYModel(spectrum, getAdapter(an), (dp) -> weightReductionFactor);
            res.setMeta(an);
            return res;
        });

        manager.addModel("scatter-analytic-variable", (context, an) -> {
            double eGun = an.getDouble("eGun", 19005d);

            VariableLossSpectrum loss = VariableLossSpectrum.withGun(eGun + 5);

            double tritiumBackground = an.getDouble("tritiumBkg", 0);

            NBkgSpectrum spectrum;
            if (tritiumBackground == 0) {
                spectrum = new NBkgSpectrum(loss);
            } else {
                spectrum = CustomNBkgSpectrum.tritiumBkgSpectrum(loss, tritiumBackground);
            }

            return new XYModel(spectrum, getAdapter(an));
        });

        manager.addModel("scatter-empiric-experimental", (context, an) -> {
            double eGun = an.getDouble("eGun", 19005d);

            //build transmisssion with given data, annotation and smoothing
            UnivariateFunction interpolator = buildInterpolator(context, an, eGun);

            double smoothing = an.getDouble("lossSmoothing", 0.3);

            VariableLossSpectrum loss = ExperimentalVariableLossSpectrum.withData(interpolator, eGun + 5, smoothing);

            NBkgSpectrum spectrum = new NBkgSpectrum(loss);

            double weightReductionFactor = an.getDouble("weightReductionFactor", 2.0);

            WeightedXYModel res
                    = new WeightedXYModel(spectrum, getAdapter(an), (dp) -> weightReductionFactor);
            res.setMeta(an);
            return res;
        });

        manager.addModel("sterile", (context, meta) -> {
            SterileNeutrinoSpectrum sp = new SterileNeutrinoSpectrum(context, meta);
            NBkgSpectrum spectrum = new NBkgSpectrum(sp);

            return new XYModel(spectrum, getAdapter(meta));
        });

        manager.addModel("gun", (context, an) -> {
            GunSpectrum gsp = new GunSpectrum();

            double tritiumBackground = an.getDouble("tritiumBkg", 0);

            NBkgSpectrum spectrum;
            if (tritiumBackground == 0) {
                spectrum = new NBkgSpectrum(gsp);
            } else {
                spectrum = CustomNBkgSpectrum.tritiumBkgSpectrum(gsp, tritiumBackground);
            }

            return new XYModel(spectrum, getAdapter(an));
        });

    }

    private TransmissionInterpolator buildInterpolator(Context context, Meta an, double eGun) {
        String transXName = an.getString("transXName", "Uset");
        String transYName = an.getString("transYName", "CR");

        double stitchBorder = an.getDouble("stitchBorder", eGun - 7);
        int nSmooth = an.getInt("nSmooth", 15);

        double w = an.getDouble("w", 0.8);

        if (an.hasValue("transFile")) {
            String transmissionFile = an.getString("transFile");

            return TransmissionInterpolator
                    .fromFile(context, transmissionFile, transXName, transYName, nSmooth, w, stitchBorder);
        } else if (an.hasMeta("transBuildAction")) {
            Meta transBuild = an.getMeta("transBuildAction");
            try {
                return TransmissionInterpolator.fromAction(context,
                        transBuild, transXName, transYName, nSmooth, w, stitchBorder);
            } catch (InterruptedException ex) {
                throw new RuntimeException("Transmission build failed");
            }
        } else {
            throw new RuntimeException("Transmission declaration not found");
        }
    }

    private XYAdapter getAdapter(Meta an) {
        if (an.hasMeta(PointAdapter.DATA_ADAPTER_KEY)) {
            return new XYAdapter(an.getMeta(PointAdapter.DATA_ADAPTER_KEY));
        } else {
            return new XYAdapter("Uread", "CR", "CRerr");
        }
    }
}
