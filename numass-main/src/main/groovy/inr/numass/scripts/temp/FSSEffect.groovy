package inr.numass.scripts.temp

import hep.dataforge.context.Context
import hep.dataforge.context.Global
import hep.dataforge.grind.Grind
import hep.dataforge.grind.GrindShell
import hep.dataforge.grind.helpers.PlotHelper
import hep.dataforge.plots.fx.FXPlotManager
import hep.dataforge.stat.fit.ParamSet
import hep.dataforge.utils.MetaMorph
import hep.dataforge.values.Values
import inr.numass.NumassPlugin
import inr.numass.models.FSS
import inr.numass.models.sterile.NumassBeta

Context ctx = Global.instance()
ctx.pluginManager().load(FXPlotManager)
ctx.pluginManager().load(NumassPlugin.class)


new GrindShell(ctx).eval {
    def fssStream = getClass().getResourceAsStream("/data/FS.txt")
    def fss = new FSS(fssStream)

    def beta = new NumassBeta();

    def params = MetaMorph.morph(ParamSet,
            Grind.buildMeta("params") {
                E0(value: 18575, err: 0.1)
                mnu2(value: 0, err: 0.01)
                msterile2(value: 1000**2, err: 1)
                U2(value: 0, err: 1e-3)
            }
    )

    def fsBeta = { double eIn, Values set ->
        double res = 0;
        for (int i = 0; i < fss.size(); i++) {
            res += fss.getP(i) * beta.value(fss.getE(i), eIn, set);
        }
        return res;
    };


    def xs = (1..400).collect{18500 + it/4}

    def noFssPoints = xs.collect { beta.value(0,it, params) }
    def fssPoints = xs.collect { fsBeta(it, params) }


    (plots as PlotHelper).plot(xs,fssPoints,"FSS")
    (plots as PlotHelper).plot(xs,noFssPoints,"noFSS")

    println "U\tFSS\tnoFSS"
    for(i in 0..399){
        println "${xs[i]}\t${fssPoints[i]}\t${noFssPoints[i]}"
    }
}