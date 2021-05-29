/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.fx.fragments

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.Appender
import ch.qos.logback.core.AppenderBase
import hep.dataforge.context.Global
import hep.dataforge.fx.output.FXTextOutput
import org.slf4j.LoggerFactory
import tornadofx.*
import java.io.PrintStream
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.function.BiConsumer

/**
 * @author Alexander Nozik
 */
class LogFragment : Fragment("DataForge output log") {

    private val timeFormatter = DateTimeFormatter.ISO_LOCAL_TIME

    private var formatter: BiConsumer<FXTextOutput, String>? = null
    private val loggerFormatter = { text: FXTextOutput, eventObject: ILoggingEvent ->
        val style = when (eventObject.level.toString()) {
            "DEBUG" -> "-fx-fill: green"
            "WARN" -> "-fx-fill: orange"
            "ERROR" -> "-fx-fill: red"
            else -> "-fx-fill: black"
        }

        runLater {
            val time = Instant.ofEpochMilli(eventObject.timeStamp)
            text.append(timeFormatter.format(LocalDateTime.ofInstant(time, ZoneId.systemDefault())) + ": ")

            text.appendColored(eventObject.loggerName, "gray")

            text.appendStyled(eventObject.formattedMessage.replace("\n", "\n\t") + "\r\n", style)
        }

    }

    val outputPane = FXTextOutput(Global).apply {
        setMaxLines(2000)
    }

    private val logAppender: Appender<ILoggingEvent> = object : AppenderBase<ILoggingEvent>() {
        override fun append(eventObject: ILoggingEvent) {
            synchronized(this) {
                loggerFormatter(outputPane, eventObject)
            }
        }
    }.apply {
        name = FX_LOG_APPENDER_NAME
        start()
    }

    override val root = outputPane.view.root
    private var stdHooked = false


    /**
     * Set custom formatter for text
     *
     * @param formatter
     */
    fun setFormatter(formatter: BiConsumer<FXTextOutput, String>) {
        this.formatter = formatter
    }

    fun appendText(text: String) {
        if (formatter == null) {
            outputPane.renderText(text)
        } else {
            formatter!!.accept(outputPane, text)
        }
    }

    fun appendLine(text: String) {
        appendText(text + "\r\n")
    }

    fun addRootLogHandler() {
        addLogHandler(Logger.ROOT_LOGGER_NAME)
    }

    fun addLogHandler(loggerName: String) {
        addLogHandler(LoggerFactory.getLogger(loggerName))
    }

    fun addLogHandler(logger: org.slf4j.Logger) {
        if (logger is Logger) {
            logger.addAppender(logAppender)
        } else {
            LoggerFactory.getLogger(javaClass).error("Failed to add log handler. Only Logback loggers are supported.")
        }

    }

    /**
     * Redirect copy of std streams to this console window
     */
    @Deprecated("")
    fun hookStd() {
        if (!stdHooked) {
            System.setOut(PrintStream(outputPane.stream))
            System.setErr(PrintStream(outputPane.stream))
            stdHooked = true
        }
    }

    /**
     * Restore default std streams
     */
    @Deprecated("")
    fun restoreStd() {
        if (stdHooked) {
            System.setOut(STD_OUT)
            System.setErr(STD_ERR)
            stdHooked = false
        }
    }

    override fun onDelete() {
        super.onDelete()
        logAppender.stop()
    }

    companion object {

        private val STD_OUT = System.out
        private val STD_ERR = System.err

        private const val FX_LOG_APPENDER_NAME = "hep.dataforge.fx"
    }

}
