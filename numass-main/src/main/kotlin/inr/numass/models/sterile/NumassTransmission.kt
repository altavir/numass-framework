/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.models.sterile

import hep.dataforge.context.Context
import hep.dataforge.maths.functions.FunctionLibrary
import hep.dataforge.meta.Meta
import hep.dataforge.stat.parametric.AbstractParametricBiFunction
import hep.dataforge.values.Values
import inr.numass.models.misc.LossCalculator
import inr.numass.utils.ExpressionUtils
import org.apache.commons.math3.analysis.BivariateFunction
import org.slf4j.LoggerFactory
import java.util.*

/**
 * @author [Alexander Nozik](mailto:altavir@gmail.com)
 */
class NumassTransmission(context: Context, meta: Meta) : AbstractParametricBiFunction(list) {
    private val trapFunc: BivariateFunction
    //private val lossCache = HashMap<Double, UnivariateFunction>()

    private val adjustX: Boolean = meta.getBoolean("adjustX", false)

    init {
        if (meta.hasValue("trapping")) {
            val trapFuncStr = meta.getString("trapping")
            trapFunc = if (trapFuncStr.startsWith("function::")) {
                FunctionLibrary.buildFrom(context).buildBivariateFunction(trapFuncStr.substring(10))
            } else {
                BivariateFunction { Ei: Double, Ef: Double ->
                    val binding = HashMap<String, Any>()
                    binding["Ei"] = Ei
                    binding["Ef"] = Ef
                    ExpressionUtils.function(trapFuncStr, binding)
                }
            }
        } else {
            LoggerFactory.getLogger(javaClass).warn("Trapping function not defined. Using default")
            trapFunc = FunctionLibrary.buildFrom(context).buildBivariateFunction("numass.trap.nominal")
        }
    }

    override fun derivValue(parName: String, eIn: Double, eOut: Double, set: Values): Double {
        return when (parName) {
            "trap" -> trapFunc.value(eIn, eOut)
            "X" -> LossCalculator.getTotalLossDeriv(set, eIn, eOut)
            else -> super.derivValue(parName, eIn, eOut, set)
        }
    }

    override fun providesDeriv(name: String): Boolean {
        return true
    }

    override fun value(eIn: Double, eOut: Double, set: Values): Double {
        // loss part
        val loss = LossCalculator.getTotalLossValue(set, eIn, eOut)
        //        double loss;
        //
        //        if(eIn-eOut >= 300){
        //            loss = 0;
        //        } else {
        //            UnivariateFunction lossFunction = this.lossCache.computeIfAbsent(X, theX ->
        //                    FunctionCaching.cacheUnivariateFunction(0, 300, 400, x -> calculator.getTotalLossValue(theX, eIn, eIn - x))
        //            );
        //
        //            loss = lossFunction.value(eIn - eOut);
        //        }

        //trapping part
        val trap = getParameter("trap", set) * trapFunc.value(eIn, eOut)
        return loss + trap
    }

    companion object {

        private val list = arrayOf("trap", "X")
    }

}
