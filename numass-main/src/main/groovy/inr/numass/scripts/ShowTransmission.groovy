package inr.numass.scripts

import hep.dataforge.context.Context
import hep.dataforge.context.Global
import hep.dataforge.grind.GrindShell
import hep.dataforge.grind.helpers.PlotHelper
import hep.dataforge.plots.fx.FXPlotManager
import hep.dataforge.stat.fit.ParamSet
import inr.numass.NumassPlugin
import inr.numass.models.sterile.NumassResolution
import inr.numass.models.sterile.SterileNeutrinoSpectrum

import static hep.dataforge.grind.Grind.buildMeta
import static hep.dataforge.grind.Grind.morph

Context ctx = Global.instance()
ctx.pluginManager().load(FXPlotManager)
ctx.pluginManager().load(NumassPlugin.class)

GrindShell shell = new GrindShell(ctx)

shell.eval {
    PlotHelper plot = plots


    ParamSet params = morph(ParamSet, "params") {
        N(value: 2.7e+06, err: 30, lower: 0)
        bkg(value: 5.0, err: 0.1)
        E0(value: 18575.0, err: 0.1)
        mnu2(value: 0, err: 0.01)
        msterile2(value: 1000**2, err: 1)
        U2(value: 0.0, err: 1e-3)
        X(value: 0.0, err: 0.01, lower: 0)
        trap(value: 1.0, err: 0.05)
    }

    def meta1 = buildMeta {
        resolution(width: 8.3e-5, tail: "(0.99797 - 3.05346E-7*D - 5.45738E-10 * D**2 - 6.36105E-14 * D**3)")
    }

    def meta2 = buildMeta {
        resolution(width: 8.3e-5, tail: "(0.99797 - 3.05346E-7*D - 5.45738E-10 * D**2 - 6.36105E-14 * D**3)*(1-5e-3*sqrt(E/1000))")
    }

    def resolution1 = new NumassResolution(
            ctx,
            meta1.getMeta("resolution")
    )

    def resolution2 = new NumassResolution(
            ctx,
            meta2.getMeta("resolution")
    )

    plot.plot(frame: "resolution", from: 13500, to: 19000) { x ->
        resolution1.value(x, 14000, params)
    }

    plot.plot(frame: "resolution", from: 13500, to: 19000) { x ->
        resolution2.value(x, 14000, params)
    }

    def spectrum1 = new SterileNeutrinoSpectrum(ctx, meta1)
    def spectrum2 = new SterileNeutrinoSpectrum(ctx, meta2)

    def x = []
    def y1 = []
    def y2 = []
    (13500..19000).step(100).each {
        x << it
        y1 << spectrum1.value(it, params)
        y2 << spectrum2.value(it, params)
    }

    plot.plot(x, y1, "spectrum1", "spectrum")
    plot.plot(x, y2, "spectrum2", "spectrum")

}

