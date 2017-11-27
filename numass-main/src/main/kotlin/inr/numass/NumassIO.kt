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
package inr.numass

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.Appender
import ch.qos.logback.core.FileAppender
import hep.dataforge.context.Context
import hep.dataforge.io.BasicIOManager
import hep.dataforge.io.IOManager
import hep.dataforge.names.Name
import hep.dataforge.utils.ReferenceRegistry
import org.apache.commons.io.output.TeeOutputStream
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

/**
 * @author Darksnake
 */
class NumassIO : BasicIOManager() {

    internal var registry = ReferenceRegistry<OutputStream>()
    //    FileAppender<ILoggingEvent> appender;


    override fun attach(context: Context) {
        super.attach(context)
    }

    override fun createLoggerAppender(): Appender<ILoggingEvent> {
        val lc = LoggerFactory.getILoggerFactory() as LoggerContext
        val ple = PatternLayoutEncoder()

        ple.pattern = "%date %level [%thread] %logger{10} [%file:%line] %msg%n"
        ple.context = lc
        ple.start()
        val appender = FileAppender<ILoggingEvent>()
        appender.file = File(workDirectory.toFile(), meta.getString("logFileName", "numass.log")).toString()
        appender.encoder = ple
        return appender
    }

    override fun detach() {
        super.detach()
        registry.forEach { it ->
            try {
                it.close()
            } catch (e: IOException) {
                LoggerFactory.getLogger(javaClass).error("Failed to close output", e)
            }
        }
    }

    private fun getExtension(type: String): String {
        return when (type) {
            IOManager.DEFAULT_OUTPUT_TYPE -> ".out"
            else -> "." + type
        }
    }

    override fun out(stage: Name?, name: Name, type: String): OutputStream {
        val tokens = ArrayList<String>()
        if (context.hasValue("numass.path")) {
            val path = context.getString("numass.path")
            if (path.contains(".")) {
                tokens.addAll(Arrays.asList(*path.split(".".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()))
            } else {
                tokens.add(path)
            }
        }

        if (stage != null && stage.length != 0) {
            tokens.addAll(Arrays.asList(*stage.asArray()))
        }

        val dirName = tokens.joinToString(File.separator)
        val fileName = name.toString() + getExtension(type)
        val out = buildOut(workDirectory, dirName, fileName)
        registry.add(out)
        return out
    }

    private fun buildOut(parentDir: Path, dirName: String?, fileName: String): OutputStream {
        val outputFile: Path

        if (!Files.exists(parentDir)) {
            throw RuntimeException("Working directory does not exist")
        }
        try {
            val dir = if (dirName.isNullOrEmpty()) {
                parentDir
            } else {
                parentDir.resolve(dirName).also {
                    Files.createDirectories(it)
                }
            }

            //        String output = source.meta().getString("output", this.meta().getString("output", fileName + ".onComplete"));
            outputFile = dir.resolve(fileName)

            return if (context.getBoolean("numass.consoleOutput", false)!!) {
                TeeOutputStream(Files.newOutputStream(outputFile), System.out)
            } else {
                Files.newOutputStream(outputFile)
            }
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        }

    }

    companion object {

        val NUMASS_OUTPUT_CONTEXT_KEY = "numass.outputDir"
    }
}
