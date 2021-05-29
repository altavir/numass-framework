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
package hep.dataforge.names

import java.util.*

/**
 * Единичное имя с возможным запросом. На данный момент проверки правильности
 * задания имени при создании не производится
 *
 * @author Alexander Nozik
 */
internal class NameToken(singlet: String) : Name {


    private val theName: String

    private val theQuery: String?

    override val first: Name
        get() = this

    override val last: Name
        get() = this

    override val query: String
        get() = theQuery ?: ""

    override val length: Int
        get() = 1

    override val tokens: List<Name>
        get() = listOf<Name>(this)

    override fun isEmpty(): Boolean = false

    init {
        //unescape string
        val unescaped = singlet.replace("\\.", ".")
        if (unescaped.matches(".*\\[.*]".toRegex())) {
            val bracketIndex = unescaped.indexOf("[")
            this.theName = unescaped.substring(0, bracketIndex)
            this.theQuery = unescaped.substring(bracketIndex + 1, unescaped.lastIndexOf("]"))
        } else {
            this.theName = unescaped
            this.theQuery = null
        }
    }

    override fun cutFirst(): Name {
        return Name.EMPTY
    }

    override fun cutLast(): Name {
        return Name.EMPTY
    }

    override fun hasQuery(): Boolean {
        return theQuery != null
    }

    override fun ignoreQuery(): NameToken {
        return if (!hasQuery()) {
            this
        } else {
            NameToken(theName)
        }
    }

    override fun toString(): String {
        return unescaped.replace(".", "\\.")
    }

    override fun entry(): String {
        return theName
    }


    override fun asArray(): Array<String> {
        return arrayOf(unescaped)
    }

    override fun hashCode(): Int {
        var hash = 7
        hash = 79 * hash + Objects.hashCode(this.unescaped)
        return hash
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) {
            return false
        }
        if (javaClass != other.javaClass) {
            return false
        }
        return unescaped == (other as? NameToken)?.unescaped
    }

    /**
     * The full name including query but without escaping
     */
    override val unescaped: String
        get() {
            return if (theQuery != null) {
                String.format("%s[%s]", theName, theQuery)
            } else {
                theName
            }
        }
}
