package inr.numass.scripts.models

import hep.dataforge.context.Global
import hep.dataforge.fx.output.FXOutputManager
import hep.dataforge.meta.buildMeta
import hep.dataforge.plots.data.DataPlot
import hep.dataforge.plots.jfreechart.JFreeChartPlugin
import hep.dataforge.stat.fit.ParamSet
import hep.dataforge.step
import inr.numass.NumassPlugin
import inr.numass.displayChart
import inr.numass.models.NBkgSpectrum
import inr.numass.models.sterile.SterileNeutrinoSpectrum

fun main() {
    NumassPlugin().startGlobal()
    JFreeChartPlugin().startGlobal()
    Global.output = FXOutputManager()



    val params = ParamSet().apply {
        setPar("N", 8e5, 6.0, 0.0, Double.POSITIVE_INFINITY)
        setPar("bkg", 2.0, 0.03)
        setPar("E0", 18575.0, 1.0)
        setPar("mnu2", 0.0, 1.0)
        setParValue("msterile2", (1000 * 1000).toDouble())
        setPar("U2", 0.0, 1e-3)
        setPar("X", 0.0, 0.01)
        setPar("trap", 1.0, 0.01)
    }




    val meta1 = buildMeta {
        "resolution.A" to 8.3e-5
    }
    val spectrum1 = NBkgSpectrum(SterileNeutrinoSpectrum(Global, meta1))

    val meta2 = buildMeta {
        "resolution.A" to 0
    }
    val spectrum2 = NBkgSpectrum(SterileNeutrinoSpectrum(Global, meta2))

    displayChart("compare").apply {
        val x = (14000.0..18600.0).step(100.0).toList()
        val y1 = x.map { spectrum1.value(it, params) }
        +DataPlot.plot("simple", x.toDoubleArray(), y1.toDoubleArray())
        val y2 = x.map { spectrum2.value(it, params) }
        +DataPlot.plot("normal", x.toDoubleArray(), y2.toDoubleArray())
        val dif = x.mapIndexed{ index, _ -> 1 - y1[index]/y2[index]  }
        +DataPlot.plot("dif", x.toDoubleArray(), dif.toDoubleArray())
    }


}