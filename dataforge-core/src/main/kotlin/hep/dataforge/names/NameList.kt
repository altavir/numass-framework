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

import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder
import hep.dataforge.meta.MetaMorph
import java.util.*
import java.util.stream.Stream
import kotlin.streams.toList

/**
 * The list of strings with cached index retrieval
 *
 * @author Alexander Nozik
 */
class NameList(list: Iterable<String>) : MetaMorph, Iterable<String> {

    private val nameList = ArrayList<String>().apply {
        //TODO check for duplicates
        addAll(list)
    }

    /**
     * An index cache to make calls of `getNumberByName` faster
     */
    @Transient
    private val indexCache = HashMap<String, Int>()

    constructor(vararg list: String) : this(list.toList())

    constructor(stream: Stream<String>) : this(stream.toList())

    constructor(meta: Meta) : this(*meta.getStringArray("names"))

    /**
     * Checks if this Names contains all the names presented in the input array
     *
     * @param names
     * @return true only if all names a presented in this Names.
     */
    fun contains(vararg names: String): Boolean {
        val list = asList()
        var res = true
        for (name in names) {
            res = res && list.contains(name)
        }
        return res
    }

    /**
     * {@inheritDoc}
     */
    operator fun contains(names: Iterable<String>): Boolean {
        val list = asList()
        var res = true
        for (name in names) {
            res = res && list.contains(name)
        }
        return res
    }

    /**
     * {@inheritDoc}
     */
    fun size(): Int {
        return nameList.size
    }

    /**
     * {@inheritDoc}
     */
    operator fun get(i: Int): String {
        return this.nameList[i]
    }

    /**
     * Finds the number of the given name in list if numbering is supported
     *
     * @param str a [java.lang.String] object.
     * @return a int.
     * @throws hep.dataforge.exceptions.NameNotFoundException if any.
     */
    fun getNumberByName(str: String): Int {
        return indexCache.computeIfAbsent(str) { nameList.indexOf(it) }
    }

    /**
     * {@inheritDoc}
     */
    override fun iterator(): Iterator<String> {
        return this.nameList.iterator()
    }

    /**
     * {@inheritDoc}
     */
    fun asArray(): Array<String> {
        return nameList.toTypedArray()
    }

    /**
     * {@inheritDoc}
     */
    fun asList(): List<String> {
        return Collections.unmodifiableList(this.nameList)
    }

    fun stream(): Stream<String> {
        return this.nameList.stream()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return nameList == (other as? NameList)?.nameList
    }

    override fun hashCode(): Int {
        return Objects.hash(nameList)
    }

    override fun toString(): String {
        return nameList.toString()
    }

    /**
     * Create new Names containing all the names in this, but for the strings in argument. The order of names is preserved
     *
     * @param minusNames
     * @return
     */
    fun minus(vararg minusNames: String): NameList {
        val newNames = ArrayList(asList())
        newNames.removeAll(Arrays.asList(*minusNames))
        return NameList(newNames)
    }

    /**
     * Create new Names with additional names preserving order.
     *
     * @param plusNames
     * @return
     */
    internal fun plus(vararg plusNames: String): NameList {
        val newNames = LinkedHashSet(asList())
        newNames.addAll(Arrays.asList(*plusNames))
        return NameList(newNames)
    }

    override fun toMeta(): Meta {
        return MetaBuilder("names").putValue("names", this.asList())
    }
}
