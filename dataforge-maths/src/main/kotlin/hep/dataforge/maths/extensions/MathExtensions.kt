package hep.dataforge.maths.extensions

import org.apache.commons.math3.analysis.UnivariateFunction
import org.apache.commons.math3.linear.*

/**
 * A number of useful extensions for existing common-maths classes
 *
 * Created by darksnake on 24-Apr-17.
 */

//vector extensions

operator fun RealVector.plus(other: RealVector): RealVector {
    return this.add(other)
}

operator fun RealVector.plus(num: Number): RealVector {
    return this.mapAdd(num.toDouble())
}

operator fun RealVector.minus(other: RealVector): RealVector {
    return this.subtract(other)
}

operator fun RealVector.minus(num: Number): RealVector {
    return this.mapSubtract(num.toDouble())
}

operator fun RealVector.unaryMinus(): RealVector {
    return this.mapMultiply(-1.0)
}

/**
 * scalar product
 * @param this
 * @param other
 * @return
 */
operator fun RealVector.times(other: RealVector): Number {
    return this.dotProduct(other)
}

operator fun RealVector.times(num: Number): RealVector {
    return this.mapMultiply(num.toDouble())
}

operator fun RealVector.times(matrix: RealMatrix): RealVector {
    return matrix.preMultiply(this)
}

operator fun RealVector.div(num: Number): RealVector {
    return this.mapDivide(num.toDouble())
}


operator fun RealVector.get(index: Int): Double {
    return this.getEntry(index);
}

operator fun RealVector.set(index: Int, value: Number) {
    return this.setEntry(index, value.toDouble());
}

fun DoubleArray.toVector(): RealVector {
    return ArrayRealVector(this);
}

//matrix extensions

/**
 * Return new map and apply given transformation to each of its elements. Closure takes 3 arguments: row number,
 * column number and actual value of matrix cell.
 * @param self
 * @param func
 * @return
 */
fun RealMatrix.map(func: (Int, Int, Double) -> Double): RealMatrix {
    val res = this.copy();
    res.walkInColumnOrder(object : DefaultRealMatrixChangingVisitor() {
        override fun visit(row: Int, column: Int, value: Double): Double {
            return func(row, column, value);
        }
    })
    return res;
}

operator fun RealMatrix.plus(other: RealMatrix): RealMatrix {
    return this.add(other)
}

/**
 * A diagonal matrix with the equal numbers
 */
fun identityMatrix(dim: Int, num: Number): RealMatrix {
    return DiagonalMatrix(DoubleArray(dim) { num.toDouble() });
}

/**
 * Identity matrix
 */
fun identityMatrix(dim: Int): RealMatrix {
    return DiagonalMatrix(DoubleArray(dim) { 1.0 });
}

/**
 * Add identity matrix x num to this matrix
 * @param self
 * @param num
 * @return
 */
operator fun RealMatrix.plus(num: Number): RealMatrix {
    return this.add(identityMatrix(this.rowDimension, num))
}

operator fun RealMatrix.minus(num: Number): RealMatrix {
    return this.subtract(identityMatrix(this.rowDimension, num))
}

operator fun RealMatrix.minus(other: RealMatrix): RealMatrix {
    return this.subtract(other)
}

operator fun RealMatrix.unaryMinus(): RealMatrix {
    return this.map { _, _, v -> -v };
}

operator fun RealMatrix.times(num: Number): RealMatrix {
    return this.scalarMultiply(num.toDouble())
}

operator fun RealMatrix.times(vector: RealVector): RealVector {
    return this.operate(vector);
}

operator fun RealMatrix.div(num: Number): RealMatrix {
    return this.scalarMultiply(1.0 / num.toDouble())
}

operator fun RealMatrix.get(i1: Int, i2: Int): Double {
    return this.getEntry(i1, i2);
}

operator fun RealMatrix.set(i1: Int, i2: Int, value: Number) {
    this.setEntry(i1, i2, value.toDouble());
}

//function extensions

operator fun UnivariateFunction.plus(function: (Double) -> Double): UnivariateFunction {
    return UnivariateFunction { x -> this.value(x) + function(x) }
}

operator fun UnivariateFunction.plus(num: Number): UnivariateFunction {
    return UnivariateFunction { x -> this.value(x) + num.toDouble() }
}

operator fun UnivariateFunction.minus(function: (Double) -> Double): UnivariateFunction {
    return UnivariateFunction { x -> this.value(x) - function(x) }
}

operator fun UnivariateFunction.minus(num: Number): UnivariateFunction {
    return UnivariateFunction { x -> this.value(x) - num.toDouble() }
}

operator fun UnivariateFunction.times(function: (Double) -> Double): UnivariateFunction {
    return UnivariateFunction { x -> this.value(x) * function(x) }
}

operator fun UnivariateFunction.times(num: Number): UnivariateFunction {
    return UnivariateFunction { x -> this.value(x) * num.toDouble() }
}

operator fun UnivariateFunction.div(function: (Double) -> Double): UnivariateFunction {
    return UnivariateFunction { x -> this.value(x) / function(x) }
}

operator fun UnivariateFunction.div(num: Number): UnivariateFunction {
    return UnivariateFunction { x -> this.value(x) / num.toDouble() }
}

operator fun UnivariateFunction.unaryMinus(): UnivariateFunction {
    return UnivariateFunction { x -> -this.value(x) }
}

operator fun UnivariateFunction.invoke(num: Number): Double {
    return this.value(num.toDouble());
}

operator fun UnivariateFunction.invoke(vector: RealVector): RealVector {
    return vector.map(this);
}

operator fun UnivariateFunction.invoke(array: DoubleArray): DoubleArray {
    return array.toVector().map(this).toArray();
}


//number extensions

operator fun Number.plus(other: RealVector): RealVector {
    return other + this
}

operator fun Number.minus(other: RealVector): RealVector {
    return (-other) + this
}

operator fun Number.times(other: RealVector): RealVector {
    return other * this;
}

operator fun Number.plus(other: RealMatrix): RealMatrix {
    return other + this
}

operator fun Number.minus(other: RealMatrix): RealMatrix {
    return (-other) + this
}

operator fun Number.times(other: RealMatrix): RealMatrix {
    return other * this;
}

operator fun Number.plus(other: UnivariateFunction): UnivariateFunction {
    return other + this
}

operator fun Number.minus(other: UnivariateFunction): UnivariateFunction {
    return -other + this
}

operator fun Number.times(other: UnivariateFunction): UnivariateFunction {
    return other * this
}

//TODO differentiable functions algebra

//fun UnivariateDifferentiableFunction plus(final Number this, UnivariateDifferentiableFunction other) {
//    return other + this
//}
//
//fun UnivariateDifferentiableFunction minus(final Number this, UnivariateDifferentiableFunction other) {
//    return (-other) + this
//}
//
//fun UnivariateDifferentiableFunction multiply(final Number this, UnivariateDifferentiableFunction other) {
//    return other * this;
//}