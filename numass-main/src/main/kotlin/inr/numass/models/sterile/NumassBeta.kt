/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.models.sterile

import hep.dataforge.exceptions.NotDefinedException
import hep.dataforge.stat.parametric.AbstractParametricBiFunction
import hep.dataforge.stat.parametric.AbstractParametricFunction
import hep.dataforge.stat.parametric.ParametricFunction
import hep.dataforge.values.Values

import java.lang.Math.*

/**
 * A bi-function for beta-spectrum calculation taking energy and final state as
 * input.
 *
 *
 * dissertation p.33
 *
 *
 * @author [Alexander Nozik](mailto:altavir@gmail.com)
 */
class NumassBeta : AbstractParametricBiFunction(list) {

    /**
     * Beta spectrum derivative
     *
     * @param n parameter number
     * @param E0
     * @param mnu2
     * @param E
     * @return
     * @throws NotDefinedException
     */
    @Throws(NotDefinedException::class)
    private fun derivRoot(n: Int, E0: Double, mnu2: Double, E: Double): Double {
        val D = E0 - E//E0-E
        if (D == 0.0) {
            return 0.0
        }

        return if (mnu2 >= 0) {
            if (E >= E0 - sqrt(mnu2)) {
                0.0
            } else {
                val bare = sqrt(D * D - mnu2)
                when (n) {
                    0 -> factor(E) * (2.0 * D * D - mnu2) / bare
                    1 -> -factor(E) * 0.5 * D / bare
                    else -> 0.0
                }
            }
        } else {
            val mu = sqrt(-0.66 * mnu2)
            if (E >= E0 + mu) {
                0.0
            } else {
                val root = sqrt(Math.max(D * D - mnu2, 0.0))
                val exp = exp(-1 - D / mu)
                when (n) {
                    0 -> factor(E) * (D * (D + mu * exp) / root + root * (1 - exp))
                    1 -> factor(E) * (-(D + mu * exp) / root * 0.5 - root * exp * (1 + D / mu) / 3.0 / mu)
                    else -> 0.0
                }
            }
        }
    }

    /**
     * Derivative of spectrum with sterile neutrinos
     *
     * @param name
     * @param E
     * @param E0
     * @param pars
     * @return
     * @throws NotDefinedException
     */
    @Throws(NotDefinedException::class)
    private fun derivRootsterile(name: String, E: Double, E0: Double, pars: Values): Double {
        val mnu2 = getParameter("mnu2", pars)
        val mst2 = getParameter("msterile2", pars)
        val u2 = getParameter("U2", pars)

        return when (name) {
            "E0" -> {
                if (u2 == 0.0) {
                    derivRoot(0, E0, mnu2, E)
                } else {
                    u2 * derivRoot(0, E0, mst2, E) + (1 - u2) * derivRoot(0, E0, mnu2, E)
                }
            }
            "mnu2" -> (1 - u2) * derivRoot(1, E0, mnu2, E)
            "msterile2" -> {
                if (u2 == 0.0) {
                    0.0
                } else {
                    u2 * derivRoot(1, E0, mst2, E)
                }
            }
            "U2" -> root(E0, mst2, E) - root(E0, mnu2, E)
            else -> 0.0
        }

    }

    /**
     * The part independent of neutrino mass. Includes global normalization
     * constant, momentum and Fermi correction
     *
     * @param E
     * @return
     */
    private fun factor(E: Double): Double {
        val me = 0.511006E6
        val eTot = E + me
        val pe = sqrt(E * (E + 2.0 * me))
        val ve = pe / eTot
        val yfactor = 2.0 * 2.0 * 1.0 / 137.039 * Math.PI
        val y = yfactor / ve
        val fn = y / abs(1.0 - exp(-y))
        val fermi = fn * (1.002037 - 0.001427 * ve)
        val res = fermi * pe * eTot
        return K * res
    }

    override fun providesDeriv(name: String): Boolean {
        return true
    }

    /**
     * Bare beta spectrum with Mainz negative mass correction
     *
     * @param E0
     * @param mnu2
     * @param E
     * @return
     */
    private fun root(E0: Double, mnu2: Double, E: Double): Double {
        //bare beta-spectrum
        val delta = E0 - E
        val bare = factor(E) * delta * sqrt(Math.max(delta * delta - mnu2, 0.0))
        return when {
            mnu2 >= 0 -> Math.max(bare, 0.0)
            delta == 0.0 -> 0.0
            delta + 0.812 * sqrt(-mnu2) <= 0 -> 0.0              //sqrt(0.66)
            else -> {
                val aux = sqrt(-mnu2 * 0.66) / delta
                Math.max(bare * (1 + aux * exp(-1 - 1 / aux)), 0.0)
            }
        }
    }

    /**
     * beta-spectrum with sterile neutrinos
     *
     * @param E
     * @param E0
     * @param pars
     * @return
     */
    private fun rootsterile(E: Double, E0: Double, pars: Values): Double {
        val mnu2 = getParameter("mnu2", pars)
        val mst2 = getParameter("msterile2", pars)
        val u2 = getParameter("U2", pars)

        return if (u2 == 0.0) {
            root(E0, mnu2, E)
        } else {
            u2 * root(E0, mst2, E) + (1 - u2) * root(E0, mnu2, E)
        }
// P(rootsterile)+ (1-P)root
    }

    override fun getDefaultParameter(name: String): Double {
        return when (name) {
            "mnu2", "U2", "msterile2" -> 0.0
            else -> super.getDefaultParameter(name)
        }
    }

    override fun derivValue(parName: String, fs: Double, eIn: Double, pars: Values): Double {
        val e0 = getParameter("E0", pars)
        return derivRootsterile(parName, eIn, e0 - fs, pars)
    }

    override fun value(fs: Double, eIn: Double, pars: Values): Double {
        val e0 = getParameter("E0", pars)
        return rootsterile(eIn, e0 - fs, pars)
    }

    /**
     * Get univariate spectrum with given final state
     */
    fun getSpectrum(fs: Double = 0.0): ParametricFunction {
        return BetaSpectrum(fs);
    }

    inner class BetaSpectrum(val fs: Double) : AbstractParametricFunction(*list) {

        override fun providesDeriv(name: String): Boolean {
            return this@NumassBeta.providesDeriv(name)
        }

        override fun derivValue(parName: String, x: Double, set: Values): Double {
            return this@NumassBeta.derivValue(parName, fs, x, set)
        }

        override fun value(x: Double, set: Values): Double {
            return this@NumassBeta.value(fs, x, set)
        }

    }


    companion object {

        private const val K = 1E-23
        private val list = arrayOf("E0", "mnu2", "msterile2", "U2")
    }

}
