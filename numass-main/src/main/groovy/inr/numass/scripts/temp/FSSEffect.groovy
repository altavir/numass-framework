package inr.numass.scripts.temp

import hep.dataforge.context.Context
import hep.dataforge.context.Global
import hep.dataforge.grind.Grind
import hep.dataforge.grind.GrindShell
import hep.dataforge.grind.helpers.PlotHelper
import hep.dataforge.meta.Meta
import hep.dataforge.plots.fx.FXPlotManager
import hep.dataforge.stat.fit.ParamSet
import hep.dataforge.utils.MetaMorph
import inr.numass.NumassPlugin
import inr.numass.models.sterile.SterileNeutrinoSpectrum

Context ctx = Global.instance()
ctx.pluginManager().load(FXPlotManager)
ctx.pluginManager().load(NumassPlugin.class)


new GrindShell(ctx).eval {
    SterileNeutrinoSpectrum sp1 = new SterileNeutrinoSpectrum(context, Meta.empty());
    SterileNeutrinoSpectrum sp2 = new SterileNeutrinoSpectrum(context, Grind.buildMeta(useFSS: false));

    def params = MetaMorph.morph(ParamSet,
            Grind.buildMeta("params") {
                N(value: 6e5, err: 1e5, lower: 0)
                bkg(value: 2, err: 0.1)
                E0(value: 18575, err: 0.1)
                mnu2(value: 0, err: 0.01)
                msterile2(value: 1000**2, err: 1)
                U2(value: 0, err: 1e-3)
                X(value: 0, err: 0.01, lower: 0)
                trap(value: 0, err: 0.05)
            }
    )


    def xs = (1..400).collect{18000 + it*2}

    def sp1Points = xs.collect { sp1.value(it, params) }
    def sp2Points = xs.collect { sp2.value(it, params) }


    (plots as PlotHelper).plot(xs,sp1Points,"FSS")
    (plots as PlotHelper).plot(xs,sp2Points,"noFSS")
}