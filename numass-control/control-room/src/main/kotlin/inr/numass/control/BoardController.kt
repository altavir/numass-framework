package inr.numass.control

import hep.dataforge.context.Context
import hep.dataforge.context.Global
import hep.dataforge.control.DeviceManager
import hep.dataforge.control.connections.Roles
import hep.dataforge.control.devices.Device
import hep.dataforge.kodex.useMeta
import hep.dataforge.kodex.useMetaList
import hep.dataforge.meta.Meta
import hep.dataforge.server.ServerManager
import hep.dataforge.storage.api.Storage
import hep.dataforge.storage.commons.StorageConnection
import hep.dataforge.storage.commons.StorageManager
import inr.numass.client.ClientUtils
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import tornadofx.*

/**
 * Created by darksnake on 12-May-17.
 */
class BoardController() : Controller(), AutoCloseable {

    val contextProperty = SimpleObjectProperty<Context>(Global)
    var context: Context by contextProperty
        private set

    val storageProperty = SimpleObjectProperty<Storage>(null)

    val serverManagerProperty = objectBinding(contextProperty) {
        context.opt(ServerManager::class.java).orElse(null)
    }

    val devices: ObservableList<Device> = FXCollections.observableArrayList();


    fun configure(meta: Meta) {
        Context.build("NUMASS", Global, meta.getMeta("context", meta)).apply {
            val numassRun = meta.optMeta("numass").map { ClientUtils.getRunName(it) }.orElse("")

            meta.useMeta("storage") {
                pluginManager.load(StorageManager::class.java,it)
            }

            val rootStorage = pluginManager.load(StorageManager::class.java).defaultStorage

            val storage = if (!numassRun.isEmpty()) {
                logger.info("Run information found. Selecting run {}", numassRun)
                rootStorage.buildShelf(numassRun, Meta.empty());
            } else {
                rootStorage
            }
            val connection = StorageConnection(storage)

            val deviceManager = pluginManager.load(DeviceManager::class.java)

            meta.useMetaList("device") {
                it.forEach {
                    deviceManager.buildDevice(it)
                }
            }
            deviceManager.devices.forEach { it.connect(connection, Roles.STORAGE_ROLE) }
        }.also {
            runLater {
                context = it
                devices.setAll(context.get(DeviceManager::class.java).devices.toList());
            }
        }
    }

    override fun close() {
        context.close()
        //Global.terminate()
    }
}