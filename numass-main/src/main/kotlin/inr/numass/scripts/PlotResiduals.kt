package inr.numass.scripts

import hep.dataforge.buildContext
import hep.dataforge.fx.output.FXOutputManager
import hep.dataforge.meta.buildMeta
import hep.dataforge.plots.jfreechart.JFreeChartPlugin
import hep.dataforge.stat.fit.ParamSet
import inr.numass.NumassPlugin
import inr.numass.data.NumassDataUtils
import inr.numass.data.analyzers.SmartAnalyzer
import inr.numass.data.api.NumassSet
import inr.numass.data.storage.NumassDirectory
import inr.numass.models.NBkgSpectrum
import inr.numass.models.sterile.SterileNeutrinoSpectrum

fun main(args: Array<String>) {

    val context = buildContext("NUMASS", NumassPlugin::class.java, JFreeChartPlugin::class.java) {
        output = FXOutputManager()
        rootDir = "D:\\Work\\Numass\\sterile\\2017_11"
        dataDir = "D:\\Work\\Numass\\data\\2017_11"
    }

    val modelMeta = buildMeta("model") {
        "modelName" to "sterile"
        "resolution" to {
            "width" to 8.3e-5
            "tail" to "function::numass.resolutionTail.2017.mod"
        }
        "transmission" to {
            "trapping" to "function::numass.trap.nominal"
        }
    }

    val spectrum = NBkgSpectrum(SterileNeutrinoSpectrum(context, modelMeta))

    val params = ParamSet().apply {
        setPar("N", 676844.0, 6.0, 0.0, Double.POSITIVE_INFINITY)
        setPar("bkg", 2.0, 0.03)
        setPar("E0", 18575.0, 1.0)
        setPar("mnu2", 0.0, 1.0)
        setParValue("msterile2", (1000 * 1000).toDouble())
        setPar("U2", 0.0, 1e-3)
        setPar("X", 0.1, 0.01)
        setPar("trap", 1.0, 0.01)
    }
    /*
    'N'	= 676844 Â± 8.5e+02	(0.00000,Infinity)
'L'	= 0 Â± 1.0
'Q'	= 0 Â± 1.0
'bkg'	= 0.0771 Â± 0.065
'E0'	= 18561.44 Â± 0.77
'mnu2'	= 0.00 Â± 0.010
'msterile2'	= 1000000.00 Â± 1.0
'U2'	= 0.000 Â± 0.0010
'X'	= 0.05000 Â± 0.010	(0.00000,Infinity)
'trap'	= 1.000 Â± 0.050
   */


    val storage = NumassDirectory.read(context, "Fill_2")!!

    val sets = (36..42).map { "set_$it" }

    val loaders = sets.mapNotNull { set ->
        storage.provide(set, NumassSet::class.java).orElse(null)
    }

    val set = NumassDataUtils.join("sum", loaders)


    val analyzer = SmartAnalyzer()

    val meta = buildMeta {
        "t0" to 15e3
        "window.lo" to 450
        "window.up" to 3000
    }
}