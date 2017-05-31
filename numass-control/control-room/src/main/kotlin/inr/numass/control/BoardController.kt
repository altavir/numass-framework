package inr.numass.control

import hep.dataforge.context.Context
import hep.dataforge.context.Global
import hep.dataforge.control.connections.Roles
import hep.dataforge.control.connections.StorageConnection
import hep.dataforge.meta.Meta
import hep.dataforge.server.ServerManager
import hep.dataforge.storage.api.Storage
import hep.dataforge.storage.commons.StorageFactory
import inr.numass.client.ClientUtils
import inr.numass.server.NumassStorageServerObject
import javafx.application.Application
import javafx.application.Platform
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import tornadofx.*
import java.io.File

/**
 * Created by darksnake on 12-May-17.
 */
class BoardController() : Controller(), AutoCloseable {
    val devices: ObservableList<DeviceViewConnection<*>> = FXCollections.observableArrayList<DeviceViewConnection<*>>();

    val contextProperty = SimpleObjectProperty<Context>(Global.instance())
    var context: Context by contextProperty
        private set

    val storageProperty = SimpleObjectProperty<Storage>()
    var storage: Storage? by storageProperty
        private set

    val serverManagerProperty = SimpleObjectProperty<ServerManager>()
    var serverManager: ServerManager? by serverManagerProperty
        private set

    fun load(app: Application) {
        runAsync {
            getConfig(app).ifPresent {
                val libDir = File(app.parameters.named.getOrDefault("libPath", "../lib"));
                val contextBuilder = Context
                        .builder("NUMASS-SERVER");
                if (libDir.exists()) {
                    Global.logger().info("Found library directory {}. Loading it into server context", libDir)
                    contextBuilder.classPath(libDir.listFiles { _, name -> name.endsWith(".jar") }.map { it.toURI().toURL() })
                }
                context = contextBuilder.build();
                load(context, it);
            }
        }

    }

    private fun load(context: Context, meta: Meta) {
        this.context = context;
        devices.clear();
        meta.getMetaList("device").forEach {
            try {
                Platform.runLater { devices.add(buildDeviceView(context, it)) };
            } catch (ex: Exception) {
                context.logger.error("Can't build device view", ex);
            }
        }

        if (meta.hasMeta("storage")) {
            val st = buildStorage(context, meta);
            val storageConnection = StorageConnection(storage);
            devices.forEach {
                if (it.device.acceptsRole(Roles.STORAGE_ROLE)) {
                    it.device.connect(storageConnection, Roles.STORAGE_ROLE);
                }
            }
            Platform.runLater {
                storage = st
                meta.optMeta("server").ifPresent { serverMeta ->
                    val sm = context.pluginManager().getOrLoad(ServerManager::class.java);
                    sm.configure(serverMeta)

                    sm.bind(NumassStorageServerObject(serverManager, storage, "numass-storage"));
                    serverManager = sm
                }
            }
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

    override fun close() {
        devices.forEach {
            it.close()
        }
        context.close();
    }
}