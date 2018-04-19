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
package inr.numass.data.storage

import hep.dataforge.exceptions.StorageException
import hep.dataforge.io.ColumnedDataReader
import hep.dataforge.io.envelopes.Envelope
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder
import hep.dataforge.providers.Provider
import hep.dataforge.storage.api.ObjectLoader
import hep.dataforge.storage.api.Storage
import hep.dataforge.storage.filestorage.FileStorage
import hep.dataforge.storage.loaders.AbstractLoader
import hep.dataforge.tables.Table
import inr.numass.data.api.NumassPoint
import inr.numass.data.api.NumassSet
import inr.numass.data.legacy.NumassFileEnvelope
import org.slf4j.LoggerFactory

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.*
import java.util.function.Supplier
import java.util.stream.Stream


/**
 * The reader for numass main detector data directory or zip format;
 *
 * @author darksnake
 */
class NumassDataLoader(
        storage: Storage,
        name: String,
        meta: Meta,
        private val items: Map<String, Supplier<out Envelope>>,
        override var isReadOnly: Boolean = true
) : AbstractLoader(storage, name, meta), ObjectLoader<Envelope>, NumassSet, Provider {

    override val meta: Meta = items[META_FRAGMENT_NAME]?.get()?.meta ?: Meta.empty()

    private val hvEnvelope: Optional<Envelope>
        get() = Optional.ofNullable(items[HV_FRAGMENT_NAME]).map { it.get() }

    private val pointEnvelopes: Stream<Envelope>
        get() = items.entries.stream()
                .filter { entry -> entry.key.startsWith(POINT_FRAGMENT_NAME) }
                .map { entry -> entry.value.get() }
                .sorted(Comparator.comparing<Envelope, Int> { t -> t.meta.getInt("external_meta.point_index", -1) })

    val isReversed: Boolean
        get() = this.meta.getBoolean("iteration_info.reverse", false)

    override val isEmpty: Boolean
        get() = items.isEmpty()

    override val description: String = this.meta.getString("description", "").replace("\\n", "\n")

    override fun fragmentNames(): Collection<String> {
        return items.keys
    }

    override val hvData: Optional<Table>
        get() = hvEnvelope.map { hvEnvelope ->
            try {
                ColumnedDataReader(hvEnvelope.data.stream, "timestamp", "block", "value").toTable()
            } catch (ex: IOException) {
                LoggerFactory.getLogger(javaClass).error("Failed to load HV data from file", ex)
                null
            }
        }


    override val points: Stream<NumassPoint>
        get() {
            return pointEnvelopes.map {
                NumassPoint.read(it)
            }
        }

    override fun pull(fragmentName: String): Envelope {
        //PENDING read data to memory?
        return items[fragmentName]?.get()
                ?: throw StorageException("The fragment with name $fragmentName is not found in the loader $name")
    }

    @Throws(StorageException::class)
    override fun push(fragmentName: String, data: Envelope) {
        tryPush()
        TODO()
    }

    override fun respond(message: Envelope): Envelope {
        throw TODO("Not supported yet.")
    }

    override val startTime: Instant
        get() = meta.optValue("start_time").map<Instant> { it.getTime() }.orElseGet { super.startTime }


    override val isOpen: Boolean
        get() = true

    override fun close() {
        //do nothing
    }


    companion object {


        @Throws(IOException::class)
        fun fromFile(storage: Storage, zipFile: Path): NumassDataLoader {
            throw UnsupportedOperationException("TODO")
        }


        /**
         * Construct numass loader from directory
         *
         * @param storage
         * @param directory
         * @return
         * @throws IOException
         */
        @Throws(IOException::class)
        fun fromDir(storage: Storage, directory: Path, name: String = FileStorage.entryName(directory)): NumassDataLoader {
            if (!Files.isDirectory(directory)) {
                throw IllegalArgumentException("Numass data directory required")
            }
            val annotation = MetaBuilder("loader")
                    .putValue("type", "numass")
                    .putValue("numass.loaderFormat", "dir")
                    //                .setValue("file.timeCreated", Instant.ofEpochMilli(directory.getContent().getLastModifiedTime()))
                    .build()

            //FIXME envelopes are lazy do we need to do additional lazy evaluations here?
            val items = LinkedHashMap<String, Supplier<out Envelope>>()

            Files.list(directory).filter { file ->
                val fileName = file.fileName.toString()
                (fileName == META_FRAGMENT_NAME
                        || fileName == HV_FRAGMENT_NAME
                        || fileName.startsWith(POINT_FRAGMENT_NAME))
            }.forEach { file ->
                try {
                    items[FileStorage.entryName(file)] = Supplier { NumassFileEnvelope.open(file, true) }
                } catch (ex: Exception) {
                    LoggerFactory.getLogger(NumassDataLoader::class.java)
                            .error("Can't load numass data directory " + FileStorage.entryName(directory), ex)
                }
            }

            return NumassDataLoader(storage, name, annotation, items)
        }

        /**
         * "start_time": "2016-04-20T04:08:50",
         *
         * @param meta
         * @return
         */
        private fun readTime(meta: Meta): Instant {
            return if (meta.hasValue("start_time")) {
                meta.getValue("start_time").getTime()
            } else {
                Instant.EPOCH
            }
        }

        /**
         * The name of informational meta file in numass data directory
         */
        val META_FRAGMENT_NAME = "meta"

        /**
         * The beginning of point fragment name
         */
        val POINT_FRAGMENT_NAME = "p"

        /**
         * The beginning of hv fragment name
         */
        val HV_FRAGMENT_NAME = "voltage"
    }

}
