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

package hep.dataforge.storage.tables

import hep.dataforge.Type
import hep.dataforge.context.async
import hep.dataforge.io.envelopes.Envelope
import hep.dataforge.meta.Meta
import hep.dataforge.meta.buildMeta
import hep.dataforge.storage.AppendableLoader
import hep.dataforge.storage.IndexedLoader
import hep.dataforge.storage.Loader
import hep.dataforge.storage.MutableStorage
import hep.dataforge.storage.files.TableLoaderType
import hep.dataforge.tables.ListTable
import hep.dataforge.tables.Table
import hep.dataforge.tables.TableFormat
import hep.dataforge.tables.ValuesSource
import hep.dataforge.values.Value
import hep.dataforge.values.Values
import kotlinx.coroutines.Deferred

@Type("hep.dataforge.storage.loader.table")
interface TableLoader : Loader<Values>, ValuesSource {
    /**
     * Format of the table
     */
    val format: TableFormat

    /**
     * Generate indexed loader based on this one. Type of the indesx is defined by meta
     */
    fun indexed(meta: Meta = Meta.empty()): IndexedTableLoader

    /**
     * Generate a mutable loader based on this one. Throws an exception if it is not possible
     */
    fun mutable(): MutableTableLoader
}

/**
 * TODO Replace with custom table which will be updated with the loader
 */
fun TableLoader.asTable(): Deferred<Table> {
    return async { ListTable(format, toList()) }
}

interface IndexedTableLoader : TableLoader, IndexedLoader<Value, Values> {
    suspend fun get(any: Any): Values? = get(Value.of(any))

    /**
     * Notify loader that it should update index for this loader
     */
    fun updateIndex()
}

/**
 * Select a range from this table loade
 */
suspend fun IndexedTableLoader.select(from: Value, to: Value): Table {
    return ListTable(format, keys.subSet(from, true, to, true).map { get(it)!! })
}

fun IndexedTableLoader.select(query: Meta): Deferred<Table> {
    TODO("To be implemented")
}


interface MutableTableLoader : TableLoader, AppendableLoader<Values>

/**
 * Create a new table loader with given name and format
 */
fun MutableStorage.createTable(name: String, format: TableFormat): MutableTableLoader {
    val meta = buildMeta {
        "name" to name
        TableLoaderType.TABLE_FORMAT_KEY to format.toMeta()
        Envelope.ENVELOPE_DATA_TYPE_KEY to TableLoaderType.BINARY_DATA_TYPE
    }
    return create(meta) as MutableTableLoader
}