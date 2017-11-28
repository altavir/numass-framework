package inr.numass.scripts.models

import hep.dataforge.context.Context
import hep.dataforge.context.Global
import hep.dataforge.fx.plots.PlotManager
import hep.dataforge.grind.GrindShell
import hep.dataforge.grind.helpers.PlotHelper
import hep.dataforge.meta.Meta
import hep.dataforge.stat.fit.FitManager
import hep.dataforge.stat.fit.FitStage
import hep.dataforge.stat.fit.FitState
import hep.dataforge.stat.fit.ParamSet
import hep.dataforge.stat.models.XYModel
import hep.dataforge.stat.parametric.ParametricFunction
import hep.dataforge.tables.Table
import inr.numass.NumassPlugin
import inr.numass.data.SpectrumAdapter
import inr.numass.data.SpectrumGenerator
import inr.numass.models.NBkgSpectrum
import inr.numass.models.NumassModelsKt
import inr.numass.models.misc.Gauss
import inr.numass.models.sterile.NumassBeta
import inr.numass.utils.DataModelUtils

import static hep.dataforge.grind.Grind.morph

Context ctx = Global.instance()
ctx.getPluginManager().load(PlotManager)
ctx.getPluginManager().load(NumassPlugin)

new GrindShell(ctx).eval {
    def beta = new NumassBeta().getSpectrum(0)
    def response = new Gauss(5.0)
    ParametricFunction spectrum = NumassModelsKt.convolute(beta, response)

    def model = new XYModel(Meta.empty(), new SpectrumAdapter(Meta.empty()), new NBkgSpectrum(spectrum));

    ParamSet params = morph(ParamSet, [:], "params") {
        N(value: 1e+12, err: 30, lower: 0)
        bkg(value: 5.0, err: 0.1)
        E0(value: 18575.0, err: 0.1)
        mnu2(value: 0, err: 0.01)
        msterile2(value: 1000**2, err: 1)
        U2(value: 0.0, err: 1e-3)
        //X(value: 0.0, err: 0.01, lower: 0)
        //trap(value: 1.0, err: 0.05)
        w(value: 150, err: 5)
        //shift(value: 1, err: 1e-2)
        tail(value: 1e-4, err: 1e-5)
    }

    SpectrumGenerator generator = new SpectrumGenerator(model, params, 12316);

    PlotHelper ph = plots

    ph.plot(data: (2000..19500).step(50).collectEntries { [it, model.value(it, params)] }, name: "spectrum")
            .configure(showLine: true, showSymbol: false, showErrors: false, thickness: 2, connectionType: "spline", color: "red")


    Table data = generator.generateData(DataModelUtils.getUniformSpectrumConfiguration(10000, 19500, 1, 950));

    //params.setParValue("w", 151)
    //params.setParValue("X", 0.01)
    //params.setParValue("trap", 0.01)
    //params.setParValue("mnu2", 4)

    ph.plot(data: (2000..19500).step(50).collectEntries { [it, model.value(it, params)] }, name: "spectrum-mod")
            .configure(showLine: true, showSymbol: false, showErrors: false, thickness: 2, connectionType: "spline", color: "green")

    ph.plot(data: data, adapter: new SpectrumAdapter())
            .configure(color: "blue")

    FitState state = new FitState(data, model, params);

    def fm = ctx.getFeature(FitManager)

    def res = fm.runStage(state, "QOW", FitStage.TASK_RUN, "N", "bkg", "E0", "U2");


    res.printState(ctx.io.out().newPrintWriter());
}