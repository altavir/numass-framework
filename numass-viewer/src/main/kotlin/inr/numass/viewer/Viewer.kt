package inr.numass.viewer

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import hep.dataforge.context.Global
import hep.dataforge.fx.dfIcon
import hep.dataforge.meta.Meta
import hep.dataforge.tables.Table
import hep.dataforge.utils.Misc
import inr.numass.data.analyzers.SimpleAnalyzer
import inr.numass.data.api.NumassBlock
import javafx.stage.Stage
import org.slf4j.LoggerFactory
import tornadofx.*

/**
 * Created by darksnake on 14-Apr-17.
 */
class Viewer : App(MainView::class) {
    init{
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

/**
 * Global point cache
 */
object PointCache{
    private val analyzer = SimpleAnalyzer()
    private val cache: MutableMap<NumassBlock, Table> = Misc.getLRUCache(1000)

    operator fun get(point: NumassBlock): Table {
        return cache.computeIfAbsent(point) { analyzer.getAmplitudeSpectrum(point, Meta.empty()) }
    }
}