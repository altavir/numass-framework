package inr.numass.scripts.models

import hep.dataforge.context.Context
import hep.dataforge.context.Global
import hep.dataforge.fx.plots.PlotManager
import hep.dataforge.grind.Grind
import hep.dataforge.grind.GrindShell
import hep.dataforge.grind.helpers.PlotHelper
import hep.dataforge.meta.Meta
import hep.dataforge.stat.fit.ParamSet
import hep.dataforge.stat.models.XYModel
import hep.dataforge.stat.parametric.ParametricFunction
import inr.numass.NumassPlugin
import inr.numass.data.SpectrumGenerator
import inr.numass.models.GaussResolution
import inr.numass.models.NBkgSpectrum
import inr.numass.models.sterile.NumassBeta
import inr.numass.models.sterile.NumassTransmission
import inr.numass.models.sterile.SterileNeutrinoSpectrum

import static hep.dataforge.grind.Grind.morph

Context ctx = Global.instance()
ctx.getPluginManager().load(PlotManager)
ctx.getPluginManager().load(NumassPlugin)

new GrindShell(ctx).eval {
    ParametricFunction spectrum = new SterileNeutrinoSpectrum(
            ctx,
            Grind.buildMeta(useFSS: false),
            new NumassBeta(),
            new NumassTransmission(ctx, Meta.empty()),
            new GaussResolution(5d)
    )

    def model = new XYModel(Meta.empty(), new NBkgSpectrum(spectrum));

    ParamSet params = morph(ParamSet, [:], "params") {
        N(value: 1e+04, err: 30, lower: 0)
        bkg(value: 1.0, err: 0.1)
        E0(value: 18575.0, err: 0.1)
        mnu2(value: 0, err: 0.01)
        msterile2(value: 1000**2, err: 1)
        U2(value: 0.0, err: 1e-3)
        X(value: 0.0, err: 0.01, lower: 0)
        trap(value: 1.0, err: 0.05)
        w(300, err: 5)
    }

    SpectrumGenerator generator = new SpectrumGenerator(model, params, 12316);

    PlotHelper ph = plots

    ph.plot((10000..18500).step(100).collectEntries {
        [it,model.value(it,params)]
    })

//
//    def data = generator.generateData(DataModelUtils.getUniformSpectrumConfiguration(10000, 18500, 10*24*3600, 850));
//
//    FitState state = new FitState(data, model, params);
//
//    def fm = ctx.getFeature(FitManager)
//
//    def res = fm.runStage(state, "QOW", FitStage.TASK_RUN, "N", "bkg", "E0", "U2");
//
//
//    res.print(ctx.io.out());
}