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
import inr.numass.models.ResolutionFunction
import inr.numass.utils.ExpressionUtils
import org.apache.commons.math3.analysis.BivariateFunction
import java.lang.Math.sqrt
import java.util.*

/**
 * @author [Alexander Nozik](mailto:altavir@gmail.com)
 */
class NumassResolution(context: Context, meta: Meta) : AbstractParametricBiFunction(list) {

    private val resA: Double = meta.getDouble("A", 8.3e-5)
    private val resB = meta.getDouble("B", 0.0)
    private val tailFunction: BivariateFunction = when {
        meta.hasValue("tail") -> {
            val tailFunctionStr = meta.getString("tail")
            if (tailFunctionStr.startsWith("function::")) {
                FunctionLibrary.buildFrom(context).buildBivariateFunction(tailFunctionStr.substring(10))
            } else {
                BivariateFunction { E, U ->
                    val binding = HashMap<String, Any>()
                    binding["E"] = E
                    binding["U"] = U
                    binding["D"] = E - U
                    ExpressionUtils.function(tailFunctionStr, binding)
                }
            }
        }
        meta.hasValue("tailAlpha") -> {
            //add polynomial function here
            val alpha = meta.getDouble("tailAlpha")
            val beta = meta.getDouble("tailBeta", 0.0)
            BivariateFunction { E: Double, U: Double -> 1 - (E - U) * (alpha + E / 1000.0 * beta) / 1000.0 }

        }
        else -> ResolutionFunction.getConstantTail()
    }

    override fun derivValue(parName: String, x: Double, y: Double, set: Values): Double {
        return 0.0
    }

    private fun getValueFast(E: Double, U: Double): Double {
        val delta = resA * E
        return when {
            E - U < 0 -> 0.0
            E - U > delta -> tailFunction.value(E, U)
            else -> (E - U) / delta
        }
    }

    override fun providesDeriv(name: String): Boolean {
        return true
    }

    override fun value(E: Double, U: Double, set: Values): Double {
        assert(resA > 0)
        if (resB <= 0) {
            return this.getValueFast(E, U)
        }
        assert(resB > 0)
        val delta = resA * E
        return when {
            E - U < 0 -> 0.0
            E - U > delta -> tailFunction.value(E, U)
            else -> (1 - sqrt(1 - (E - U) / E * resB)) / (1 - sqrt(1 - resA * resB))
        }
    }

    companion object {

        private val list = arrayOf<String>() //leaving
    }

}
