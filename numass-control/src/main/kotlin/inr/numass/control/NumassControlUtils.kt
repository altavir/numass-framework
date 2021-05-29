package inr.numass.control

import hep.dataforge.context.Context
import hep.dataforge.context.Global
import hep.dataforge.context.launch
import hep.dataforge.control.connections.Roles
import hep.dataforge.control.devices.Device
import hep.dataforge.fx.dfIcon
import hep.dataforge.io.MetaFileReader
import hep.dataforge.io.XMLMetaReader
import hep.dataforge.meta.Meta
import hep.dataforge.nullable
import hep.dataforge.storage.MutableStorage
import hep.dataforge.storage.StorageConnection
import hep.dataforge.storage.StorageManager
import hep.dataforge.storage.createShelf
import javafx.application.Application
import javafx.stage.Stage
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Paths


/**
 * Created by darksnake on 08-May-17.
 */
const val DEFAULT_CONFIG_LOCATION = "./numass-control.xml"
//val STORING_STATE = "storing"
//val dfIcon: Image = Image(Global::class.java.getResourceAsStream("/img/df.png"))

fun getRunName(config: Meta): String {
    return if (config.hasValue("numass.run")) {
        config.getString("numass.run")
    } else if (config.hasMeta("numass.server")) {
        TODO("Not implemented")
    } else {
        ""
    }
}

/**
 * Create a single or multiple storage connections for a device
 * @param device
 * *
 * @param config
 */
fun Device.connectStorage(config: Meta) {
    //TODO add on reset listener
    if (config.hasMeta("storage") && acceptsRole(Roles.STORAGE_ROLE)) {
        val numassRun = getRunName(config)
        val manager = context.getOrLoad(StorageManager::class.java)

        config.getMetaList("storage").forEach { node ->
            logger.info("Creating storage for device with getMeta: {}", node)
            //building storage in a separate thread
            launch {
                var storage = manager.create(node) as MutableStorage
                if (!numassRun.isEmpty()) {
                    try {
                        storage = storage.createShelf(numassRun)
                    } catch (e: Exception) {
                        logger.error("Failed to build shelf", e)
                    }
                }
                connect(StorageConnection { storage }, Roles.STORAGE_ROLE)
            }
        }
    }
}

fun readResourceMeta(path: String): Meta {
    val resource = Global.getResource(path)
    if (resource != null) {
        return XMLMetaReader().read(resource.stream)
    } else {
        throw RuntimeException("Resource $path not found")
    }
}


fun getConfig(app: Application): Meta? {
    val debugConfig = app.parameters.named["config.resource"]
    if (debugConfig != null) {
        return readResourceMeta(debugConfig)
    }

    var configFileName: String? = app.parameters.named["config"]
    val logger = LoggerFactory.getLogger(app.javaClass)
    if (configFileName == null) {
        logger.info("Configuration path not defined. Loading configuration from {}", DEFAULT_CONFIG_LOCATION)
        configFileName = DEFAULT_CONFIG_LOCATION
    }
    val configFile = Paths.get(configFileName)

    return if (Files.exists(configFile)) {
        MetaFileReader.read(configFile)
    } else {
        logger.warn("Configuration file not found")
        null
    }
}


fun findDeviceMeta(config: Meta, criterion: (Meta) -> Boolean): Meta? {
    return config.getMetaList("device").stream().filter(criterion).findFirst().nullable
}

fun setupContext(meta: Meta): Context {
    val ctx = Global.getContext("NUMASS-CONTROL")
    ctx.plugins.load(StorageManager::class.java)
    return ctx
}

fun setDFStageIcon(stage: Stage) {
    stage.icons.add(dfIcon)
}

