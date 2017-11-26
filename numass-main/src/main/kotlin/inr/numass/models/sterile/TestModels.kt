/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.models.sterile

import hep.dataforge.context.Context
import hep.dataforge.maths.MathPlugin
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder
import hep.dataforge.stat.fit.ParamSet
import hep.dataforge.stat.parametric.ParametricFunction
import inr.numass.Numass
import inr.numass.models.BetaSpectrum
import inr.numass.models.ModularSpectrum
import inr.numass.models.NBkgSpectrum
import inr.numass.models.ResolutionFunction
import org.apache.commons.math3.analysis.BivariateFunction

/**
 *
 * @author Alexander Nozik
 */
object TestModels {

    /**
     * @param args the command line arguments
     */
    @JvmStatic
    fun main(args: Array<String>) {
        val context = Numass.buildContext()
        /*
        		<model modelName="sterile" fssFile="FS.txt">
			<resolution width = "1.22e-4" tailAlpha="1e-2"/>
			<transmission trapping="numass.trap2016"/>
		</model>
         */
        val meta = MetaBuilder("model")
                .putNode(MetaBuilder("resolution")
                        .putValue("width", 1.22e-4)
                        .putValue("tailAlpha", 1e-2)
                )
                .putNode(MetaBuilder("transmission")
                        .putValue("trapping", "numass.trap2016")
                )
        val oldFunc = oldModel(context, meta)
        val newFunc = newModel(context, meta)

        val allPars = ParamSet()
                .setPar("N", 7e+05, 1.8e+03, 0.0, java.lang.Double.POSITIVE_INFINITY)
                .setPar("bkg", 1.0, 0.050)
                .setPar("E0", 18575.0, 1.4)
                .setPar("mnu2", 0.0, 1.0)
                .setPar("msterile2", 1000.0 * 1000.0, 0.0)
                .setPar("U2", 0.0, 1e-4, -1.0, 1.0)
                .setPar("X", 0.04, 0.01, 0.0, java.lang.Double.POSITIVE_INFINITY)
                .setPar("trap", 1.0, 0.01, 0.0, java.lang.Double.POSITIVE_INFINITY)

        var u = 14000.0
        while (u < 18600) {
            //            double oldVal = oldFunc.value(u, allPars);
            //            double newVal = newFunc.value(u, allPars);
            val oldVal = oldFunc.derivValue("trap", u, allPars)
            val newVal = newFunc.derivValue("trap", u, allPars)
            System.out.printf("%f\t%g\t%g\t%g%n", u, oldVal, newVal, 1.0 - oldVal / newVal)
            u += 100.0
        }
    }

    private fun oldModel(context: Context, meta: Meta): ParametricFunction {
        val A = meta.getDouble("resolution", meta.getDouble("resolution.width", 8.3e-5)!!)!!//8.3e-5
        val from = meta.getDouble("from", 13900.0)!!
        val to = meta.getDouble("to", 18700.0)!!
        context.chronicle.report("Setting up tritium model with real transmission function")
        val resolutionTail: BivariateFunction
        if (meta.hasValue("resolution.tailAlpha")) {
            resolutionTail = ResolutionFunction.getAngledTail(meta.getDouble("resolution.tailAlpha")!!, meta.getDouble("resolution.tailBeta", 0.0)!!)
        } else {
            resolutionTail = ResolutionFunction.getRealTail()
        }
        //RangedNamedSetSpectrum beta = new BetaSpectrum(context.io().getFile("FS.txt"));
        val beta = BetaSpectrum()
        val sp = ModularSpectrum(beta, ResolutionFunction(A, resolutionTail), from, to)
        if (meta.getBoolean("caching", false)!!) {
            context.chronicle.report("Caching turned on")
            sp.setCaching(true)
        }
        //Adding trapping energy dependence

        if (meta.hasValue("transmission.trapping")) {
            val trap = MathPlugin.buildFrom(context).buildBivariateFunction(meta.getString("transmission.trapping"))
            sp.setTrappingFunction(trap)
        }

        return NBkgSpectrum(sp)
    }

    private fun newModel(context: Context, meta: Meta): ParametricFunction {
        val sp = SterileNeutrinoSpectrum(context, meta)
        return NBkgSpectrum(sp)
    }

}
