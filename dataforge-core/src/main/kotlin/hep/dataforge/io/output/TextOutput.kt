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

package hep.dataforge.io.output

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.encoder.Encoder
import ch.qos.logback.core.encoder.EncoderBase
import hep.dataforge.asMap
import hep.dataforge.context.Context
import hep.dataforge.context.FileReference
import hep.dataforge.description.NodeDescriptor
import hep.dataforge.description.ValueDescriptor
import hep.dataforge.io.ColumnedDataWriter
import hep.dataforge.io.IOUtils
import hep.dataforge.io.envelopes.Envelope
import hep.dataforge.io.envelopes.EnvelopeType
import hep.dataforge.io.envelopes.TaglessEnvelopeType
import hep.dataforge.io.envelopes.buildEnvelope
import hep.dataforge.io.history.Record
import hep.dataforge.meta.Meta
import hep.dataforge.meta.Metoid
import hep.dataforge.tables.Table
import hep.dataforge.useValue
import hep.dataforge.values.ValueType
import org.slf4j.LoggerFactory
import java.awt.Color
import java.io.OutputStream
import java.io.PrintWriter
import java.time.Instant
import java.util.concurrent.Executors


/**
 * A n output that could display plain text with attributes
 */
interface TextOutput : Output {
    fun renderText(text: String, vararg attributes: TextAttribute)

    /**
     * Render new line honoring offsets and bullets
     */
    fun newLine(meta: Meta) {
        renderText("\n")
        render("", meta)
    }

    @JvmDefault
    fun renderText(text: String, color: Color) {
        renderText(text, TextColor(color))
    }
}

/**
 * A display based on OutputStream. The stream must be closed by caller
 */
open class StreamOutput(override val context: Context, val stream: OutputStream) : Output, AutoCloseable, TextOutput {
    private val printer = PrintWriter(stream)
    private val executor = Executors.newSingleThreadExecutor()

    var isOpen: Boolean = true
        protected set

    protected open val logEncoder: Encoder<ILoggingEvent> by lazy {
        PatternLayoutEncoder().apply {
            this.pattern = "%date %level [%thread] %logger{10} [%file:%line] %msg%n"
            this.context = LoggerFactory.getILoggerFactory() as LoggerContext
            start()
        }
    }

    override fun render(obj: Any, meta: Meta) {
        if (!isOpen) {
            error("Can't write to closed output")
        }
        executor.run {
            meta.useValue("text.offset") { repeat(it.int) { renderText("\t") } }
            meta.useValue("text.bullet") { renderText(it.string + " ") }
            when (obj) {
                is Meta -> renderMeta(obj, meta)
                is SelfRendered -> {
                    obj.render(this@StreamOutput, meta)
                }
                is Table -> {
                    //TODO add support for tab-stops
                    renderText(obj.format.names.joinToString(separator = "\t"), TextColor(Color.BLUE))
                    obj.rows.forEach { values ->
                        printer.println(obj.format.names.map { values[it] }.joinToString(separator = "\t"))
                    }
                }
                is Envelope -> {
                    val envelopeType = EnvelopeType.resolve(meta.getString("envelope.encoding", TaglessEnvelopeType.TAGLESS_ENVELOPE_TYPE))
                            ?: throw RuntimeException("Unknown envelope encoding")
                    val envelopeProperties = meta.getMeta("envelope.properties", Meta.empty()).asMap { it.string }
                    envelopeType.getWriter(envelopeProperties).write(stream, obj)
                }
                is ILoggingEvent -> {
                    printer.println(String(logEncoder.encode(obj)))
                }
                is CharSequence -> printer.println(obj)
                is Record -> printer.println(obj)
                is ValueDescriptor -> {
                    if (obj.required) renderText("(*) ", Color.CYAN)
                    renderText(obj.name, Color.RED)
                    if (obj.multiple) renderText(" (mult)", Color.CYAN)
                    renderText(" (${obj.type.first()})")
                    if (obj.hasDefault()) {
                        val def = obj.default
                        if (def.type == ValueType.STRING) {
                            renderText(" = \"")
                            renderText(def.string, Color.YELLOW)
                            renderText("\": ")
                        } else {
                            renderText(" = ")
                            renderText(def.string, Color.YELLOW)
                            renderText(": ")
                        }
                    } else {
                        renderText(": ")
                    }
                    renderText(obj.info)
                }
                is NodeDescriptor -> {
                    obj.childrenDescriptors().forEach { key, value ->
                        val newMeta = meta.builder
                                .setValue("text.offset", meta.getInt("text.offset", 0) + 1)
                                .setValue("text.bullet", "+")
                        renderText(key + "\n", Color.BLUE)
                        if (value.required) renderText("(*) ", Color.CYAN)

                        renderText(value.name, Color.MAGENTA)

                        if (value.multiple) renderText(" (mult)", Color.CYAN)

                        if (!value.info.isEmpty()) {
                            renderText(": ${value.info}")
                        }
                        render(value, newMeta)
                    }

                    obj.valueDescriptors().forEach { key, value ->
                        val newMeta = meta.builder
                                .setValue("text.offset", meta.getInt("text.offset", 0) + 1)
                                .setValue("text.bullet", "-")
                        renderText(key + "\n", Color.BLUE)
                        render(value, newMeta)
                    }
                }
                is Metoid -> { // render custom metoid
                    val renderType = obj.meta.getString("@output.type", "@default")
                    context.findService(OutputRenderer::class.java) { it.type == renderType }
                            ?.render(this@StreamOutput, obj, meta)
                            ?: renderMeta(obj.meta, meta)
                }
                else -> printer.println(obj)
            }
            printer.flush()
        }
    }

