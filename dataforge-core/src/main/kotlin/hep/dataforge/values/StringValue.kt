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

import hep.dataforge.exceptions.ValueConversionException
import java.text.NumberFormat
import java.text.ParseException
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeParseException
import java.util.*

/**
 * TODO заменить интерфейсы на используемые в javax.jcr
 *
 * @author Alexander Nozik
 */
internal class StringValue
/**
 * Если передается строка в кавычках, то кавычки откусываются
 *
 * @param value a [String] object.
 */
(value: String) : AbstractValue() {

    /**
     * {@inheritDoc}
     *
     *
     * Всегда возвращаем строковое значение строки в ковычках чтобы избежать
     * проблем с пробелами
     */
    override val string: String

    //    public StringValue(Boolean value) {
    //        this.value = value.toString();
    //    }

    /**
     * {@inheritDoc}
     */
    override val boolean: Boolean
        get() {
            try {
                return java.lang.Boolean.valueOf(string)
            } catch (ex: NumberFormatException) {
                throw ValueConversionException(this, ValueType.BOOLEAN)
            }

        }

    /**
     * {@inheritDoc}
     */
    override val number: Number
        get() {
            try {
                return NumberFormat.getInstance().parse(string)
            } catch (ex: ParseException) {
                throw ValueConversionException(this, ValueType.NUMBER)
            } catch (ex: NumberFormatException) {
                throw ValueConversionException(this, ValueType.NUMBER)
            }

        }

    /**
     * {@inheritDoc}
     */
    override val time: Instant
        get() {
            try {
                return if (string.endsWith("Z")) {
                    Instant.parse(string)
                } else {
                    LocalDateTime.parse(string).toInstant(ZoneOffset.UTC)
                }
            } catch (ex: DateTimeParseException) {
                throw ValueConversionException(this, ValueType.TIME)
            }

        }

    /**
     * {@inheritDoc}
     */
    override val type: ValueType
        get() = ValueType.STRING

    /**
     * {@inheritDoc}
     */
    override fun hashCode(): Int {
        var hash = 3
        hash = 47 * hash + Objects.hashCode(this.string)
        return hash
    }

    /**
     * {@inheritDoc}
     */
    override fun equals(other: Any?): Boolean {
        return when (other) {
            null -> false
            is Value -> this.string == other.string
            else -> false
        }
    }

    init {
        if (value.startsWith("\"") && value.endsWith("\"")) {
            this.string = value.substring(1, value.length - 1)
        } else {
            this.string = value
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun toString(): String {
        return "\"" + string + "\""
    }

    override val value: Any
        get() {
            return this.string
        }
}
