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

import hep.dataforge.context.Context
import hep.dataforge.io.envelopes.*
import hep.dataforge.meta.Meta
import hep.dataforge.meta.buildMeta
import hep.dataforge.nullable
import hep.dataforge.storage.StorageElement
import hep.dataforge.storage.files.TableLoaderType.Companion.TABLE_FORMAT_KEY
import hep.dataforge.storage.files.TableLoaderType.Companion.binaryTableWriter
import hep.dataforge.storage.tables.IndexedTableLoader
import hep.dataforge.storage.tables.MutableTableLoader
import hep.dataforge.tables.MetaTableFormat
import hep.dataforge.tables.TableFormat
import hep.dataforge.values.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.*


/**
 * @param reader read Values and move buffer position to next entry
 */
open class FileTableLoader internal constructor(
        context: Context,
        parent: StorageElement?,
        name: String,
        path: Path,
        val reader: (ByteBuffer, TableFormat) -> Values
) : FileCollectionLoader<Values>(
        context = context,
        parent = parent,
        name = name,
        type = Values::class,
        path = path
), IndexedTableLoader {

    override val format: TableFormat by lazy {
        when {
            meta.hasMeta(TABLE_FORMAT_KEY) -> MetaTableFormat(meta.getMeta("format"))
            meta.hasValue(TABLE_FORMAT_KEY) -> MetaTableFormat.forNames(*meta.getStringArray("format"))
            else -> throw RuntimeException("Format definition not found")
        }
    }

    protected val defaultIndex by lazy { TreeMap<Value, Long>().apply { fillIndex(this) } }


    protected fun getOffset(index: Int): Long? {
        if (index == 0) {
            return 0
        } else if (index >= defaultIndex.size) {
            updateIndex()
        }
        return defaultIndex[index.asValue()]
    }

    override val keys: NavigableSet<Value>
        get() = defaultIndex.navigableKeySet()

    override suspend fun get(key: Value): Values? {
        return getOffset(key.int)?.let {
            synchronized(data) {
                reader(data.buffer.apply { position(it.toInt()) }, format)
            }
        }
    }


    override fun mutable(): AppendableFileTableLoader {
        return when (reader) {
            binaryTableReader -> AppendableFileTableLoader(this, binaryTableWriter)
            textTableReader -> error("Text output is no longer supported")
            else -> error("Unsupported table serialization format")
        }
    }

    override fun indexed(meta: Meta): IndexedTableLoader {
        return if (meta.isEmpty) {
            this
        } else {
            IndexedFileTableLoader(this, meta.getString("field"))
        }
    }

    private fun fillIndex(index: TreeMap<Value, Long>) {
        val sequence = if (index.isEmpty()) {
            readAll()
        } else {
            readAll(index.lastKey().int)
        }
        sequence.forEach {
            index.putIfAbsent(it.index.asValue(), it.offset)
        }
    }

    override fun updateIndex() {
        fillIndex(defaultIndex)
    }


    override fun readAll(startIndex: Int): Sequence<Entry> {
        val offset = getOffset(startIndex) ?: throw Error("The index value is unavailable")
        synchronized(data) {
            var counter = startIndex
            val buffer = data.buffer
            buffer.position(offset.toInt())
            return sequence {
                while (buffer.remaining() > 0) {
                    yield(Entry(counter, buffer.position().toLong(), reader(buffer, format)))
                    counter++
                }
            }
        }
    }
}

/**
 * File table loader with alternate index
 */
class IndexedFileTableLoader(val loader: FileTableLoader, val indexField: String) : IndexedTableLoader by loader {

    //TODO implement index caching
    private val secondaryIndex: TreeMap<Value, Value> by lazy {
        TreeMap<Value, Value>().apply {
            loader.forEachIndexed { index, values ->
                this[values.getValue(indexField)] = index.asValue()
            }
        }
    }

    override fun updateIndex() {
        loader.forEachIndexed { index, values ->
            secondaryIndex[values.getValue(indexField)] = index.asValue()
        }
    }

    override suspend fun get(key: Value): Values? {
        return secondaryIndex[key]?.let { loader.get(it) }
    }
}

/**
 * Appendable version of FileTableLoader. Build
 */
class AppendableFileTableLoader(
        val loader: FileTableLoader,
        val writer: (Values, TableFormat) -> ByteBuffer = binaryTableWriter
) : IndexedTableLoader by loader, MutableTableLoader {

    private val mutableEnvelope = MutableFileEnvelope.readExisting(loader.path)

    /**
     * Append single point
     */
    override suspend fun append(item: Values) {
        withContext(Dispatchers.IO) {
            mutableEnvelope.append(writer(item, format))
            loader.updateIndex()
        }
    }

    suspend fun append(vararg values: Any) {
        append(ValueMap.of(format.namesAsArray(), *values))
    }

    /**
     * Batch append operation
     */
    fun appendAll(collection: Iterable<Values>) {
        mutableEnvelope.appendAll(collection.map { writer(it, format) })
        loader.updateIndex()
    }

    override fun close() {
        mutableEnvelope.close()
    }
}

