package inr.numass.models.mc

import hep.dataforge.context.Global
import hep.dataforge.fx.plots.display
import hep.dataforge.maths.chain.Chain
import hep.dataforge.meta.buildMeta
import hep.dataforge.plots.XYFunctionPlot
import hep.dataforge.plots.jfreechart.chart
import hep.dataforge.stat.PolynomialDistribution
import hep.dataforge.stat.fit.ParamSet
import inr.numass.NumassPlugin
import inr.numass.models.sterile.SterileNeutrinoSpectrum

fun sampleBeta(params: ParamSet): Chain<Double> {
    TODO()
}


fun main(args: Array<String>) {
    NumassPlugin().startGlobal()
    val meta = buildMeta("model") {
        "fast" to true
        node("resolution") {
            "width" to 1.22e-4
            "tailAlpha" to 1e-2
        }
    }
    val allPars = ParamSet()
            .setPar("N", 7e+05, 1.8e+03, 0.0, java.lang.Double.POSITIVE_INFINITY)
            .setPar("bkg", 1.0, 0.050)
            .setPar("E0", 18575.0, 1.4)
            .setPar("mnu2", 0.0, 1.0)
            .setPar("msterile2", 1000.0 * 1000.0, 0.0)
            .setPar("U2", 0.0, 1e-4, -1.0, 1.0)
            .setPar("X", 0.0, 0.01, 0.0, java.lang.Double.POSITIVE_INFINITY)
            .setPar("trap", 1.0, 0.01, 0.0, java.lang.Double.POSITIVE_INFINITY)

    val sp = SterileNeutrinoSpectrum(Global, meta)

    val spectrumPlot = XYFunctionPlot.plot("spectrum", 14000.0, 18600.0, 500) {
        sp.value(it, allPars)
    }

    val distribution = PolynomialDistribution(0.0, 5000.0, 3.0);

    val distributionPlot = XYFunctionPlot.plot("distribution", 14000.0, 18500.0, 500) {
        50 * distribution.density(18600.0 - it)
    }

    Global.display {
        chart {
            add(spectrumPlot)
            add(distributionPlot)
        }
    }
}