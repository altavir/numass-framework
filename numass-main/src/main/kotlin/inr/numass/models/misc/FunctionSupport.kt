package inr.numass.models.misc

import hep.dataforge.maths.domains.HyperSquareDomain
import hep.dataforge.values.Values

interface FunctionSupport {
    /**
     * Get support for function itself
     */
    fun getSupport(params: Values): Pair<Double, Double>

    /**
     * GetSupport for function derivative
     */
    fun getDerivSupport(parName: String, params: Values): Pair<Double, Double>
}

interface BiFunctionSupport {
    /**
     * Get support for function itself
     */
    fun getSupport(x: Double, y: Double, params: Values): HyperSquareDomain

    /**
     * GetSupport for function derivative
     */
    fun getDerivSupport(parName: String, x: Double, params: Values): HyperSquareDomain
}