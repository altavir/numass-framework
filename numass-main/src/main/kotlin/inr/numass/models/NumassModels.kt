package inr.numass.models

import hep.dataforge.maths.integration.UnivariateIntegrator
import hep.dataforge.names.NameList
import hep.dataforge.stat.models.Model
import hep.dataforge.stat.models.ModelFactory
import hep.dataforge.stat.parametric.AbstractParametricFunction
import hep.dataforge.stat.parametric.ParametricFunction
import hep.dataforge.utils.ContextMetaFactory
import hep.dataforge.values.Values
import inr.numass.models.misc.FunctionSupport
import inr.numass.utils.NumassIntegrator
import java.util.stream.Stream


fun model(name: String,factory: ContextMetaFactory<Model>): ModelFactory {
    return ModelFactory.build(name, factory);
}

// spectra operations
//TODO move to stat

/**
 * Calculate sum of two parametric functions
 */
operator fun ParametricFunction.plus(func: ParametricFunction): ParametricFunction {
    val mergedNames = NameList(Stream.concat(names.stream(), func.names.stream()).distinct())
    return object : AbstractParametricFunction(mergedNames) {
        override fun derivValue(parName: String, x: Double, set: Values): Double {
            return func.derivValue(parName, x, set) + this@plus.derivValue(parName, x, set)
        }

        override fun value(x: Double, set: Values): Double {
            return this@plus.value(x, set) + func.value(x, set)
        }

        override fun providesDeriv(name: String): Boolean {
            return this@plus.providesDeriv(name) && func.providesDeriv(name)
        }

    }
}

operator fun ParametricFunction.minus(func: ParametricFunction): ParametricFunction {
    val mergedNames = NameList(Stream.concat(names.stream(), func.names.stream()).distinct())
    return object : AbstractParametricFunction(mergedNames) {
        override fun derivValue(parName: String, x: Double, set: Values): Double {
            return func.derivValue(parName, x, set) - this@minus.derivValue(parName, x, set)
        }

        override fun value(x: Double, set: Values): Double {
            return this@minus.value(x, set) - func.value(x, set)
        }

        override fun providesDeriv(name: String): Boolean {
            return this@minus.providesDeriv(name) && func.providesDeriv(name)
        }

    }
}

/**
 * Calculate product of two parametric functions
 *
 */
operator fun ParametricFunction.times(func: ParametricFunction): ParametricFunction {
    val mergedNames = NameList(Stream.concat(names.stream(), func.names.stream()).distinct())
    return object : AbstractParametricFunction(mergedNames) {
        override fun derivValue(parName: String, x: Double, set: Values): Double {
            return this@times.value(x, set) * func.derivValue(parName, x, set) + this@times.derivValue(parName, x, set) * func.value(x, set)
        }

        override fun value(x: Double, set: Values): Double {
            return this@times.value(x, set) * func.value(x, set)
        }

        override fun providesDeriv(name: String): Boolean {
            return this@times.providesDeriv(name) && func.providesDeriv(name)
        }

    }
}

/**
 * Multiply parametric function by fixed value
 */
operator fun ParametricFunction.times(num: Number): ParametricFunction {
    return object : AbstractParametricFunction(names) {
        override fun derivValue(parName: String, x: Double, set: Values): Double {
            return this@times.value(x, set) * num.toDouble()
        }

        override fun value(x: Double, set: Values): Double {
            return this@times.value(x, set) * num.toDouble()
        }

        override fun providesDeriv(name: String): Boolean {
            return this@times.providesDeriv(name)
        }

    }
}

/**
 * Calculate convolution of two parametric functions
 * @param func the function with which this function should be convoluded
 * @param integrator optional integrator to be used in convolution
 * @param support a function defining borders for integration. It takes 3 parameter: set of parameters,
 * name of the derivative (empty for value) and point in which convolution should be calculated.
 */
fun ParametricFunction.convolute(
        func: ParametricFunction,
        integrator: UnivariateIntegrator<*> = NumassIntegrator.getDefaultIntegrator(),
        support: Values.(String, Double) -> Pair<Double, Double>
): ParametricFunction {
    val mergedNames = NameList(Stream.concat(names.stream(), func.names.stream()).distinct())
    return object : AbstractParametricFunction(mergedNames) {
        override fun derivValue(parName: String, x: Double, set: Values): Double {
            val (a, b) = set.support(parName, x)
            return integrator.integrate(a, b) { y: Double ->
                this@convolute.derivValue(parName, y, set) * func.value(x - y, set) +
                        this@convolute.value(y, set) * func.derivValue(parName, x - y, set)
            }
        }

        override fun value(x: Double, set: Values): Double {
            val (a, b) = set.support("", x)
            return integrator.integrate(a, b) { y: Double -> this@convolute.value(y, set) * func.value(x - y, set) }
        }

        override fun providesDeriv(name: String?): Boolean {
            return this@convolute.providesDeriv(name) && func.providesDeriv(name)
        }

    }

}

@JvmOverloads
fun <T> ParametricFunction.convolute(
        func: T,
        integrator: UnivariateIntegrator<*> = NumassIntegrator.getDefaultIntegrator()
): ParametricFunction where T : ParametricFunction, T : FunctionSupport {
    //inverted order for correct boundaries
    return func.convolute(this, integrator) { parName, x ->
        if (parName.isEmpty()) {
            func.getSupport(this)
        } else {
            func.getDerivSupport(parName, this)
        }
    }
}