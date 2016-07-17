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
import hep.dataforge.datafitter.FitManager;
import hep.dataforge.datafitter.FitPlugin;
import hep.dataforge.datafitter.models.ModelManager;
import hep.dataforge.datafitter.models.WeightedXYModel;
import hep.dataforge.datafitter.models.XYModel;
import hep.dataforge.meta.Meta;
import hep.dataforge.plotfit.PlotFitResultAction;
import hep.dataforge.plots.PlotDataAction;
import hep.dataforge.tables.PointAdapter;
import hep.dataforge.tables.XYAdapter;
import inr.numass.actions.AdjustErrorsAction;
import inr.numass.actions.FindBorderAction;
import inr.numass.actions.MergeDataAction;
import inr.numass.actions.MonitorCorrectAction;
import inr.numass.actions.PrepareDataAction;
import inr.numass.actions.ReadNumassDataAction;
import inr.numass.actions.ReadNumassStorageAction;
import inr.numass.actions.ShowEnergySpectrumAction;
import inr.numass.actions.ShowLossSpectrumAction;
import inr.numass.actions.SlicingAction;
import inr.numass.actions.SummaryAction;
import inr.numass.models.BetaSpectrum;
import inr.numass.models.CustomNBkgSpectrum;
import inr.numass.models.EmpiricalLossSpectrum;
import inr.numass.models.ExperimentalVariableLossSpectrum;
import inr.numass.models.GaussSourceSpectrum;
import inr.numass.models.GunSpectrum;
import inr.numass.models.ModularSpectrum;
import inr.numass.models.NBkgSpectrum;
import inr.numass.models.RangedNamedSetSpectrum;
import inr.numass.models.ResolutionFunction;
import inr.numass.models.TransmissionInterpolator;
import inr.numass.models.VariableLossSpectrum;
import org.apache.commons.math3.analysis.BivariateFunction;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.util.FastMath;

/**
 *
 * @author Alexander Nozik
 */
@PluginDef(group = "inr.numass", name = "numass",
        dependsOn = {"hep.dataforge:MINUIT", "hep.dataforge:plots"},
        description = "Numass data analysis tools")
public class NumassPlugin extends BasicPlugin {

    @Override
    public void attach(Context context) {
//        StorageManager.buildFrom(context);
        FitManager fm = context.provide("fitting", FitPlugin.class).getFitManager();
        ModelManager mm = fm.getModelManager();
        loadModels(mm);

        ActionManager actions = ActionManager.buildFrom(context);
        actions.registerAction(SlicingAction.class);
        actions.registerAction(PrepareDataAction.class);
        actions.registerAction(ReadNumassDataAction.class);
        actions.registerAction(MergeDataAction.class);
        actions.registerAction(FindBorderAction.class);
        actions.registerAction(MonitorCorrectAction.class);
        actions.registerAction(SummaryAction.class);
        actions.registerAction(PlotDataAction.class);
        actions.registerAction(PlotFitResultAction.class);
        actions.registerAction(ShowLossSpectrumAction.class);
        actions.registerAction(AdjustErrorsAction.class);
        actions.registerAction(ReadNumassStorageAction.class);
        actions.registerAction(ShowEnergySpectrumAction.class);
    }

    @Override
    public void detach() {

    }

    /**
     * Load all numass model factories
     *
     * @param manager
     */
    private void loadModels(ModelManager manager) {

        manager.addModel("modularbeta", (context, an) -> {
            double A = an.getDouble("resolution", 8.3e-5);//8.3e-5
            double from = an.getDouble("from", 14400d);
            double to = an.getDouble("to", 19010d);
            RangedNamedSetSpectrum beta = new BetaSpectrum(context.io().getFile("FS.txt"));
            ModularSpectrum sp = new ModularSpectrum(beta, A, from, to);
            NBkgSpectrum spectrum = new NBkgSpectrum(sp);

            return new XYModel(spectrum, getAdapter(an));
        });

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

        manager.addModel("sterile", (context, an) -> {
            double A = an.getDouble("resolution", an.getDouble("resolution.width", 8.3e-5));//8.3e-5
            double from = an.getDouble("from", 13900d);
            double to = an.getDouble("to", 18700d);
            context.getReport().report("Setting up tritium model with real transmission function");
            BivariateFunction resolutionTail;
            if (an.hasValue("resolution.tailAlpha")) {
                resolutionTail = ResolutionFunction.getAngledTail(an.getDouble("resolution.tailAlpha"), an.getDouble("resolution.tailBeta", 0));
            } else {
                resolutionTail = ResolutionFunction.getRealTail();
            }
            RangedNamedSetSpectrum beta = new BetaSpectrum(context.io().getFile("FS.txt"));
            ModularSpectrum sp = new ModularSpectrum(beta, new ResolutionFunction(A, resolutionTail), from, to);
            if (an.getBoolean("caching", false)) {
                context.getReport().report("Caching turned on");
                sp.setCaching(true);
            }
            //Adding trapping energy dependence
            //Intercept = 4.95745, B1 = -0.36879, B2 = 0.00827
            //sp.setTrappingFunction((Ei,Ef)->LossCalculator.getTrapFunction().value(Ei, Ef)*(4.95745-0.36879*Ei+0.00827*Ei*Ei));
            sp.setTrappingFunction((Ei, Ef) -> {
                return 6.2e-5 * FastMath.exp(-(Ei - Ef) / 350d) + 1.97e-4 - 6.818e-9 * Ei;
            });
            context.getReport().report("Using folowing trapping formula: {}",
                    "6.2e-5 * FastMath.exp(-(Ei - Ef) / 350d) + 1.97e-4 - 6.818e-9 * Ei");
            NBkgSpectrum spectrum = new NBkgSpectrum(sp);

            return new XYModel(spectrum, getAdapter(an));
        });

        manager.addModel("modularbeta-unadeabatic", (context, an) -> {
            double A = an.getDouble("resolution", 8.3e-5);//8.3e-5
            double from = an.getDouble("from", 14400d);
            double to = an.getDouble("to", 19010d);
            BivariateFunction reolutionTail = (double E, double U) -> {
                double x = E - U;
                if (x > 1500) {
                    return 0.98;
                } else //Intercept = 1.00051, Slope = -1.3552E-5
                {
                    return 1.00051 - 1.3552E-5 * x;
                }
            };
            RangedNamedSetSpectrum beta = new BetaSpectrum(context.io().getFile("FS.txt"));
            ModularSpectrum sp = new ModularSpectrum(beta, new ResolutionFunction(A, reolutionTail), from, to);
            NBkgSpectrum spectrum = new NBkgSpectrum(sp);

            return new XYModel(spectrum, getAdapter(an));
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
        } else if (an.hasNode("transBuildAction")) {
            Meta transBuild = an.getNode("transBuildAction");
            try {
                return TransmissionInterpolator.fromAction((Context) context,
                        transBuild, transXName, transYName, nSmooth, w, stitchBorder);
            } catch (InterruptedException ex) {
                throw new RuntimeException("Transmission build failed");
            }
        } else {
            throw new RuntimeException("Transmission declaration not found");
        }
    }

    private XYAdapter getAdapter(Meta an) {
        if (an.hasNode(PointAdapter.DATA_ADAPTER_ANNOTATION_NAME)) {
            return new XYAdapter(an.getNode(PointAdapter.DATA_ADAPTER_ANNOTATION_NAME));
        } else {
            return new XYAdapter("Uread", "CR", "CRerr");
        }
    }
}
