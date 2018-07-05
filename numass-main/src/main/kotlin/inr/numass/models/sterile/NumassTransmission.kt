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
import inr.numass.models.LossCalculator
import inr.numass.utils.ExpressionUtils
import org.apache.commons.math3.analysis.BivariateFunction
import org.slf4j.LoggerFactory
import java.util.*

/**
 * @author [Alexander Nozik](mailto:altavir@gmail.com)
 */
class NumassTransmission(context: Context, meta: Meta) : AbstractParametricBiFunction(list) {
    private val calculator: LossCalculator = LossCalculator.instance()
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

    private fun getX(eIn: Double, set: Values): Double {
        return if (adjustX) {
            //From our article
            set.getDouble("X") * Math.log(eIn / ION_POTENTIAL) * eIn * ION_POTENTIAL / 1.9580741410115568e6
        } else {
            set.getDouble("X")
        }
    }

    fun p0(eIn: Double, set: Values): Double {
        return LossCalculator.instance().getLossProbability(0, getX(eIn, set))
    }

    override fun derivValue(parName: String, eIn: Double, eOut: Double, set: Values): Double {
        return when (parName) {
            "trap" -> trapFunc.value(eIn, eOut)
            "X" -> calculator.getTotalLossDeriv(getX(eIn, set), eIn, eOut)
            else -> super.derivValue(parName, eIn, eOut, set)
        }
    }

    override fun providesDeriv(name: String): Boolean {
        return true
    }

    override fun value(eIn: Double, eOut: Double, set: Values): Double {
        //calculate X taking into account its energy dependence
        val X = getX(eIn, set)
        // loss part
        val loss = calculator.getTotalLossValue(X, eIn, eOut)
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
        private const val ION_POTENTIAL = 15.4//eV
    }

}
