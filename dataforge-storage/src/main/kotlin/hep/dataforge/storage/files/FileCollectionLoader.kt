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

package hep.dataforge.storage.files

import hep.dataforge.connections.ConnectionHelper
import hep.dataforge.context.Context
import hep.dataforge.data.binary.Binary
import hep.dataforge.io.envelopes.Envelope
import hep.dataforge.io.envelopes.EnvelopeReader
import hep.dataforge.meta.Meta
import hep.dataforge.storage.Loader
import hep.dataforge.storage.StorageElement
import java.nio.file.Path
import kotlin.reflect.KClass

/**
 * An abastract loader that reads the file an an envelope and then interprets the data as a sequence as binary objects with fixed offset
 */
abstract class FileCollectionLoader<T : Any>(
        override val context: Context,
        override val parent: StorageElement? = null,
        final override val name: String,
        final override val type: KClass<T>,
        final override val path: Path
) : Loader<T>, FileStorageElement {
    private val _connectionHelper = ConnectionHelper(this)

    private var envelope: Envelope? = null
        get() {
            if (field == null) {
                field = EnvelopeReader.readFile(path)
            }
            return field
        }

    override val meta: Meta
        get() = envelope?.meta ?: error("Loader is closed")

    override fun getConnectionHelper(): ConnectionHelper = _connectionHelper

    val data: Binary
        get() = envelope?.data ?: error("Loader is closed")

    /**
     * Sequence of <index, offset, value>
     * @param startIndex from which one needs to read entries
     */
    protected abstract fun readAll(startIndex: Int = 0): Sequence<Entry>

    override fun iterator(): Iterator<T> = readAll().map { it.value }.iterator()

    fun forEachIndexed(operation: (index: Int, T) -> Unit) {
        readAll().forEach { operation(it.index, it.value) }
    }

    override fun close() {
        envelope = null
    }

    protected inner class Entry(val index: Int, val offset: Long, val value: T)
}