package hep.dataforge.workspace

import hep.dataforge.context.Context
import hep.dataforge.context.Global
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchKey
import java.security.MessageDigest

/**
 * Dynamic workspace that is parsed from file using external algorithm. Workspace is reloaded only if file is changed
 */
class FileBasedWorkspace(
    private val path: Path,
    private val parser: (Path) -> Workspace
) : DynamicWorkspace(), AutoCloseable {

    private var watchJob: Job? = null

    private val fileMonitor: WatchKey by lazy {
        val service = path.fileSystem.newWatchService()
        path.parent.register(service, StandardWatchEventKinds.ENTRY_MODIFY)
    }

    override fun buildWorkspace(): Workspace {
        if (watchJob == null) {
            watchJob = GlobalScope.launch {
                while (true) {
                    fileMonitor.pollEvents().forEach {
                        if (it.context() == path) {
                            logger.info("Workspace configuration changed. Invalidating.")
                            invalidate()
                        }
                    }
                    fileMonitor.reset()
                }
            }
        }
        return parser(path)
    }


    private fun getCheckSum(): ByteArray {
        try {
            val md = MessageDigest.getInstance("MD5")
            md.update(Files.readAllBytes(path))
            return md.digest()
        } catch (ex: Exception) {
            throw RuntimeException("Failed to generate file checksum", ex)
        }

    }

    override fun close() {
        fileMonitor.cancel()
        watchJob?.cancel()
    }

    companion object {

        /**
         * Find appropriate parser and builder a workspace
         *
         * @param context        a parent context for workspace. Workspace usually creates its own context.
         * @param path           path of the file to create workspace from
         * @param transformation a finalization transformation applied to workspace after loading
         * @return
         */
        @JvmOverloads
        @JvmStatic
        fun build(
            context: Context,
            path: Path,
            transformation: (Workspace.Builder) -> Workspace = { it.build() }
        ): FileBasedWorkspace {
            val fileName = path.fileName.toString()
            return context.serviceStream(WorkspaceParser::class.java)
                .filter { parser -> parser.listExtensions().stream().anyMatch { fileName.endsWith(it) } }
                .findFirst()
                .map { parser ->
                    FileBasedWorkspace(path) { p -> transformation(parser.parse(context, p)) }
                }
                .orElseThrow { RuntimeException("Workspace parser for $path not found") }
        }

        fun build(path: Path): Workspace {
            return build(Global, path) { it.build() }
        }
    }
}
