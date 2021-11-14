package inr.numass.viewer

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import hep.dataforge.context.Context
import hep.dataforge.context.Global
import hep.dataforge.fx.dfIcon
import javafx.stage.Stage
import org.slf4j.LoggerFactory
import tornadofx.*

/**
 * Created by darksnake on 14-Apr-17.
 */
class Viewer : App(MainView::class) {
    init {
        (LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger).level = Level.INFO
    }

    val context: Context = Global.getContext("numass-viewer")

    override fun start(stage: Stage) {
        super.start(stage)
        stage.icons += dfIcon
    }

    override fun stop() {
        context.close()
        Global.terminate();
        super.stop()
    }
}

internal val App.context get() = (this as Viewer).context