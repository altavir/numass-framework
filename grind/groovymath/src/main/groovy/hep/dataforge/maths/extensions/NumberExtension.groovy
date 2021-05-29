package hep.dataforge.maths.extensions

import org.apache.commons.math3.analysis.UnivariateFunction
import org.apache.commons.math3.analysis.differentiation.UnivariateDifferentiableFunction
import org.apache.commons.math3.linear.RealMatrix
import org.apache.commons.math3.linear.RealVector

/**
 * Created by darksnake on 06-Nov-16.
 */
class NumberExtension {
    static RealVector plus(final Number self, RealVector other) {
        return other + self
    }

    static RealVector minus(final Number self, RealVector other) {
        return (-other) + self
    }

    static RealVector multiply(final Number self, RealVector other) {
        return other * self;
    }

    static RealMatrix plus(final Number self, RealMatrix other) {
        return other + self
    }

    static RealMatrix minus(final Number self, RealMatrix other) {
        return (-other) + self
    }

    static RealMatrix multiply(final Number self, RealMatrix other) {
        return other * self;
    }

    static UnivariateFunction plus(final Number self, UnivariateFunction other) {
        return other + self
    }

    static UnivariateFunction minus(final Number self, UnivariateFunction other) {
        return (-other) + self
    }

    static UnivariateFunction multiply(final Number self, UnivariateFunction other) {
        return other * self;
    }

    static UnivariateDifferentiableFunction plus(final Number self, UnivariateDifferentiableFunction other) {
        return other + self
    }

    static UnivariateDifferentiableFunction minus(final Number self, UnivariateDifferentiableFunction other) {
        return (-other) + self
    }

    static UnivariateDifferentiableFunction multiply(final Number self, UnivariateDifferentiableFunction other) {
        return other * self;
    }

    /**
     * Fix for bugged power method in DefaultGroovyMethods
     * @param self
     * @param exponent
     * @return
     */
    static Number power(Number self, Number exponent) {
        return Math.pow(self.doubleValue(),exponent.doubleValue());
//        double base, exp, answer;
//        base = self.doubleValue();
//        exp = exponent.doubleValue();
//
//        answer = Math.pow(base, exp);
//        if ((double) ((int) answer) == answer) {
//            return (int) answer;
//        } else if ((double) ((long) answer) == answer) {
//            return (long) answer;
//        } else {
//            return answer;
//        }
    }
}
