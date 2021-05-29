package hep.dataforge.maths

import org.apache.commons.math3.analysis.UnivariateFunction
import org.apache.commons.math3.linear.*

/**
 * A common class for static constructors and converters
 * Created by darksnake on 25-Nov-16.
 */
class GM {
    /**
     * Build identity matrix with given dimension multiplied by given value
     * @param dim
     * @param val
     * @return
     */
    static RealMatrix identityMatrix(int dim, double val) {
        List diag = new ArrayList();
        for (int i = 0; i < dim; i++) {
            diag.add(val);
        }
        return new DiagonalMatrix(diag as double[]);
    }

    static RealMatrix matrix(double[][] values) {
        return new Array2DRowRealMatrix(values);
    }

    static RealMatrix matrix(List<List<? extends Number>> values) {
        double[][] dvals = values as double[][];
        return new Array2DRowRealMatrix(dvals);
    }

    static RealVector vector(double[] values) {
        return new ArrayRealVector(values);
    }

    static RealVector vector(Collection<Double> values) {
        return new ArrayRealVector(values as double[]);
    }

    static UnivariateFunction function(Closure<Double> cl) {
        return { x ->
            cl.call(x).toDouble()
        }
    }
}
