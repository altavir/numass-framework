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

import hep.dataforge.context.Context
import hep.dataforge.context.Global
import hep.dataforge.events.Event
import hep.dataforge.events.EventBuilder
import hep.dataforge.meta.Meta
import hep.dataforge.storage.StorageElement
import hep.dataforge.storage.files.FileStorage
import hep.dataforge.storage.files.FileStorageElement
import inr.numass.NumassEnvelopeType
import java.nio.file.Files
import java.nio.file.Path

/**
 * Numass storage directory. Works as a normal directory, but creates a numass loader from each directory with meta
 */
class NumassDirectory : FileStorage.Directory() {
    override val name: String = NUMASS_DIRECTORY_TYPE

    override suspend fun read(context: Context, path: Path, parent: StorageElement?): FileStorageElement? {
        val meta = FileStorage.resolveMeta(path){ NumassEnvelopeType.infer(it)?.reader?.read(it)?.meta }
        return if (Files.isDirectory(path) && meta != null) {
            NumassDataLoader(context, parent, path.fileName.toString(), path)
        } else {
            super.read(context, path, parent)
        }
    }

    companion object {
        val INSTANCE = NumassDirectory()
        const val NUMASS_DIRECTORY_TYPE = "inr.numass.storage.directory"

        /**
         * Simple read for scripting and debug
         */
        fun read(context: Context = Global, path: String): FileStorageElement?{
            return runBlocking { INSTANCE.read(context, context.getDataFile(path).absolutePath)}
        }
    }
}

class NumassDataPointEvent(meta: Meta) : Event(meta) {

    val fileSize: Int = meta.getInt(FILE_SIZE_KEY, 0)

    val fileName: String = meta.getString(FILE_NAME_KEY)

    override fun toString(): String {
        return String.format("(%s) [%s] : pushed numass data file with name '%s' and size '%d'",
                time().toString(), sourceTag(), fileName, fileSize)
    }

    companion object {


        const val FILE_NAME_KEY = "fileName"
        const val FILE_SIZE_KEY = "fileSize"

        fun build(source: String, fileName: String, fileSize: Int): NumassDataPointEvent {
            return NumassDataPointEvent(builder(source, fileName, fileSize).buildEventMeta())
        }

        fun builder(source: String, fileName: String, fileSize: Int): EventBuilder<*> {
            return EventBuilder.make("numass.storage.pushData")
                    .setSource(source)
                    .setMetaValue(FILE_NAME_KEY, fileName)
                    .setMetaValue(FILE_SIZE_KEY, fileSize)
        }
    }

}

