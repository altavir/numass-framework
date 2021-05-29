package hep.dataforge.maths.extensions

import org.apache.commons.math3.analysis.differentiation.DerivativeStructure
import org.apache.commons.math3.analysis.differentiation.UnivariateDifferentiableFunction

/**
 *  A static extension for commons-math UnivariateDifferentiableFunction.
 *  To use complicated functions one should use {@code import static DerivativeStructure* }
 * Created by darksnake on 06-Nov-15.
 */
class UnivariateDifferentiableFunctionExtension {
    static UnivariateDifferentiableFunction plus(
            final UnivariateDifferentiableFunction self, UnivariateDifferentiableFunction function) {
        return { DerivativeStructure d -> self.value(d).add(function.value(d)) }
    }

    static UnivariateDifferentiableFunction plus(final UnivariateDifferentiableFunction self, Number num) {
        return { DerivativeStructure d -> self.value(d).add(num.doubleValue()) }
    }

    static UnivariateDifferentiableFunction minus(
            final UnivariateDifferentiableFunction self, UnivariateDifferentiableFunction function) {
        return { DerivativeStructure d -> self.value(d).subtract(function.value(d)) }
    }

    static UnivariateDifferentiableFunction minus(final UnivariateDifferentiableFunction self, Number num) {
        return { DerivativeStructure d -> self.value(d).subtract(num.d) }
    }

    static UnivariateDifferentiableFunction multiply(
            final UnivariateDifferentiableFunction self, UnivariateDifferentiableFunction function) {
        return { DerivativeStructure d -> self.value(d).multiply(function.value(d)) }
    }

    static UnivariateDifferentiableFunction multiply(final UnivariateDifferentiableFunction self, Number num) {
        return { DerivativeStructure d -> self.value(d).multiply(num.doubleValue()) }
    }

    static UnivariateDifferentiableFunction div(
            final UnivariateDifferentiableFunction self, UnivariateDifferentiableFunction function) {
        return { DerivativeStructure d -> self.value(d).divide(function.value(d)) }
    }

    static UnivariateDifferentiableFunction div(final UnivariateDifferentiableFunction self, Number num) {
        return { DerivativeStructure d -> self.value(d).divide(num.doubleValue()) }
    }

    static UnivariateDifferentiableFunction power(
            final UnivariateDifferentiableFunction self, UnivariateDifferentiableFunction function) {
        return { DerivativeStructure d -> self.value(d).pow(function.value(d)) }
    }

    static UnivariateDifferentiableFunction power(final UnivariateDifferentiableFunction self, Number num) {
        return { DerivativeStructure d -> self.value(d).pow(num.doubleValue()) }
    }

    static UnivariateDifferentiableFunction negative(final UnivariateDifferentiableFunction self) {
        return { DerivativeStructure d -> self.value(d).negate() }
    }

    static DerivativeStructure call(final UnivariateDifferentiableFunction self, DerivativeStructure d) {
        return self.value(d)
    }

}
