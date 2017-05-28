package inr.numass.scripts

import hep.dataforge.context.Context
import hep.dataforge.context.Global
import hep.dataforge.grind.Grind
import hep.dataforge.grind.helpers.PlotHelper
import hep.dataforge.plots.fx.FXPlotManager
import inr.numass.models.sterile.NumassResolution
import javafx.application.Platform

Context ctx = Global.instance()
ctx.pluginManager().load(FXPlotManager)

PlotHelper plot = new PlotHelper(ctx);

def resolution1 = new NumassResolution(
        ctx,
        Grind.buildMeta("resolution", width: 8.3e-5, tail: "(0.99797 - 3.05346E-7*D - 5.45738E-10 * D**2 - 6.36105E-14 * D**3)")
)

plot.plot(from: 13500, to: 19000) { x ->
    resolution1.value(x, 14000, null)
}

def resolution2 = new NumassResolution(
        ctx,
        Grind.buildMeta("resolution",
                width: 8.3e-5,
                tail: "(0.99797 - 3.05346E-7*D - 5.45738E-10 * D**2 - 6.36105E-14 * D**3)*(1-5e-3*sqrt(E/1000))"
        )
)

plot.plot(from: 13500, to: 19000) { x ->
    resolution2.value(x, 14000, null)
}


Platform.setImplicitExit(true)

