package inr.numass.control

import hep.dataforge.context.Context
import hep.dataforge.context.Global
import hep.dataforge.control.connections.Roles
import hep.dataforge.control.devices.Device
import hep.dataforge.exceptions.StorageException
import hep.dataforge.fx.dfIcon
import hep.dataforge.io.MetaFileReader
import hep.dataforge.io.XMLMetaReader
import hep.dataforge.meta.Meta
import hep.dataforge.storage.commons.StorageConnection
import hep.dataforge.storage.commons.StorageFactory
import hep.dataforge.storage.commons.StorageManager
import inr.numass.client.ClientUtils
import javafx.application.Application
import javafx.stage.Stage
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.text.ParseException
import java.util.*
import java.util.function.Predicate

/**
 * Created by darksnake on 08-May-17.
 */
val DEFAULT_CONFIG_LOCATION = "./numass-control.xml"
//val STORING_STATE = "storing"
//val dfIcon: Image = Image(Global::class.java.getResourceAsStream("/img/df.png"))

/**
 * Create a single or multiple storage connections for a device

 * @param device
 * *
 * @param config
 */
fun connectStorage(device: Device, config: Meta) {
    //TODO add on reset listener
    if (config.hasMeta("storage") && device.acceptsRole(Roles.STORAGE_ROLE)) {
        val numassRun = ClientUtils.getRunName(config)
        config.getMetaList("storage").forEach { node ->
            device.context.logger.info("Creating storage for device with meta: {}", node)
            //building storage in a separate thread
            Thread {
                var storage = StorageFactory.buildStorage(device.context, node)
                if (!numassRun.isEmpty()) {
                    try {
                        storage = storage.buildShelf(numassRun, Meta.empty())
                    } catch (e: StorageException) {
                        device.context.logger.error("Failed to build shelf", e)
                    }

                }
                device.connect(StorageConnection(storage), Roles.STORAGE_ROLE)
            }.start()
        }
    }
}

fun readResourceMeta(path: String): Meta {
    try {
        return XMLMetaReader().read(Global::class.java.getResourceAsStream(path))
    } catch (e: IOException) {
        throw RuntimeException(e)
    } catch (e: ParseException) {
        throw RuntimeException(e)
    }

}

fun getConfig(app: Application): Optional<Meta> {
    val debugConfig = app.parameters.named["config.resource"]
    if (debugConfig != null) {
        return Optional.ofNullable(readResourceMeta(debugConfig))
    }

    var configFileName: String? = app.parameters.named["config"]
    val logger = LoggerFactory.getLogger(app.javaClass)
    if (configFileName == null) {
        logger.info("Configuration path not defined. Loading configuration from {}", DEFAULT_CONFIG_LOCATION)
        configFileName = DEFAULT_CONFIG_LOCATION
    }
    val configFile = Paths.get(configFileName)

    if (Files.exists(configFile)) {
        try {
            val config = MetaFileReader.read(configFile)
            return Optional.of(config)
        } catch (e: IOException) {
            throw RuntimeException(e)
        } catch (e: ParseException) {
            throw RuntimeException(e)
        }

    } else {
        logger.warn("Configuration file not found")
        return Optional.empty<Meta>()
    }
}


fun findDeviceMeta(config: Meta, criterion: Predicate<Meta>): Optional<Meta> {
    return config.getMetaList("device").stream().filter(criterion).findFirst().map { it -> it }
}

fun setupContext(meta: Meta): Context {
    val ctx = Global.getContext("NUMASS-CONTROL")
    ctx.getPluginManager().getOrLoad(StorageManager::class.java)
    return ctx
}

fun setDFStageIcon(stage: Stage) {
    stage.icons.add(dfIcon)
}

