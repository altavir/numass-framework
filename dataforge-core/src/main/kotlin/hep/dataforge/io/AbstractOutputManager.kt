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
package hep.dataforge.io

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.Appender
import ch.qos.logback.core.UnsynchronizedAppenderBase
import hep.dataforge.context.BasicPlugin
import hep.dataforge.context.Context
import hep.dataforge.io.OutputManager.Companion.LOGGER_APPENDER_NAME
import hep.dataforge.io.output.Output
import hep.dataforge.meta.Meta

/**
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
abstract class AbstractOutputManager(meta: Meta = Meta.empty()) : OutputManager, BasicPlugin(meta) {
    /**
     * Create logger appender for this manager
     *
     * @return
     */
    open fun createLoggerAppender(): Appender<ILoggingEvent> {
        return object : UnsynchronizedAppenderBase<ILoggingEvent>() {
            override fun append(eventObject: ILoggingEvent) {
                get("@log").render(eventObject)
            }
        }
    }

    private fun addLoggerAppender(logger: Logger) {
        val loggerContext = logger.loggerContext
        val appender = createLoggerAppender()
        appender.name = LOGGER_APPENDER_NAME
        appender.context = loggerContext
        appender.start()
        logger.addAppender(appender)
    }

    private fun removeLoggerAppender(logger: Logger) {
        val app = logger.getAppender(LOGGER_APPENDER_NAME)
        if (app != null) {
            logger.detachAppender(app)
            app.stop()
        }
    }

    override fun attach(context: Context) {
        super.attach(context)
        if (context.logger is ch.qos.logback.classic.Logger) {
            addLoggerAppender(context.logger as ch.qos.logback.classic.Logger)
        }
    }

    override fun detach() {
        if (logger is ch.qos.logback.classic.Logger) {
            removeLoggerAppender(logger as ch.qos.logback.classic.Logger)
        }
        super.detach()
    }
}

/**
 * The simple output manager, which redirects everything to a single output stream
 */
class SimpleOutputManager(val default: Output) : AbstractOutputManager() {
    override fun get(meta: Meta): Output = default
}

