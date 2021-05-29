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

import hep.dataforge.names.AlphanumComparator
import hep.dataforge.utils.NamingUtils
import java.io.Serializable
import java.nio.ByteBuffer
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeParseException
import java.util.stream.Stream
import kotlin.streams.toList

/**
 * The list of supported Value types.
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
enum class ValueType {
    BINARY, NUMBER, BOOLEAN, STRING, TIME, NULL
}

/**
 * An immutable wrapper class that can hold Numbers, Strings and Instant
 * objects. The general contract for Value is that it is immutable, more
 * specifically, it can't be changed after first call (it could be lazy
 * calculated though)
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
interface Value : Serializable, Comparable<Value> {

    /**
     * The number representation of this value
     *
     * @return a [Number] object.
     */
    val number: Number

    /**
     * Boolean representation of this Value
     *
     * @return a boolean.
     */
    val boolean: Boolean

    @JvmDefault
    val double: Double
        get() = number.toDouble()

    @JvmDefault
    val int: Int
        get() = number.toInt()

    @JvmDefault
    val long: Long
        get() = number.toLong()

    /**
     * Instant representation of this Value
     *
     * @return
     */
    val time: Instant

    @JvmDefault
    val binary: ByteBuffer
        get() = ByteBuffer.wrap(string.toByteArray())

    /**
     * The String representation of this value
     *
     * @return a [String] object.
     */
    val string: String

    val type: ValueType

    /**
     * Return list of values representation of current value. If value is
     * instance of ListValue, than the actual list is returned, otherwise
     * immutable singleton list is returned.
     *
     * @return
     */
    @JvmDefault
    val list: List<Value>
        get() = listOf(this)

    @JvmDefault
    val isNull: Boolean
        get() = this.type == ValueType.NULL

    /**
     * True if it is a list value
     *
     * @return
     */
    @JvmDefault
    val isList: Boolean
        get() = false

    /**
     * Return underlining object. Used for dynamic calls mostly
     */
    val value: Any

    @JvmDefault
    override fun compareTo(other: Value): Int {
        return when (type) {
            ValueType.NUMBER -> ValueUtils.NUMBER_COMPARATOR.compare(number, other.number)
            ValueType.BOOLEAN -> boolean.compareTo(other.boolean)
            ValueType.STRING -> AlphanumComparator.INSTANCE.compare(this.string, other.string)
            ValueType.TIME -> time.compareTo(other.time)
            ValueType.NULL -> if (other.type == ValueType.NULL) 0 else -1
            ValueType.BINARY -> binary.compareTo(other.binary)
        }
    }

    companion object {
        const val NULL_STRING = "@null"

        val NULL: Value = object : Value {
            override val boolean: Boolean = false
            override val double: Double = java.lang.Double.NaN
            override val number: Number = 0
            override val time: Instant = Instant.MIN
            override val string: String = "@null"
            override val type: ValueType = ValueType.NULL
            override val value: Any = double
        }

        fun of(list: Array<Any>): Value {
            return list.map(::of).asValue()
        }

        fun of(list: Collection<Any?>): Value {
            return list.map(::of).asValue()
        }

        /**
         * Reflection based Value resolution
         *
         * @param obj a [Object] object.
         * @return a [Value] object.
         */
        fun of(value: Any?): Value {
            return when (value) {
                null -> Value.NULL
                is Value -> value
                is Number -> NumberValue(value)
                is Instant -> TimeValue(value)
                is LocalDateTime -> TimeValue(value)
                is Boolean -> BooleanValue.ofBoolean(value)
                is String -> StringValue(value)
                is Collection<Any?> -> Value.of(value)
                is Stream<*> -> Value.of(value.toList())
                is Array<*> -> Value.of(value.map(::of))
                is Enum<*> -> StringValue(value.name)
                else -> StringValue(value.toString())
            }
        }
    }
}

/**
 * Java compatibility layer
 */
object ValueFactory {
    @JvmField
    val NULL = Value.NULL

    @JvmStatic
    fun of(value: Any?): Value = if (value is String) {
        value.parseValue()
    } else {
        Value.of(value)
    }

    @JvmStatic
    @JvmOverloads
    fun parse(value: String, lazy: Boolean = true): Value {
        return if (lazy) {
            LateParseValue(value)
        } else {
            value.parseValue()
        }
    }
}


fun String.asValue(): Value {
    return StringValue(this)
}

/**
 * create a boolean Value
 *
 * @param b a boolean.
 * @return a [Value] object.
 */
fun Boolean.asValue(): Value {
    return BooleanValue.ofBoolean(this)
}

fun Number.asValue(): Value {
    return NumberValue(this)
}

fun LocalDateTime.asValue(): Value {
    return TimeValue(this)
}

fun Instant.asValue(): Value {
    return TimeValue(this)
}

fun Iterable<Value>.asValue(): Value {
    val list = this.toList()
    return when (list.size) {
        0 -> Value.NULL
        1 -> list[0]
        else -> ListValue(list)
    }
}

val Value?.nullableDouble: Double?
    get() = this?.let { if (isNull) null else double }


//fun asValue(list: Collection<Any>): Value {
//    return when {
//        list.isEmpty() -> Value.NULL
//        list.size == 1 -> asValue(list.first())
//        else -> ListValue(list.map { Value.of(it) })
//    }
//}


/**
 * Create Value from String using closest match conversion
 *
 * @param str a [String] object.
 * @return a [Value] object.
 */
fun String.parseValue(): Value {

    //Trying to get integer
    if (isEmpty()) {
        return Value.NULL
    }

    //string constants
    if (startsWith("\"") && endsWith("\"")) {
        return StringValue(substring(1, length - 2))
    }

    try {
        val `val` = Integer.parseInt(this)
        return Value.of(`val`)
    } catch (ignored: NumberFormatException) {
    }

    //Trying to get double
    try {
        val `val` = java.lang.Double.parseDouble(this)
        return Value.of(`val`)
    } catch (ignored: NumberFormatException) {
    }

    //Trying to get Instant
    try {
        val `val` = Instant.parse(this)
        return Value.of(`val`)
    } catch (ignored: DateTimeParseException) {
    }

    //Trying to parse LocalDateTime
    try {
        val `val` = LocalDateTime.parse(this).toInstant(ZoneOffset.UTC)
        return Value.of(`val`)
    } catch (ignored: DateTimeParseException) {
    }

    if ("true" == this || "false" == this) {
        return BooleanValue.ofBoolean(this)
    }

    if (startsWith("[") && endsWith("]")) {
        //FIXME there will be a problem with nested lists because of splitting
        val strings = NamingUtils.parseArray(this)
        return Value.of(strings)
    }

    //Give up and return a StringValue
    return StringValue(this)
}
