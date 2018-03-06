package inr.numass.data.storage

import com.github.robtimus.filesystems.sftp.SFTPEnvironment
import hep.dataforge.context.Context
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder
import hep.dataforge.storage.api.Storage
import hep.dataforge.storage.api.StorageType
import hep.dataforge.storage.commons.StorageManager
import hep.dataforge.storage.filestorage.FileStorage
import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Created by darksnake on 17-May-17.
 */
class NumassStorageFactory : StorageType {

    override fun type(): String {
        return "numass"
    }

    override fun build(context: Context, meta: Meta): Storage {
        if (meta.hasValue("path")) {
            val uri = URI.create(meta.getString("path"))
            val path: Path
            if (uri.scheme.startsWith("ssh")) {
                try {
                    val username = meta.getString("userName", uri.userInfo)
                    //String host = meta.getString("host", uri.getHost());
                    val port = meta.getInt("port", 22)
                    val env = SFTPEnvironment()
                            .withUsername(username)
                            .withPassword(meta.getString("password", "").toCharArray())
                    val fs = FileSystems.newFileSystem(uri, env, context.classLoader)
                    path = fs.getPath(uri.path)
                } catch (e: Exception) {
                    throw RuntimeException(e)
                }

            } else {
                path = Paths.get(uri)
            }
            return NumassStorage(context, meta, path)
        } else {
            context.logger.warn("A storage path not provided. Creating default root storage in the working directory")
            return NumassStorage(context, meta, context.io.workDir)
        }
    }

    companion object {

        /**
         * Build local storage with Global context. Used for tests.
         *
         * @param file
         * @return
         */
        fun buildLocal(context: Context, file: Path, readOnly: Boolean, monitor: Boolean): FileStorage {
            val manager = context.load(StorageManager::class.java, Meta.empty())
            return manager.buildStorage(buildStorageMeta(file.toUri(), readOnly, monitor)) as FileStorage
        }

        fun buildLocal(context: Context, path: String, readOnly: Boolean, monitor: Boolean): FileStorage {
            val file = context.io.dataDir.resolve(path)
            return buildLocal(context, file, readOnly, monitor)
        }

        fun buildStorageMeta(path: URI, readOnly: Boolean, monitor: Boolean): MetaBuilder {
            return MetaBuilder("storage")
                    .setValue("path", path.toString())
                    .setValue("type", "numass")
                    .setValue("readOnly", readOnly)
                    .setValue("monitor", monitor)
        }
    }
}
