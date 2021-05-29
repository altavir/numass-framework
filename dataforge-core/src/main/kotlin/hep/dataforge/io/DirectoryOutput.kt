package hep.dataforge.io

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.Appender
import ch.qos.logback.core.FileAppender
import hep.dataforge.context.FileReference
import hep.dataforge.context.Plugin
import hep.dataforge.context.PluginDef
import hep.dataforge.context.PluginFactory
import hep.dataforge.io.OutputManager.Companion.OUTPUT_NAME_KEY
import hep.dataforge.io.OutputManager.Companion.OUTPUT_STAGE_KEY
import hep.dataforge.io.OutputManager.Companion.OUTPUT_TYPE_KEY
import hep.dataforge.io.output.FileOutput
import hep.dataforge.io.output.Output
import hep.dataforge.meta.Meta
import hep.dataforge.names.Name
import org.slf4j.LoggerFactory
import java.io.File

/**
 * A directory based IO manager. Any named output is redirected to file in corresponding directory inside work directory
 */
@PluginDef(name = "output.dir", group = "hep.dataforge", info = "Directory based output plugin")
class DirectoryOutput : AbstractOutputManager() {

    //internal var registry = ReferenceRegistry<OutputStream>()
    //    FileAppender<ILoggingEvent> appender;

    private val map = HashMap<Meta, FileOutput>()


    override fun createLoggerAppender(): Appender<ILoggingEvent> {
        val lc = LoggerFactory.getILoggerFactory() as LoggerContext
        val ple = PatternLayoutEncoder()

        ple.pattern = "%date %level [%thread] %logger{10} [%file:%line] %msg%n"
        ple.context = lc
        ple.start()
        val appender = FileAppender<ILoggingEvent>()
        appender.file = File(context.workDir.toFile(), meta.getString("logFileName", "${context.name}.log")).toString()
        appender.encoder = ple
        return appender
    }

    override fun detach() {
        super.detach()
        map.values.forEach {
            //TODO add catch
            it.close()
        }
    }

    /**
     * Get file extension for given content type
     */
    private fun getExtension(type: String): String {
        return when (type) {
            Output.BINARY_TYPE -> "df"
            else -> "out"
        }
    }

    override fun get(meta: Meta): Output {
        val name = meta.getString(OUTPUT_NAME_KEY)
        val stage = Name.of(meta.getString(OUTPUT_STAGE_KEY, ""))
        val extension = meta.optString("file.extension").orElseGet { getExtension(meta.getString(OUTPUT_TYPE_KEY, Output.TEXT_TYPE)) }
        val reference = FileReference.newWorkFile(context, name, extension, stage)
        return FileOutput(reference)
    }

    class Factory : PluginFactory() {
        override val type: Class<out Plugin> = DirectoryOutput::class.java

        override fun build(meta: Meta): Plugin {
            return DirectoryOutput()
        }
    }

}