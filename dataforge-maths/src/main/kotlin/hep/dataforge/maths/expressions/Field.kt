/*
 * Copyright  2018 Alexander Nozik.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package hep.dataforge.maths.expressions

/**
 * A context for mathematical operations on specific type of numbers. The input could be defined as any number,
 * but the output is always typed as given type.
 * @param T - input type for operation arguments
 * @param N - the type of operation result. In general supposed to be subtype of T
 */
interface Field<in T, out N> {
    val one: N
    val zero: N

    fun add(a: T, b: T): N

    /**
     * Arithmetical sum of arguments.
     * Looks like  N plus(a:Number, b: Number) in java
     */
    operator fun T.plus(b: T): N {
        return add(this, b)
    }

    fun subtract(a: T, b: T): N

    /**
     * Second argument subtracted from the first
     */
    operator fun T.minus(b: T): N {
        return subtract(this, b)
    }

    fun divide(a: T, b: T): N

    /**
     * Division
     */
    operator fun T.div(b: T): N {
        return divide(this, b)
    }

    fun multiply(a: T, b: T): N

    /**
     * Multiplication
     */
    operator fun T.times(b: T): N {
        return multiply(this, b)
    }

    fun negate(a: T): N

    /**
     * Negation
     */
    operator fun T.unaryMinus(): N {
        return negate(this)
    }

    /**
     * Transform from input type to output type.
     * Throws an exception if transformation is not available.
     */
    fun transform(n: T): N

}

/**
 * Additional operations that could be performed on numbers in context
 */
interface ExtendedField<in T, out N> : Field<T, N> {
    fun sin(n: T): N
    fun cos(n: T): N
    fun exp(n: T): N
    fun pow(n: T, p: T): N

//    fun reminder(a: T, b: T): N
    //TODO etc
}

/**
 * Additional tools to work with expressions
 */
interface VariableField<in T, out N : Number> : Field<T, N> {
    /**
     * define a variable and its value
     */
    fun variable(name: String, value: Number): N
}


/**
 * Backward compatibility class for connoms-math/commons-numbers field/
 */
abstract class FieldCompat<T : Number, out N : Number, out C : Field<T, N>>(val nc: C) : Number() {
    abstract val self: T
    operator fun plus(n: T): N {
        return with(nc) { self.plus(n) }
    }

    operator fun minus(n: T): N {
        return with(nc) { self.minus(n) }
    }

    operator fun times(n: T): N {
        return with(nc) { self.times(n) }
    }

    operator fun div(n: T): N {
        return with(nc) { self.div(n) }
    }

    operator fun unaryMinus(): N {
        return with(nc) { self.unaryMinus() }
    }

    //A temporary fix for https://youtrack.jetbrains.com/issue/KT-22972
    abstract override fun toByte(): Byte

    abstract override fun toChar(): Char

    abstract override fun toDouble(): Double

    abstract override fun toFloat(): Float

    abstract override fun toInt(): Int

    abstract override fun toLong(): Long

    abstract override fun toShort(): Short

}