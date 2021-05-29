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

import hep.dataforge.exceptions.NamingException
import java.util.*
import java.util.stream.Collectors
import kotlin.streams.toList

/**
 * The name path composed of tokens
 *
 * @author Alexander Nozik
 */
internal class CompositeName(private val names: LinkedList<NameToken>) : Name {

    override val first: Name
        get() = names.first

    override val last: Name
        get() = names.last

    override val query: String
        get() = names.last.query

    override val length: Int
        get() = names.size

    override fun isEmpty(): Boolean = false

    override val tokens: List<Name>
        get() = Collections.unmodifiableList<Name>(names)

    override fun cutFirst(): Name {
        when (length) {
            2 -> return names.last
            1 -> throw NamingException("Can not cut name token")
            else -> {
                val tokens = LinkedList(names)
                tokens.removeFirst()
                return CompositeName(tokens)
            }
        }
    }

    override fun cutLast(): Name {
        return when (length) {
            2 -> names.first
            1 -> throw NamingException("Can not cut name token")
            else -> {
                val tokens = LinkedList(names)
                tokens.removeLast()
                CompositeName(tokens)
            }
        }
    }

    override fun hasQuery(): Boolean {
        return names.last.hasQuery()
    }

    override fun ignoreQuery(): Name {
        //Replace last element if needed
        if (hasQuery()) {
            val tokens = LinkedList(names)
            tokens.removeLast()
            tokens.addLast(names.last.ignoreQuery())
            return CompositeName(tokens)
        } else {
            return this
        }
    }


    override fun toString(): String {
        val it = Iterable { names.stream().map<String> { it.toString() }.iterator() }
        return it.joinToString(Name.NAME_TOKEN_SEPARATOR)
    }

    override fun asArray(): Array<String> {
        return names.stream().map<String> { it.toString() }.toList().toTypedArray()
    }

    override val unescaped: String
        get() {
            val it = Iterable { names.stream().map<String> { it.unescaped }.iterator() }
            return it.joinToString(Name.NAME_TOKEN_SEPARATOR)
        }

    override fun entry(): String {
        return first.entry()
    }

    override fun hashCode(): Int {
        var hash = 3
        hash = 19 * hash + Objects.hashCode(this.names)
        return hash
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) {
            return false
        }
        if (javaClass != other.javaClass) {
            return false
        }

        return (other as? CompositeName)?.names == this.names
    }

    companion object {

        fun of(tokens: List<Name>): CompositeName {
            val list = tokens.stream()
                    .flatMap { it -> it.tokens.stream() }
                    .map<NameToken> { NameToken::class.java.cast(it) }.collect(Collectors.toCollection { LinkedList<NameToken>() })
            return CompositeName(list)
        }
    }

}
