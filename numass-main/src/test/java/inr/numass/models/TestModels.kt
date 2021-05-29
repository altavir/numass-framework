/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.models

import hep.dataforge.context.Context
import hep.dataforge.maths.functions.FunctionLibrary
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder
import hep.dataforge.stat.fit.ParamSet
import hep.dataforge.stat.parametric.ParametricFunction
import hep.dataforge.step
import inr.numass.Numass
import inr.numass.models.sterile.SterileNeutrinoSpectrum
import org.apache.commons.math3.analysis.BivariateFunction


/**
 * @param args the command line arguments
 */

fun main() {
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
//                    .putValue("trapping", "function::numass.trap.nominal")
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
            .setPar("X", 0.0, 0.01, 0.0, java.lang.Double.POSITIVE_INFINITY)
            .setPar("trap", 1.0, 0.01, 0.0, java.lang.Double.POSITIVE_INFINITY)

    for (u in 14000.0..18600.0 step 100.0) {
        val oldVal = oldFunc.value(u, allPars);
        val newVal = newFunc.value(u, allPars);
//        val oldVal = oldFunc.derivValue("trap", u, allPars)
//        val newVal = newFunc.derivValue("trap", u, allPars)
        System.out.printf("%f\t%g\t%g\t%g%n", u, oldVal, newVal, 1.0 - oldVal / newVal)
    }
}

private fun oldModel(context: Context, meta: Meta): ParametricFunction {
    val A = meta.getDouble("resolution", meta.getDouble("resolution.width", 8.3e-5))//8.3e-5
    val from = meta.getDouble("from", 13900.0)
    val to = meta.getDouble("to", 18700.0)
    context.history.report("Setting up tritium model with real transmission function")

    val resolutionTail: BivariateFunction = if (meta.hasValue("resolution.tailAlpha")) {
        ResolutionFunction.getAngledTail(meta.getDouble("resolution.tailAlpha"), meta.getDouble("resolution.tailBeta", 0.0))
    } else {
        ResolutionFunction.getRealTail()
    }
    //RangedNamedSetSpectrum beta = new BetaSpectrum(context.io().getFile("FS.txt"));
    val sp = ModularSpectrum(BetaSpectrum(), ResolutionFunction(A, resolutionTail), from, to)
    if (meta.getBoolean("caching", false)) {
        context.history.report("Caching turned on")
        sp.setCaching(true)
    }
    //Adding trapping energy dependence

    if (meta.hasValue("transmission.trapping")) {
        val trap = FunctionLibrary.buildFrom(context).buildBivariateFunction(meta.getString("transmission.trapping"))
        sp.setTrappingFunction(trap)
    }

    return NBkgSpectrum(sp)
}

private fun newModel(context: Context, meta: Meta): ParametricFunction {
    val sp = SterileNeutrinoSpectrum(context, meta)
    return NBkgSpectrum(sp)
}

