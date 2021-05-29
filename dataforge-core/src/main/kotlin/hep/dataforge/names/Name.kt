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

import java.util.stream.Stream
import java.util.stream.StreamSupport
import kotlin.streams.toList

/**
 *
 * The general interface for working with names.
 * The name is a dot separated list of strings like `token1.token2.token3`.
 * Each token could contain additional query in square brackets. Following symbols are prohibited in name tokens: `{}.:\`.
 * Escaped dots (`\.`) are ignored.
 * Square brackets are allowed only to designate queries.
 *
 *
 * The [Name] is not connected with [javax.naming.Name] because DataForge does not need JNDI. No need to declare the dependency.
 *
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
interface Name : Comparable<Name> {

    /**
     * Query for last elements without brackets
     *
     * @return a [java.lang.String] object.
     */
    val query: String

    /**
     * The number of tokens in this name
     *
     * @return a int.
     */
    val length: Int

    /**
     * First token
     *
     * @return a [hep.dataforge.names.Name] object.
     */
    val first: Name

    /**
     * Last token
     *
     * @return a [hep.dataforge.names.Name] object.
     */
    val last: Name

    /**
     * Get the list of contained tokens
     *
     * @return
     */
    val tokens: List<Name>

    /**
     * Returns true only for EMPTY name
     *
     * @return
     */
    fun isEmpty(): Boolean


    /**
     * The name as a String including query and escaping
     *
     * @return
     */
    override fun toString(): String

    /**
     * if has query for the last element
     *
     * @return a boolean.
     */
    fun hasQuery(): Boolean

    /**
     * This name without last element query. If there is no query, returns
     * itself
     *
     * @return
     */
    fun ignoreQuery(): Name

    /**
     * The whole name but the first token
     *
     * @return a [hep.dataforge.names.Name] object.
     */
    fun cutFirst(): Name

    /**
     * The whole name but the lat token
     *
     * @return a [hep.dataforge.names.Name] object.
     */
    fun cutLast(): Name


    /**
     * Return the leading name without query
     *
     * @return a [java.lang.String] object.
     */
    fun entry(): String

    /**
     * Create a new name with given name appended to the end of this one
     *
     * @param name
     * @return
     */
    @JvmDefault
    operator fun plus(name: Name): Name {
        return join(this, name)
    }

    /**
     * Append a name to the end of this name treating new name as a single name segment
     *
     * @param name
     * @return
     */
    @JvmDefault
    operator fun plus(name: String): Name {
        return join(this, ofSingle(name))
    }

    fun asArray(): Array<String>

    @JvmDefault
    fun equals(name: String): Boolean {
        return this.toString() == name
    }

    override fun compareTo(other: Name): Int {
        return this.toString().compareTo(other.toString())
    }

    /**
     * Convert to string without escaping separators
     *
     * @return
     */
    val unescaped: String

    companion object {


        const val NAME_TOKEN_SEPARATOR = "."

        val EMPTY: Name = EmptyName()

        /**
         *
         */
        fun empty(): Name {
            return EMPTY
        }

        fun of(name: String?): Name {
            if (name == null || name.isEmpty()) {
                return EMPTY
            }
            val tokens = name.split("(?<!\\\\)\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            return if (tokens.size == 1) {
                NameToken(name)
            } else {
                of(Stream.of(*tokens).map<NameToken>{ NameToken(it) }.toList())
            }
        }

        /**
         * Build name from string ignoring name token separators and treating it as a single name token
         *
         * @param name
         * @return
         */
        fun ofSingle(name: String): Name {
            return if (name.isEmpty()) {
                EMPTY
            } else {
                NameToken(name)
            }
        }

        /**
         * Join all segments in the given order. Segments could be composite.
         *
         * @param segments
         * @return a [hep.dataforge.names.Name] object.
         */
        fun join(vararg segments: String): Name {
            if (segments.isEmpty()) {
                return EMPTY
            } else if (segments.size == 1) {
                return of(segments[0])
            }

            return of(Stream.of(*segments).filter { it -> !it.isEmpty() }.map<Name>{ of(it) }.toList())
        }

        fun joinString(vararg segments: String): String {
            return segments.joinToString(NAME_TOKEN_SEPARATOR)
        }

        fun join(vararg segments: Name): Name {
            if (segments.isEmpty()) {
                return EMPTY
            } else if (segments.size == 1) {
                return segments[0]
            }

            return of(Stream.of(*segments).filter { it -> !it.isEmpty() }.toList())
        }

        fun of(tokens: Iterable<String>): Name {
            return of(StreamSupport.stream(tokens.spliterator(), false)
                    .filter { str -> !str.isEmpty() }
                    .map<NameToken>{ NameToken(it) }.toList())
        }

        fun of(tokens: List<Name>): Name {
            return when {
                tokens.isEmpty() -> EMPTY
                tokens.size == 1 -> tokens[0]
                else -> CompositeName.of(tokens)
            }
        }
    }
}
