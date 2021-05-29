package inr.numass.viewer

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
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

    override fun start(stage: Stage) {
        stage.icons += dfIcon
        super.start(stage)
    }

    override fun stop() {
        super.stop()
        Global.terminate();
    }
}