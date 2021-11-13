package hep.dataforge.maths.expressions

import hep.dataforge.names.NameList
import org.apache.commons.math3.analysis.differentiation.DerivativeStructure

class DSNumber(val ds: DerivativeStructure, nc: DSField) : FieldCompat<Number, DSNumber, DSField>(nc) {
    override val self: DSNumber = this

    fun deriv(parName: String): Double {
        return deriv(mapOf(parName to 1))
    }

    fun deriv(orders: Map<String, Int>): Double {
        return ds.getPartialDerivative(*nc.names.map { orders[it] ?: 0 }.toIntArray())
    }

    override fun toByte(): Byte = ds.value.toInt().toByte()

    override fun toChar(): Char = ds.value.toInt().toChar()

    override fun toDouble(): Double = ds.value

    override fun toFloat(): Float = ds.value.toFloat()

    override fun toInt(): Int = ds.value.toInt()

    override fun toLong(): Long = ds.value.toLong()

    override fun toShort(): Short = ds.value.toInt().toShort()

    /**
     * Return new DSNumber, obtained by applying given function to underlying ds
     */
    inline fun eval(func: (DerivativeStructure) -> DerivativeStructure): DSNumber {
        return DSNumber(func(ds), nc)
    }
}

class DSField(val order: Int, val names: NameList) : VariableField<Number, DSNumber>, ExtendedField<Number, DSNumber> {

    override val one: DSNumber = transform(1.0)

    override val zero: DSNumber = transform( 0.0)

    constructor(order: Int, vararg names: String) : this(order, NameList(*names))

    override fun transform(n: Number): DSNumber {
        return if (n is DSNumber) {
            if (n.nc.names == this.names) {
                n
            } else {
                //TODO add conversion
                throw RuntimeException("Names mismatch in derivative structure")
            }
        } else {
            DSNumber(DerivativeStructure(names.size(), order, n.toDouble()), this)
        }
    }

    override fun variable(name: String, value: Number): DSNumber {
        if (!names.contains(name)) {
            //TODO add conversions probably
            throw RuntimeException("Name $name is not a part of the number context")
        }
        return DSNumber(DerivativeStructure(names.size(), order, names.getNumberByName(name), value.toDouble()), this)
    }

    override fun add(a: Number,b: Number): DSNumber {
        return if (b is DSNumber) {
            transform(a).eval { it.add(b.ds) }
        } else {
            transform(a).eval { it.add(b.toDouble()) }
        }
    }

    override fun subtract(a: Number,b: Number): DSNumber {
        return if (b is DSNumber) {
            transform(a).eval { it.subtract(b.ds) }
        } else {
            transform(a).eval { it.subtract(b.toDouble()) }
        }

    }

    override fun divide(a: Number,b: Number): DSNumber {
        return if (b is DSNumber) {
            transform(a).eval { it.divide(b.ds) }
        } else {
            transform(a).eval { it.divide(b.toDouble()) }
        }

    }

    override fun negate(a: Number): DSNumber {
        return (a as? DSNumber)?.eval { it.negate() } ?: transform(-a.toDouble())
    }

    override fun multiply(a: Number,b: Number): DSNumber {
        return when (b) {
            is DSNumber -> transform(a).eval { it.multiply(b.ds) }
            is Int -> transform(a).eval { it.multiply(b) }
            else -> transform(a).eval { it.multiply(b.toDouble()) }
        }

    }


    override fun sin(n: Number): DSNumber {
        return transform(n).eval { it.sin() }
    }

    override fun cos(n: Number): DSNumber {
        return transform(n).eval { it.cos() }
    }

    override fun exp(n: Number): DSNumber {
        return transform(n).eval { it.exp() }
    }

    override fun pow(n: Number, p: Number): DSNumber {
        return when (p) {
            is Int -> transform(n).eval { it.pow(p) }
            is DSNumber -> transform(n).eval { it.pow(p.ds) }
            else -> transform(n).eval { it.pow(p.toDouble()) }
        }
    }
}