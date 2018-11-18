package inr.numass.control

import ch.qos.logback.classic.Level
import hep.dataforge.control.devices.Device
import hep.dataforge.control.devices.DeviceFactory
import hep.dataforge.exceptions.ControlException
import hep.dataforge.meta.Meta
import hep.dataforge.optional
import javafx.scene.Scene
import javafx.stage.Stage
import org.slf4j.LoggerFactory
import tornadofx.*
import java.util.*

/**
 * Created by darksnake on 14-May-17.
 */
abstract class NumassControlApplication<in D : Device> : App() {
    private var device: D? = null

    override fun start(stage: Stage) {
        Locale.setDefault(Locale.US)// чтобы отделение десятичных знаков было точкой
        val rootLogger = LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME) as ch.qos.logback.classic.Logger
        rootLogger.level = Level.INFO

        device = setupDevice().also {
            val controller = it.getDisplay()
            val scene = Scene(controller.view?.root ?: controller.getBoardView())
            stage.scene = scene

            stage.show()
            setupStage(stage, it)
            setDFStageIcon(stage)
        }
    }

    /**
     * Get a device factory for given device

     * @return
     */
    protected abstract val deviceFactory: DeviceFactory

    protected abstract fun setupStage(stage: Stage, device: D)


    abstract fun getDeviceMeta(config: Meta): Meta

    private fun setupDevice(): D {
        val config = getConfig(this).optional.orElseGet { readResourceMeta("config/devices.xml") }

        val ctx = setupContext(config)
        val deviceConfig = getDeviceMeta(config)


        try {
            @Suppress("UNCHECKED_CAST")
            val d = deviceFactory.build(ctx, deviceConfig) as D
            d.init()
            d.connectStorage(config)

            return d
        } catch (e: ControlException) {
            throw RuntimeException("Failed to build device", e)
        }

    }

    override fun stop() {
        try {
            device?.shutdown()
        } catch (ex: Exception) {
            LoggerFactory.getLogger(javaClass).error("Failed to properly shutdown application", ex);
            device?.context?.close()
        } finally {
            super.stop()
        }
    }


}
