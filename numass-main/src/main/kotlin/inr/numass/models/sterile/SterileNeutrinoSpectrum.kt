/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.models.sterile

import hep.dataforge.context.Context
import hep.dataforge.description.NodeDef
import hep.dataforge.description.NodeDefs
import hep.dataforge.description.ValueDef
import hep.dataforge.description.ValueDefs
import hep.dataforge.exceptions.NotDefinedException
import hep.dataforge.maths.integration.UnivariateIntegrator
import hep.dataforge.meta.Meta
import hep.dataforge.stat.parametric.AbstractParametricBiFunction
import hep.dataforge.stat.parametric.AbstractParametricFunction
import hep.dataforge.stat.parametric.ParametricBiFunction
import hep.dataforge.values.ValueType.BOOLEAN
import hep.dataforge.values.Values
import inr.numass.getFSS
import inr.numass.models.FSS
import inr.numass.models.misc.LossCalculator
import inr.numass.utils.NumassIntegrator

/**
 * Compact all-in-one model for sterile neutrino spectrum
 *
 * @author Alexander Nozik
 */
@NodeDefs(
        NodeDef(key = "resolution"),
        NodeDef(key = "transmission")
)
@ValueDefs(
        ValueDef(key = "fssFile", info = "The name for external FSS file. By default internal FSS file is used"),
        ValueDef(key = "useFSS", type = arrayOf(BOOLEAN))
)

/**
 * @param source variables:Eo offset,Ein; parameters: "mnu2", "msterile2", "U2"
 * @param transmission variables:Ein,Eout; parameters: "A"
 * @param resolution variables:Eout,U; parameters: "X", "trap"
 */

class SterileNeutrinoSpectrum @JvmOverloads constructor(
        context: Context,
        configuration: Meta,
        val source: ParametricBiFunction = NumassBeta(),
        val transmission: ParametricBiFunction = NumassTransmission(context, configuration.getMetaOrEmpty("transmission")),
        val resolution: ParametricBiFunction = NumassResolution(context, configuration.getMeta("resolution", Meta.empty()))
) : AbstractParametricFunction(*list) {


    /**
     * auxiliary function for trans-res convolution
     */
    private val transRes: ParametricBiFunction = TransRes()
    private val fss: FSS? = getFSS(context, configuration)
    //    private boolean useMC;
    private val fast: Boolean = configuration.getBoolean("fast", true)

    override fun derivValue(parName: String, u: Double, set: Values): Double {
        return when (parName) {
            "U2", "msterile2", "mnu2", "E0" -> integrate(u, source.derivative(parName), transRes, set)
            "X", "trap" -> integrate(u, source, transRes.derivative(parName), set)
            else -> throw NotDefinedException()
        }
    }

    override fun value(u: Double, set: Values): Double {
        return integrate(u, source, transRes, set)
    }

    override fun providesDeriv(name: String): Boolean {
        return source.providesDeriv(name) && transmission.providesDeriv(name) && resolution.providesDeriv(name)
    }


    /**
     * Direct Gauss-Legendre integration
     *
     * @param u
     * @param sourceFunction
     * @param transResFunction
     * @param set
     * @return
     */
    private fun integrate(
            u: Double,
            sourceFunction: ParametricBiFunction,
            transResFunction: ParametricBiFunction,
            set: Values): Double {

        val eMax = set.getDouble("E0") + 5.0

        if (u >= eMax) {
            return 0.0
        }

        val integrator: UnivariateIntegrator<*> = if (fast) {
            when {
                eMax - u < 300 -> NumassIntegrator.getFastInterator()
                eMax - u > 2000 -> NumassIntegrator.getHighDensityIntegrator()
                else -> NumassIntegrator.getDefaultIntegrator()
            }

        } else {
            NumassIntegrator.getHighDensityIntegrator()
        }

        return integrator.integrate(u, eMax) { eIn -> sumByFSS(eIn, sourceFunction, set) * transResFunction.value(eIn, u, set) }
    }

    private fun sumByFSS(eIn: Double, sourceFunction: ParametricBiFunction, set: Values): Double {
        return if (fss == null) {
            sourceFunction.value(0.0, eIn, set)
        } else {
            (0 until fss.size()).sumByDouble { fss.getP(it) * sourceFunction.value(fss.getE(it), eIn, set) }
        }
    }



    private inner class TransRes : AbstractParametricBiFunction(arrayOf("X", "trap")) {

        override fun providesDeriv(name: String): Boolean {
            return true
        }

        override fun derivValue(parName: String, eIn: Double, u: Double, set: Values): Double {
            return when (parName) {
                "X" -> throw NotDefinedException()//TODO implement p0 derivative
                "trap" -> lossRes(transmission.derivative(parName), eIn, u, set)
                else -> super.derivValue(parName, eIn, u, set)
            }
        }

        override fun value(eIn: Double, u: Double, set: Values): Double {

            val p0 = LossCalculator.p0(set, eIn)
            return p0 * resolution.value(eIn, u, set) + lossRes(transmission, eIn, u, set)
        }

        private fun lossRes(transFunc: ParametricBiFunction, eIn: Double, u: Double, set: Values): Double {
            val integrand = { eOut: Double -> transFunc.value(eIn, eOut, set) * resolution.value(eOut, u, set) }

            val border = u + 30
            val firstPart = NumassIntegrator.getFastInterator().integrate(u, Math.min(eIn, border), integrand)
            val secondPart: Double = if (eIn > border) {
                if (fast) {
                    NumassIntegrator.getDefaultIntegrator().integrate(border, eIn, integrand)
                } else {
                    NumassIntegrator.getHighDensityIntegrator().integrate(border, eIn, integrand)
                }
            } else {
                0.0
            }
            return firstPart + secondPart
        }

    }

    companion object {

        private val list = arrayOf("X", "trap", "E0", "mnu2", "msterile2", "U2")
    }

}
