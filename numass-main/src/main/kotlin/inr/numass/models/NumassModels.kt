package inr.numass.models

import hep.dataforge.maths.integration.UnivariateIntegrator
import hep.dataforge.names.Names
import hep.dataforge.stat.models.Model
import hep.dataforge.stat.models.ModelDescriptor
import hep.dataforge.stat.models.ModelFactory
import hep.dataforge.stat.parametric.AbstractParametricFunction
import hep.dataforge.stat.parametric.ParametricFunction
import hep.dataforge.utils.ContextMetaFactory
import hep.dataforge.values.Values
import inr.numass.utils.NumassIntegrator
import java.util.stream.Stream


fun model(name: String, descriptor: ModelDescriptor? = null, factory: ContextMetaFactory<Model>): ModelFactory {
    return ModelFactory.build(name, descriptor, factory);
}

// spectra operations
//TODO move to stat

/**
 * Calculate sum of two parametric functions
 */
operator fun ParametricFunction.plus(func: ParametricFunction): ParametricFunction {
    val mergedNames = Names.of(Stream.concat(names.stream(), func.names.stream()).distinct())
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
    val mergedNames = Names.of(Stream.concat(names.stream(), func.names.stream()).distinct())
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
    val mergedNames = Names.of(Stream.concat(names.stream(), func.names.stream()).distinct())
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
 */
fun ParametricFunction.convolute(func: ParametricFunction,
                                 integrator: UnivariateIntegrator<*> = NumassIntegrator.getDefaultIntegrator()): ParametricFunction {
    val mergedNames = Names.of(Stream.concat(names.stream(), func.names.stream()).distinct())
    return object : AbstractParametricFunction(mergedNames){
        override fun derivValue(parName: String, x: Double, set: Values): Double {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun value(x: Double, set: Values): Double {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun providesDeriv(name: String?): Boolean {
            return this@convolute.providesDeriv(name) && func.providesDeriv(name)
        }

    }

}