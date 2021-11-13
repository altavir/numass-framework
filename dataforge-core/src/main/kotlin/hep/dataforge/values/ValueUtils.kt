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

package hep.dataforge.values

import hep.dataforge.io.IOUtils
import hep.dataforge.providers.Path
import hep.dataforge.providers.Provider
import hep.dataforge.toBigDecimal
import java.io.DataInput
import java.io.DataOutput
import java.io.IOException
import java.io.Serializable
import java.math.BigDecimal
import java.math.BigInteger
import java.nio.ByteBuffer
import java.time.Instant
import java.util.*

data class ValueRange(override val start: Value, override val endInclusive: Value) : ClosedRange<Value> {
    operator fun contains(any: Any): Boolean {
        return contains(Value.of(any))
    }
}

operator fun Value.rangeTo(other: Value): ValueRange = ValueRange(this, other)

/**
 * Read a string value as an enum
 */
inline fun <reified T : Enum<T>> ValueProvider.getEnum(name: String): T {
    return enumValueOf(getString(name))
}

/**
 * Read a string value as an enum with default. If string field does not represent valid enum, an exception is thrown and default is ignored
 */
inline fun <reified T : Enum<T>> ValueProvider.getEnum(name: String, default: T): T {
    return optString(name).map { enumValueOf<T>(it) }.orElse(default)
}

/**
 * Created by darksnake on 06-Aug-16.
 */
object ValueUtils {

    val NUMBER_COMPARATOR: Comparator<Number> = NumberComparator()


    /**
     * Build a meta provider from given general provider
     *
     * @param provider
     * @return
     */
    @JvmStatic
    fun Provider.asValueProvider(): ValueProvider {
        return this as? ValueProvider ?: object : ValueProvider {
            override fun optValue(path: String): Optional<Value> {
                return this@asValueProvider.provide(Path.of(path, ValueProvider.VALUE_TARGET)).map<Value> { Value::class.java.cast(it) }
            }
        }
    }

    private class NumberComparator : Comparator<Number>, Serializable {

        override fun compare(x: Number, y: Number): Int {
            val d1 = x.toDouble()
            val d2 = y.toDouble()
            return if ((d1 != 0.0 || d2 != 0.0) && Math.abs(d1 - d2) / Math.max(d1, d2) < RELATIVE_NUMERIC_PRECISION) {
                0
            } else if (isSpecial(x) || isSpecial(y)) {
                java.lang.Double.compare(d1, d2)
            } else {
                toBigDecimal(x).compareTo(toBigDecimal(y))
            }
        }

        companion object {
            private const val RELATIVE_NUMERIC_PRECISION = 1e-5

            private fun isSpecial(x: Number): Boolean {
                val specialDouble = x is Double && (java.lang.Double.isNaN(x) || java.lang.Double.isInfinite(x))
                val specialFloat = x is Float && (java.lang.Float.isNaN(x) || java.lang.Float.isInfinite(x))
                return specialDouble || specialFloat
            }

            private fun toBigDecimal(number: Number): BigDecimal {
                if (number is BigDecimal) {
                    return number
                }
                if (number is BigInteger) {
                    return BigDecimal(number)
                }
                if (number is Byte || number is Short
                        || number is Int || number is Long) {
                    return BigDecimal(number.toLong())
                }
                if (number is Float || number is Double) {
                    return BigDecimal(number.toDouble())
                }

                try {
                    return BigDecimal(number.toString())
                } catch (e: NumberFormatException) {
                    throw RuntimeException("The given number (\"" + number
                            + "\" of class " + number.javaClass.name
                            + ") does not have a parsable string representation", e)
                }

            }
        }
    }

}

/**
 * Fast and compact serialization for values
 *
 * @param oos
 * @param value
 * @throws IOException
 */
@Throws(IOException::class)
fun DataOutput.writeValue(value: Value) {
    if (value.isList) {
        writeByte('*'.code) // List designation
        writeShort(value.list.size)
        for (subValue in value.list) {
            writeValue(subValue)
        }
    } else {
        when (value.type) {
            ValueType.NULL -> writeChar('0'.code) // null
            ValueType.TIME -> {
                writeByte('T'.code)//Instant
                writeLong(value.time.epochSecond)
                writeLong(value.time.nano.toLong())
            }
            ValueType.STRING -> {
                this.writeByte('S'.code)//String
                IOUtils.writeString(this, value.string)
            }
            ValueType.NUMBER -> {
                val num = value.number
                when (num) {
                    is Double -> {
                        writeByte('D'.code) // double
                        writeDouble(num.toDouble())
                    }
                    is Int -> {
                        writeByte('I'.code) // integer
                        writeInt(num.toInt())
                    }
                    is Long -> {
                        writeByte('L'.code)
                        writeLong(num.toLong())
                    }
                    else -> {
                        writeByte('N'.code) // BigDecimal
                        val decimal = num.toBigDecimal()
                        val bigInt = decimal.unscaledValue().toByteArray()
                        val scale = decimal.scale()
                        writeShort(bigInt.size)
                        write(bigInt)
                        writeInt(scale)
                    }
                }
            }
            ValueType.BOOLEAN -> if (value.boolean) {
                writeByte('+'.code) //true
            } else {
                writeByte('-'.code) // false
            }
            ValueType.BINARY -> {
                val binary = value.binary
                writeByte('X'.code)
                writeInt(binary.limit())
                write(binary.array())
            }
        }
    }
}

