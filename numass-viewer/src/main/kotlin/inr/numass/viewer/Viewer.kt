package inr.numass.viewer

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import hep.dataforge.fx.work.WorkManager
import hep.dataforge.storage.commons.StorageManager
import javafx.stage.Stage
import org.slf4j.LoggerFactory
import tornadofx.*

/**
 * Created by darksnake on 14-Apr-17.
 */
class Viewer : App(MainView::class) {

    override fun start(stage: Stage) {
        (LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger).level = Level.INFO
        StorageManager().startGlobal()
        WorkManager().startGlobal()
        super.start(stage)
    }
}