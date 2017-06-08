package inr.numass.control

import ch.qos.logback.classic.Level
import hep.dataforge.control.connections.Roles
import hep.dataforge.control.devices.Device
import hep.dataforge.exceptions.ControlException
import hep.dataforge.meta.Meta
import hep.dataforge.utils.ContextMetaFactory
import javafx.scene.Scene
import javafx.stage.Stage
import org.slf4j.LoggerFactory
import tornadofx.*
import java.util.*
import java.util.function.Predicate

/**
 * Created by darksnake on 14-May-17.
 */
abstract class NumassControlApplication<D : Device> : App() {
    private var device: D by singleAssign()

    override fun start(stage: Stage) {
        Locale.setDefault(Locale.US)// чтобы отделение десятичных знаков было точкой
        val rootLogger = LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME) as ch.qos.logback.classic.Logger
        rootLogger.level = Level.INFO

        device = setupDevice()
        val controller = buildView(device)
        device.connect(controller, Roles.VIEW_ROLE, Roles.DEVICE_LISTENER_ROLE)
        val scene = Scene(controller.pane)
        stage.scene = scene

        stage.show()
        setupStage(stage, device)
        setDFStageIcon(stage)
    }

    /**
     * Build a view connection

     * @return
     */
    protected abstract fun buildView(device: D): DeviceViewConnection<D>

    /**
     * Get a device factory for given device

     * @return
     */
    protected abstract val deviceFactory: ContextMetaFactory<D>

    protected abstract fun setupStage(stage: Stage, device: D)

    protected abstract fun acceptDevice(meta: Meta): Boolean

    private fun setupDevice(): D {
        val config = getConfig(this)
                .orElseGet { readResourceMeta("/config/devices.xml") }

        val ctx = setupContext(config)
        val deviceConfig = findDeviceMeta(config, Predicate<Meta> { this.acceptDevice(it) })
                .orElseThrow { RuntimeException("Device configuration not found") }


        try {
            @Suppress("UNCHECKED_CAST")
            val d = deviceFactory.build(ctx, deviceConfig) as D
            d.init()
            connectStorage(d, config)

            return d
        } catch (e: ControlException) {
            throw RuntimeException("Failed to build device", e)
        }

    }

    override fun stop() {
        try {
            device.shutdown()
        } catch (ex: Exception) {
            LoggerFactory.getLogger(javaClass).error("Failed to shutdown application", ex);
        } finally {
            device.context.close()
            super.stop()
        }
    }


}