    open fun renderMeta(meta: Meta, options: Meta) {
        printer.println(meta.toString())
    }

    override fun renderText(text: String, vararg attributes: TextAttribute) {
        printer.println(text)
    }

    override fun close() {
        isOpen = false
        stream.close()
    }
}

/**
 * A stream output with ANSI colors enabled
 */
class ANSIStreamOutput(context: Context, stream: OutputStream) : StreamOutput(context, stream) {

    override val logEncoder: Encoder<ILoggingEvent> by lazy {
        object : EncoderBase<ILoggingEvent>() {
            override fun headerBytes(): ByteArray = ByteArray(0)

            override fun footerBytes(): ByteArray = ByteArray(0)

            override fun encode(event: ILoggingEvent): ByteArray {
                return buildString {
                    append(Instant.ofEpochMilli(event.timeStamp).toString() + "\t")
                    //%level [%thread] %logger{10} [%file:%line] %msg%n
                    if (event.threadName != Thread.currentThread().name) {
                        append("[${event.threadName}]\t")
                    }
                    append(IOUtils.wrapANSI(event.loggerName, IOUtils.ANSI_BLUE) + "\t")

                    when (event.level) {
                        Level.ERROR -> appendLine(IOUtils.wrapANSI(event.message, IOUtils.ANSI_RED))
                        Level.WARN -> appendLine(IOUtils.wrapANSI(event.message, IOUtils.ANSI_YELLOW))
                        else -> appendLine(event.message)
                    }
                }.toByteArray()
            }

        }.apply {
            this.context = LoggerFactory.getILoggerFactory() as LoggerContext
            start()
        }
    }

    private fun wrapText(text: String, vararg attributes: TextAttribute): String {
        return attributes.find { it is TextColor }?.let {
            when ((it as TextColor).color) {
                Color.BLACK -> IOUtils.wrapANSI(text, IOUtils.ANSI_BLACK)
                Color.RED -> IOUtils.wrapANSI(text, IOUtils.ANSI_RED)
                Color.GREEN -> IOUtils.wrapANSI(text, IOUtils.ANSI_GREEN)
                Color.YELLOW -> IOUtils.wrapANSI(text, IOUtils.ANSI_YELLOW)
                Color.BLUE -> IOUtils.wrapANSI(text, IOUtils.ANSI_BLUE)
                //Color. -> IOUtils.wrapANSI(text, IOUtils.ANSI_PURPLE)
                Color.CYAN -> IOUtils.wrapANSI(text, IOUtils.ANSI_CYAN)
                Color.WHITE -> IOUtils.wrapANSI(text, IOUtils.ANSI_WHITE)
                else -> {
                    //Color is not resolved
                    text
                }
            }
        } ?: text
    }

    override fun renderText(text: String, vararg attributes: TextAttribute) {
        super.renderText(wrapText(text, *attributes), *attributes)
    }
}

class FileOutput(val file: FileReference) : Output, AutoCloseable {
    override val context: Context
        get() = file.context

    private val streamOutput by lazy {
        StreamOutput(context, file.outputStream)
    }

    override fun render(obj: Any, meta: Meta) {
        when (obj) {
            is Table -> {
                streamOutput.render(
                        buildEnvelope {
                            meta(meta)
                            data {
                                ColumnedDataWriter.writeTable(it, obj, "")
                            }
                        }
                )
            }
            else -> streamOutput.render(obj, meta)
        }
    }

    override fun close() {
        streamOutput.close()
    }

}

sealed class TextAttribute

class TextColor(val color: Color) : TextAttribute()
class TextStrong : TextAttribute()
class TextEmphasis : TextAttribute()