private val textTableReader: (ByteBuffer, TableFormat) -> Values = { buffer, format ->
    val line = buildString {
        do {
            val char = buffer.get().toChar()
            append(char)
        } while (char != '\n')
    }
    val values = line.split("\\s+").map { LateParseValue(it) }
    ValueMap(format.names.zip(values).toMap())
}

private val binaryTableReader: (ByteBuffer, TableFormat) -> Values = { buffer, format ->
    ValueMap(format.names.associate { it to buffer.getValue() }.toMap()).also {
        do {
            val char = buffer.get().toChar()
        } while (char != '\n')
    }
}

class TableLoaderType : FileStorageElementType {
    companion object {
        const val TABLE_ENVELOPE_TYPE = "hep.dataforge.storage.table"
        const val BINARY_DATA_TYPE = "binary"
        const val TEXT_DATA_TYPE = "text"

        const val TABLE_FORMAT_KEY = "format"


        val textTableWriter: (Values, TableFormat) -> ByteBuffer = { values, format ->
            val string = format.names.map { values[it] }.joinToString(separator = "\t", postfix = "\n")
            ByteBuffer.wrap(string.toByteArray(Charsets.UTF_8))
        }

        val binaryTableWriter: (Values, TableFormat) -> ByteBuffer = { values, format ->
            val baos = ByteArrayOutputStream(256)
            val stream = DataOutputStream(baos)
            format.names.map { values[it] }.forEach {
                stream.writeValue(it)
            }
            stream.writeByte('\n'.toInt())
            stream.flush()
            ByteBuffer.wrap(baos.toByteArray())
        }

    }

    override val name: String = TABLE_ENVELOPE_TYPE

    override fun create(context: Context, meta: Meta, parent: StorageElement?): FileStorageElement {
        if (!meta.hasMeta(TABLE_FORMAT_KEY)) {
            throw IllegalArgumentException("Values format not found")
        }
        val fileName = meta.getString("name")
        val path: Path = ((parent as? FileStorageElement)?.path ?: context.dataDir).resolve("$fileName.df")
        return create(context, meta, path, parent)

    }

    private fun create(context: Context, meta: Meta, path: Path, parent: StorageElement?): FileTableLoader {
        val type = meta.getString(Envelope.ENVELOPE_DATA_TYPE_KEY, BINARY_DATA_TYPE)

        val envelope = EnvelopeBuilder()
                .setEnvelopeType(TABLE_ENVELOPE_TYPE)
                .meta(meta)
                .build()

        val name = path.fileName.toString()

        return Files.newOutputStream(path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE).use {
            when (type) {
                BINARY_DATA_TYPE -> {
                    DefaultEnvelopeType.INSTANCE.writer.write(it, envelope)
                    FileTableLoader(context, parent, name, path, binaryTableReader)
                }
                TEXT_DATA_TYPE -> {
                    TaglessEnvelopeType.INSTANCE.writer.write(it, envelope)
                    FileTableLoader(context, parent, name, path, textTableReader)
                }
                else -> throw RuntimeException("Unknown data type for table loader")
            }
        }
    }

    /**
     * Create a standalone loader without a storage
     */
    fun create(context: Context, path: Path, meta: Meta): FileTableLoader {
        return create(context, meta, path, null)
    }

    /**
     * Utility method to create binary table lader with given format
     */
    fun create(context: Context, path: Path, format: TableFormat): FileTableLoader {
        return create(context, path, buildMeta {
            TABLE_FORMAT_KEY to format.toMeta()
            Envelope.ENVELOPE_DATA_TYPE_KEY to BINARY_DATA_TYPE
        })
    }

    override suspend fun read(context: Context, path: Path, parent: StorageElement?): FileStorageElement? {
        val envelope = EnvelopeReader.readFile(path)

        val name = envelope.meta.optString("name").nullable ?: path.fileName.toString()

        return when (envelope.dataType) {
            BINARY_DATA_TYPE -> FileTableLoader(context, parent, name, path, binaryTableReader)
            TEXT_DATA_TYPE -> FileTableLoader(context, parent, name, path, textTableReader)
            else -> throw RuntimeException("Unknown data type for table loader")
        }
    }

    /**
     * Read orphaned loader
     */
    fun read(context: Context, path: Path): FileTableLoader {
        val envelope = EnvelopeReader.readFile(path)
        val name = envelope.meta.optString("name").nullable ?: path.fileName.toString()
        return when (envelope.dataType) {
            BINARY_DATA_TYPE -> FileTableLoader(context, null, name, path, binaryTableReader)
            TEXT_DATA_TYPE -> FileTableLoader(context, null, name, path, textTableReader)
            else -> throw RuntimeException("Unknown data type for table loader")
        }
    }
}