/**
 * Value deserialization
 */
fun DataInput.readValue(): Value {
    val type = readByte().toInt().toChar()
    when (type) {
        '*' -> {
            val listSize = readShort()
            val valueList = ArrayList<Value>()
            for (i in 0 until listSize) {
                valueList.add(readValue())
            }
            return Value.of(valueList)
        }
        '0' -> return Value.NULL
        'T' -> {
            val time = Instant.ofEpochSecond(readLong(), readLong())
            return time.asValue()
        }
        'S' -> return IOUtils.readString(this).asValue()
        'D' -> return readDouble().asValue()
        'I' -> return readInt().asValue()
        'L' -> return readLong().asValue()
        'N' -> {
            val intSize = readShort()
            val intBytes = ByteArray(intSize.toInt())
            readFully(intBytes)
            val scale = readInt()
            val bdc = BigDecimal(BigInteger(intBytes), scale)
            return bdc.asValue()
        }
        'X' -> {
            val length = readInt()
            val buffer = ByteArray(length)
            readFully(buffer)
            return BinaryValue(ByteBuffer.wrap(buffer))
        }
        '+' -> return BooleanValue.TRUE
        '-' -> return BooleanValue.FALSE
        else -> throw RuntimeException("Wrong value serialization format. Designation $type is unexpected")
    }
}


fun ByteBuffer.getValue(): Value {
    val type = get().toInt().toChar()
    when (type) {
        '*' -> {
            val listSize = getShort()
            val valueList = ArrayList<Value>()
            for (i in 0 until listSize) {
                valueList.add(getValue())
            }
            return Value.of(valueList)
        }
        '0' -> return Value.NULL
        'T' -> {
            val time = Instant.ofEpochSecond(getLong(), getLong())
            return time.asValue()
        }
        'S' -> {
            val length = getInt()
            val buffer = ByteArray(length)
            get(buffer)
            return String(buffer, Charsets.UTF_8).asValue()
        }
        'D' -> return getDouble().asValue()
        'I' -> return getInt().asValue()
        'L' -> return getLong().asValue()
        'N' -> {
            val intSize = getShort()
            val intBytes = ByteArray(intSize.toInt())
            get()
            val scale = getInt()
            val bdc = BigDecimal(BigInteger(intBytes), scale)
            return bdc.asValue()
        }
        'X' -> {
            val length = getInt()
            val buffer = ByteArray(length)
            get(buffer)
            return BinaryValue(ByteBuffer.wrap(buffer))
        }
        '+' -> return BooleanValue.TRUE
        '-' -> return BooleanValue.FALSE
        else -> throw RuntimeException("Wrong value serialization format. Designation $type is unexpected")
    }
}

fun ByteBuffer.putValue(value: Value) {
    if (value.isList) {
        put('*'.code.toByte()) // List designation
        if (value.list.size > Short.MAX_VALUE) {
            throw RuntimeException("The array values of size more than ${Short.MAX_VALUE} could not be serialized")
        }
        putShort(value.list.size.toShort())
        value.list.forEach { putValue(it) }
    } else {
        when (value.type) {
            ValueType.NULL -> put('0'.code.toByte()) // null
            ValueType.TIME -> {
                put('T'.code.toByte())//Instant
                putLong(value.time.epochSecond)
                putLong(value.time.nano.toLong())
            }
            ValueType.STRING -> {
                put('S'.code.toByte())//String
                if (value.string.length > Int.MAX_VALUE) {
                    throw RuntimeException("The string valuse of size more than ${Int.MAX_VALUE} could not be serialized")
                }
                put(value.string.toByteArray(Charsets.UTF_8))
            }
            ValueType.NUMBER -> {
                val num = value.number
                when (num) {
                    is Double -> {
                        put('D'.code.toByte()) // double
                        putDouble(num.toDouble())
                    }
                    is Int -> {
                        put('I'.code.toByte()) // integer
                        putInt(num.toInt())
                    }
                    is Long -> {
                        put('L'.code.toByte())
                        putLong(num.toLong())
                    }
                    is BigDecimal -> {
                        put('N'.code.toByte()) // BigDecimal
                        val bigInt = num.unscaledValue().toByteArray()
                        val scale = num.scale()
                        if (bigInt.size > Short.MAX_VALUE) {
                            throw RuntimeException("Too large BigDecimal")
                        }
                        putShort(bigInt.size.toShort())
                        put(bigInt)
                        putInt(scale)
                    }
                    else -> {
                        throw RuntimeException("Custom number serialization is not allowed. Yet")
                    }
                }
            }
            ValueType.BOOLEAN -> if (value.boolean) {
                put('+'.code.toByte()) //true
            } else {
                put('-'.code.toByte()) // false
            }
            ValueType.BINARY -> {
                put('X'.code.toByte())
                val binary = value.binary
                putInt(binary.limit())
                put(binary.array())
            }
        }
    }
}
