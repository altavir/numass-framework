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

import hep.dataforge.connections.ConnectionHelper
import hep.dataforge.context.Context
import hep.dataforge.io.ColumnedDataReader
import hep.dataforge.io.envelopes.Envelope
import hep.dataforge.meta.Meta
import hep.dataforge.providers.Provider
import hep.dataforge.storage.Loader
import hep.dataforge.storage.StorageElement
import hep.dataforge.storage.files.FileStorageElement
import hep.dataforge.tables.Table
import inr.numass.data.NumassDataUtils
import inr.numass.data.NumassEnvelopeType
import inr.numass.data.api.NumassPoint
import inr.numass.data.api.NumassSet
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlin.reflect.KClass
import kotlin.streams.toList


/**
 * The reader for numass main detector data directory or zip format;
 *
 * @author darksnake
 */
class NumassDataLoader(
    override val context: Context,
    override val parent: StorageElement?,
    override val name: String,
    override val path: Path
) : Loader<NumassPoint>, NumassSet, Provider, FileStorageElement {

    override val type: KClass<NumassPoint> = NumassPoint::class

    private val _connectionHelper = ConnectionHelper(this)

    override fun getConnectionHelper(): ConnectionHelper = _connectionHelper


    override val meta: Meta by lazy {
        val metaPath = path.resolve("meta")
        NumassEnvelopeType.infer(metaPath)?.reader?.read(metaPath)?.meta ?: Meta.empty()
    }

    override suspend fun getHvData(): Table? {
        val hvEnvelope = path.resolve(HV_FRAGMENT_NAME).let {
            NumassEnvelopeType.infer(it)?.reader?.read(it) ?: error("Can't read hv file")
        }
        return try {
            ColumnedDataReader(hvEnvelope.data.stream, "timestamp", "block", "value").toTable()
        } catch (ex: IOException) {
            LoggerFactory.getLogger(javaClass).error("Failed to load HV data from file", ex)
            null
        }
    }


    private val pointEnvelopes: List<Envelope> by lazy {
        Files.list(path)
            .filter { it.fileName.toString().startsWith(POINT_FRAGMENT_NAME) }
            .map {
                NumassEnvelopeType.infer(it)?.reader?.read(it) ?: error("Can't read point file")
            }.toList()
    }

    val isReversed: Boolean
        get() = this.meta.getBoolean("iteration_info.reverse", false)

    val description: String
        get() = this.meta.getString("description", "").replace("\\n", "\n")


    override val points: List<NumassPoint>
        get() = pointEnvelopes.map {
            NumassDataUtils.read(it)
        }


    override val startTime: Instant
        get() = meta.optValue("start_time").map<Instant> { it.time }.orElseGet { super.startTime }

    override fun close() {
        //do nothing
    }


    companion object {
//
//        @Throws(IOException::class)
//        fun fromFile(storage: Storage, zipFile: Path): NumassDataLoader {
//            throw UnsupportedOperationException("TODO")
//        }
//
//
//        /**
//         * Construct numass loader from directory
//         *
//         * @param storage
//         * @param directory
//         * @return
//         * @throws IOException
//         */
//        @Throws(IOException::class)
//        fun fromDir(storage: Storage, directory: Path, name: String = FileStorage.entryName(directory)): NumassDataLoader {
//            if (!Files.isDirectory(directory)) {
//                throw IllegalArgumentException("Numass data directory required")
//            }
//            val annotation = MetaBuilder("loader")
//                    .putValue("type", "numass")
//                    .putValue("numass.loaderFormat", "dir")
//                    //                .setValue("file.timeCreated", Instant.ofEpochMilli(directory.getContent().getLastModifiedTime()))
//                    .build()
//
//            //FIXME envelopes are lazy do we need to do additional lazy evaluations here?
//            val items = LinkedHashMap<String, Supplier<out Envelope>>()
//
//            Files.list(directory).filter { file ->
//                val fileName = file.fileName.toString()
//                (fileName == META_FRAGMENT_NAME
//                        || fileName == HV_FRAGMENT_NAME
//                        || fileName.startsWith(POINT_FRAGMENT_NAME))
//            }.forEach { file ->
//                try {
//                    items[FileStorage.entryName(file)] = Supplier { NumassFileEnvelope.open(file, true) }
//                } catch (ex: Exception) {
//                    LoggerFactory.getLogger(NumassDataLoader::class.java)
//                            .error("Can't load numass data directory " + FileStorage.entryName(directory), ex)
//                }
//            }
//
//            return NumassDataLoader(storage, name, annotation, items)
//        }
//
//        fun fromDir(context: Context, directory: Path, name: String = FileStorage.entryName(directory)): NumassDataLoader {
//            return fromDir(DummyStorage(context), directory, name)
//        }
//
//        /**
//         * "start_time": "2016-04-20T04:08:50",
//         *
//         * @param meta
//         * @return
//         */
//        private fun readTime(meta: Meta): Instant {
//            return if (meta.hasValue("start_time")) {
//                meta.getValue("start_time").time
//            } else {
//                Instant.EPOCH
//            }
//        }

        /**
         * The name of informational meta file in numass data directory
         */
        const val META_FRAGMENT_NAME = "meta"

        /**
         * The beginning of point fragment name
         */
        const val POINT_FRAGMENT_NAME = "p"

        /**
         * The beginning of hv fragment name
         */
        const val HV_FRAGMENT_NAME = "voltage"
    }
}


fun Context.readNumassSet(path:Path):NumassDataLoader{
    return NumassDataLoader(this,null,path.fileName.toString(),path)
}


