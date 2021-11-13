/* 
 * Copyright 2015 Alexander Nozik.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package hep.dataforge.tables

import hep.dataforge.Named
import hep.dataforge.values.Value

import java.io.Serializable
import java.util.stream.Stream
import java.util.stream.StreamSupport

/**
 * Column of values with format meta
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */

interface Column : Named, Iterable<Value>, Serializable {

    val format: ColumnFormat


    override val name: String
        get() = format.name

    /**
     * Get the value with the given index
     * @param n
     * @return
     */
    operator fun get(n: Int): Value

    //TODO add custom value type accessors

    /**
     * Get values as list
     * @return
     */
    fun asList(): List<Value>

    /**
     * The length of the column
     * @return
     */
    fun size(): Int

    /**
     * Get the values as a stream
     * @return
     */

    fun stream(): Stream<Value> {
        return StreamSupport.stream(spliterator(), false)
    }
}
