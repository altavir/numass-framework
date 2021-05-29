package hep.dataforge

import java.math.BigDecimal
import java.math.MathContext

/**
 * Extension of basic java classes
 * Created by darksnake on 29-Apr-17.
 */

//Number

/**
 * Convert a number to BigDecimal
 */
fun Number.toBigDecimal(mathContext: MathContext =  MathContext.UNLIMITED): BigDecimal {
    return when(this){
        is BigDecimal -> this
        is Int, is Long, is Double, is Float -> this.toBigDecimal(mathContext)
        else -> this.toDouble().toBigDecimal(mathContext)
    }
}

operator fun Number.plus(other: Number): Number {
    return this.toBigDecimal().add(other.toBigDecimal());
}

operator fun Number.minus(other: Number): Number {
    return this.toBigDecimal().subtract(other.toBigDecimal());
}

operator fun Number.div(other: Number): Number {
    return this.toBigDecimal().divide(other.toBigDecimal());
}

operator fun Number.times(other: Number): Number {
    return this.toBigDecimal().multiply(other.toBigDecimal());
}

operator fun Number.compareTo(other: Number): Int {
    return this.toBigDecimal().compareTo(other.toBigDecimal());
}

/**
 * Generate iterable sequence from range
 */
infix fun ClosedFloatingPointRange<Double>.step(step: Double): Sequence<Double> {
    return generateSequence(this.start) {
        val res = it + step;
        if (res > this.endInclusive) {
            null
        } else {
            res
        }
    }
}