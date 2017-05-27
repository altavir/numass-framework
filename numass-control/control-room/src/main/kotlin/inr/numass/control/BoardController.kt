package inr.numass.control

import hep.dataforge.context.Context
import hep.dataforge.control.connections.Roles
import hep.dataforge.control.connections.StorageConnection
import hep.dataforge.meta.Meta
import hep.dataforge.server.ServerManager
import hep.dataforge.storage.api.Storage
import hep.dataforge.storage.commons.StorageFactory
import inr.numass.client.ClientUtils
import inr.numass.server.NumassStorageServerObject
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import tornadofx.*

/**
 * Created by darksnake on 12-May-17.
 */
class BoardController() : Controller() {
    val devices: ObservableList<DeviceViewConnection<*>> = FXCollections.observableArrayList<DeviceViewConnection<*>>();

    val storageProperty = SimpleObjectProperty<Storage>()
    var storage by storageProperty
        private set

    val serverManagerProperty = SimpleObjectProperty<ServerManager>()
    var serverManager: ServerManager by serverManagerProperty
        private set

    fun load(context: Context, meta: Meta) {
        devices.clear();
        meta.getMetaList("device").forEach {
            try {
                devices.add(buildDeviceView(context, it));
            } catch (ex: Exception) {
                context.logger.error("Can't build device view", ex);
            }
        }

        if (meta.hasMeta("storage")) {
            storage = buildStorage(context, meta);
            val storageConnection = StorageConnection(storage);
            devices.forEach {
                if (it.device.acceptsRole(Roles.STORAGE_ROLE)) {
                    it.device.connect(storageConnection, Roles.STORAGE_ROLE);
                }
            }
        }

        meta.optMeta("server").ifPresent { serverMeta ->
            serverManager = context.pluginManager().getOrLoad(ServerManager::class.java);
            serverManager.configure(serverMeta)

            serverManager.bind(NumassStorageServerObject(serverManager, storage, "numass-storage"));
        }
    }

    private fun buildDeviceView(context: Context, deviceMeta: Meta): DeviceViewConnection<*> {
        context.logger.info("Building device with meta: {}", deviceMeta)
        val factory = context.serviceStream(DeviceViewFactory::class.java)
                .filter { it.type == deviceMeta.getString("type") }
                .findFirst();

        if (factory.isPresent) {
            val device = factory.get().build(context, deviceMeta);
            val view = factory.get().buildView(device);
            device.connect(view, Roles.VIEW_ROLE, Roles.DEVICE_LISTENER_ROLE)
            device.init();
            return view;
        } else {
            throw RuntimeException("Device factory not found");
        }
    }

    private fun buildStorage(context: Context, meta: Meta): Storage {
        val storageMeta = meta.getMeta("storage").builder
                .putValue("readOnly", false)
                .putValue("monitor", true)

        context.logger.info("Creating storage for server with meta {}", storageMeta)
        var storage = StorageFactory.buildStorage(context, storageMeta);

        val numassRun = ClientUtils.getRunName(meta)
        if (!numassRun.isEmpty()) {
            context.logger.info("Run information found. Selecting run {}", numassRun)
            storage = storage.buildShelf(numassRun, Meta.empty());
        }
        return storage;
    }
}