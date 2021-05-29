package hep.dataforge.maths.extensions

import hep.dataforge.maths.GM
import org.apache.commons.math3.linear.RealMatrix
import org.apache.commons.math3.linear.RealVector

/**
 * Extension module for Commons Math linear
 * Created by darksnake on 01-Jul-16.
 */
class RealVectorExtension {

    static {
        //TODO add class cast from double[] to vector
    }

    static RealVector plus(final RealVector self, RealVector other) {
        return self.add(other)
    }

    static RealVector plus(final RealVector self, Number num) {
        return self.mapAdd(num)
    }

    static RealVector minus(final RealVector self, RealVector other) {
        return self.subtract(other)
    }

    static RealVector minus(final RealVector self, Number num) {
        return self.mapSubtract(num)
    }

    static RealVector negative(final RealVector self) {
        return self.mapMultiply(-1d)
    }

    /**
     * scalar product
     * @param self
     * @param other
     * @return
     */
    static Number multiply(final RealVector self, RealVector other) {
        return self.dotProduct(other)
    }

    static RealVector multiply(final RealVector self, Number num) {
        return self.mapMultiply(num)
    }

    static RealVector multiply(final RealVector self, RealMatrix matrix) {
        return matrix.preMultiply(self)
    }

    static RealVector div(final RealVector self, Number num) {
        return self.mapDivide(num)
    }

    static RealVector power(final RealVector self, Number num) {
        return self.map { it**num }
    }

    static RealVector leftShift(final RealVector self, Object obj) {
        return self.append(obj)
    }

    static Number getAt(final RealVector self, int index) {
        return self.getEntry(index);
    }

    static Void putAt(final RealVector self, int index, Number value) {
        return self.setEntry(index, value);
    }

    static RealVector transform(final RealVector self, Closure<Double> transformation) {
        return GM.vector(self.toArray().collect(transformation))
    }

    static Object asType(final RealVector self, Class type) {
        if (type.isAssignableFrom(List.class) || type == double[]) {
            return self.toArray()
        }
    }

    static Object asVector(final List<Double> self) {
        return GM.vector(self)
    }

}
