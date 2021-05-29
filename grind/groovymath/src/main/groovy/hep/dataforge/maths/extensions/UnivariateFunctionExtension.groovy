package hep.dataforge.maths.extensions

import org.apache.commons.math3.analysis.UnivariateFunction
import org.apache.commons.math3.linear.RealVector

/**
 * A static extension for commons-math UnivariateFunctions
 * Created by darksnake on 06-Nov-15.
 */
class UnivariateFunctionExtension {

    static Double value(final UnivariateFunction self, Number x) {
        return self.value(x as Double)
    }

    static Double value(final UnivariateFunction self, int x) {
        //FIXME do some magic to force groovy to work with integers as doubles
        return self.value(x as Double)
    }

    static Double call(final UnivariateFunction self, Number x) {
        return value(self, x)
    }

    static UnivariateFunction plus(final UnivariateFunction self, UnivariateFunction function) {
        return { double x ->
            return self.value(x) + function.value(x)
        }
    }

    static UnivariateFunction plus(final UnivariateFunction self, Number num) {
        return { double x ->
            return self.value(x) + num
        }
    }

    static UnivariateFunction minus(final UnivariateFunction self, UnivariateFunction function) {
        return { double x ->
            return self.value(x) - function.value(x)
        }
    }

    static UnivariateFunction minus(final UnivariateFunction self, Number num) {
        return { double x ->
            return self.value(x as double) - num
        }
    }

    static UnivariateFunction multiply(final UnivariateFunction self, UnivariateFunction function) {
        return { double x ->
            return self.value(x) * function.value(x)
        }
    }

    static UnivariateFunction multiply(final UnivariateFunction self, Number num) {
        return { double x ->
            return self.value(x) * num
        }
    }

    static UnivariateFunction div(final UnivariateFunction self, UnivariateFunction function) {
        return { double x ->
            return self.value(x) / function.value(x)
        }
    }

    static UnivariateFunction div(final UnivariateFunction self, Number num) {
        return { double x ->
            return self.value(x) / num
        }
    }

    static UnivariateFunction power(final UnivariateFunction self, UnivariateFunction function) {
        return { double x ->
            return (self.value(x)**(function.value(x))).doubleValue()
        }
    }

    static UnivariateFunction power(final UnivariateFunction self, Number num) {
        return { double x ->
            return (self.value(x)**(num)).getDouble()
        }
    }

    static UnivariateFunction negative(final UnivariateFunction self) {
        return { double x ->
            return -self.value(x)
        }
    }

    static RealVector value(final UnivariateFunction self, RealVector vector) {
        return vector.map(self);
    }


}

