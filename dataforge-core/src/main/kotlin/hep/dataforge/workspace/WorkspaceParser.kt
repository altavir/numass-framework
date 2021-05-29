package hep.dataforge.workspace

import hep.dataforge.context.Context

import java.io.IOException
import java.io.Reader
import java.nio.file.Files
import java.nio.file.Path

/**
 * A parser for a workspace
 */
interface WorkspaceParser {
    /**
     * List all extensions managed by this parser
     *
     * @return
     */
    fun listExtensions(): List<String>

    /**
     * Parse a file as a workspace
     *
     * @param path
     * @return
     */
    fun parse(parentContext: Context, path: Path): Workspace.Builder {
        try {
            return parse(parentContext, Files.newBufferedReader(path))
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

    }

    fun parse(parentContext: Context, reader: Reader): Workspace.Builder
}