//
///**
// * The file storage containing numass data directories or zips.
// *
// *
// * Any subdirectory is treated as numass data directory. Any zip must have
// * `NUMASS_ZIP_EXTENSION` extension to be recognized. Any other files are
// * ignored.
// *
// *
// * @author Alexander Nozik
// */
//class NumassStorage : FileStorage {
//
//    val description: String
//        get() = meta.getString("description", "")
//
//    private constructor(parent: FileStorage, config: Meta, shelf: String) : super(parent, config, shelf)
//
//    constructor(context: Context, config: Meta, path: Path) : super(context, config, path)
//
//    init {
//        refresh()
//    }
//
//    override fun refresh() {
//        try {
//            this.shelves.clear()
//            this.loaders.clear()
//            Files.list(dataDir).forEach { file ->
//                try {
//                    if (Files.isDirectory(file)) {
//                        val metaFile = file.resolve(NumassDataLoader.META_FRAGMENT_NAME)
//                        if (Files.exists(metaFile)) {
//                            this.loaders[entryName(file)] = NumassDataLoader.fromDir(this, file)
//                        } else {
//                            this.shelves[entryName(file)] = NumassStorage(this, meta, entryName(file))
//                        }
//                    } else if (file.fileName.endsWith(NUMASS_ZIP_EXTENSION)) {
//                        this.loaders[entryName(file)] = NumassDataLoader.fromFile(this, file)
//                    } else {
//                        //updating non-numass loader files
//                        updateFile(file)
//                    }
//                } catch (ex: IOException) {
//                    LoggerFactory.getLogger(javaClass).error("Error while creating numass loader", ex)
//                } catch (ex: StorageException) {
//                    LoggerFactory.getLogger(javaClass).error("Error while creating numass group", ex)
//                }
//            }
//        } catch (ex: IOException) {
//            throw RuntimeException(ex)
//        }
//
//    }
//
//    @Throws(StorageException::class)
//    fun pushNumassData(path: String?, fileName: String, data: ByteBuffer) {
//        if (path == null || path.isEmpty()) {
//            pushNumassData(fileName, data)
//        } else {
//            val st = buildShelf(path) as NumassStorage
//            st.pushNumassData(fileName, data)
//        }
//    }
//
//    /**
//     * Read nm.zip content and write it as a new nm.zip file
//     *
//     * @param fileName
//     */
//    @Throws(StorageException::class)
//    fun pushNumassData(fileName: String, data: ByteBuffer) {
//        //FIXME move zip to internal
//        try {
//            val nmFile = dataDir.resolve(fileName + NUMASS_ZIP_EXTENSION)
//            if (Files.exists(nmFile)) {
//                LoggerFactory.getLogger(javaClass).warn("Trying to rewrite existing numass data file {}", nmFile.toString())
//            }
//            Files.newByteChannel(nmFile, CREATE, WRITE).use { channel -> channel.write(data) }
//
//            dispatchEvent(NumassDataPointEvent.build(name, fileName, Files.size(nmFile).toInt()))
//        } catch (ex: IOException) {
//            throw StorageException(ex)
//        }
//
//    }
//
//    @Throws(StorageException::class)
//    override fun createShelf(shelfConfiguration: Meta, shelfName: String): NumassStorage {
//        return NumassStorage(this, shelfConfiguration, shelfName)
//    }
//
//    /**
//     * A list of legacy DAT files in the directory
//     *
//     * @return
//     */
//    fun legacyFiles(): List<NumassSet> {
//        try {
//            val files = ArrayList<NumassSet>()
//            Files.list(dataDir).forEach { file ->
//                if (Files.isRegularFile(file) && file.fileName.toString().toLowerCase().endsWith(".dat")) {
//                    //val name = file.fileName.toString()
//                    try {
//                        files.add(NumassDatFile(file, Meta.empty()))
//                    } catch (ex: Exception) {
//                        LoggerFactory.getLogger(javaClass).error("Error while reading legacy numass file " + file.fileName, ex)
//                    }
//
//                }
//            }
//            return files
//        } catch (ex: IOException) {
//            throw RuntimeException(ex)
//        }
//
//    }
//
//    @Throws(Exception::class)
//    override fun close() {
//        super.close()
//        //close remote file system after use
//        try {
//            dataDir.fileSystem.close()
//        } catch (ex: UnsupportedOperationException) {
//
//        }
//
//    }
//

//
//    companion object {
//
//        const val NUMASS_ZIP_EXTENSION = ".nm.zip"
//        const val NUMASS_DATA_LOADER_TYPE = "numassData"
//    }
//
//}

//class NumassStorageFactory : StorageType {
//
//    override fun type(): String {
//        return "numass"
//    }
//
//    override fun build(context: Context, meta: Meta): Storage {
//        if (meta.hasValue("path")) {
//            val uri = URI.create(meta.getString("path"))
//            val path: Path
//            if (uri.scheme.startsWith("ssh")) {
//                try {
//                    val username = meta.getString("userName", uri.userInfo)
//                    //String host = meta.getString("host", uri.getHost());
//                    val port = meta.getInt("port", 22)
//                    val env = SFTPEnvironment()
//                            .withUsername(username)
//                            .withPassword(meta.getString("password", "").toCharArray())
//                    val fs = FileSystems.newFileSystem(uri, env, context.classLoader)
//                    path = fs.getPath(uri.path)
//                } catch (e: Exception) {
//                    throw RuntimeException(e)
//                }
//
//            } else {
//                path = Paths.get(uri)
//            }
//            if(!Files.exists(path)){
//                context.logger.info("File $path does not exist. Creating a new storage directory.")
//                Files.createDirectories(path)
//            }
//            return NumassStorage(context, meta, path)
//        } else {
//            context.logger.warn("A storage path not provided. Creating default root storage in the working directory")
//            return NumassStorage(context, meta, context.workDir)
//        }
//    }
//
//    companion object {
//
//        /**
//         * Build local storage with Global context. Used for tests.
//         *
//         * @param file
//         * @return
//         */
//        fun buildLocal(context: Context, file: Path, readOnly: Boolean, monitor: Boolean): FileStorage {
//            val manager = context.load(StorageManager::class.java, Meta.empty())
//            return manager.buildStorage(buildStorageMeta(file.toUri(), readOnly, monitor)) as FileStorage
//        }
//
//        fun buildLocal(context: Context, path: String, readOnly: Boolean, monitor: Boolean): FileStorage {
//            val file = context.dataDir.resolve(path)
//            return buildLocal(context, file, readOnly, monitor)
//        }
//
//        fun buildStorageMeta(path: URI, readOnly: Boolean, monitor: Boolean): MetaBuilder {
//            return MetaBuilder("storage")
//                    .setValue("path", path.toString())
//                    .setValue("type", "numass")
//                    .setValue("readOnly", readOnly)
//                    .setValue("monitor", monitor)
//        }
//    }
//}
