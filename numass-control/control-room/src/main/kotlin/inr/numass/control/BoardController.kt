package inr.numass.control

import hep.dataforge.context.Context
import hep.dataforge.context.Global
import hep.dataforge.control.DeviceManager
import hep.dataforge.control.devices.Device
import hep.dataforge.kodex.useMeta
import hep.dataforge.kodex.useMetaList
import hep.dataforge.meta.Meta
import hep.dataforge.server.ServerManager
import hep.dataforge.storage.commons.StorageConnection
import hep.dataforge.storage.commons.StorageManager
import inr.numass.client.ClientUtils
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import tornadofx.*
import kotlin.streams.toList

/**
 * Created by darksnake on 12-May-17.
 */
class BoardController() : Controller(), AutoCloseable {

    val contextProperty = SimpleObjectProperty<Context>(Global.instance())
    var context: Context by contextProperty

//    val metaProperty = SimpleObjectProperty<Meta>(Meta.empty())
//    var meta: Meta by metaProperty

    val numassRunProperty = SimpleStringProperty("")
    var numassRun: String by numassRunProperty
        private set

    val storageProperty = nonNullObjectBinding(contextProperty, numassRunProperty) {
        val rootStorage = context.pluginManager.getOrLoad(StorageManager::class.java).defaultStorage

        if (!numassRun.isEmpty()) {
            context.logger.info("Run information found. Selecting run {}", numassRun)
            rootStorage.buildShelf(numassRun, Meta.empty());
        } else {
            rootStorage
        }
    }.apply {
        onChange {
            val connection = StorageConnection(value)
            devices.forEach { device ->
                device.forEachConnection(StorageConnection::class.java) { device.disconnect(it) }//removing all ald storage connections
                device.connect(connection)
            }
        }
    }

    val serverManagerProperty = objectBinding(contextProperty) {
        context.optFeature(ServerManager::class.java).orElse(null)
    }

    val devices: ObservableList<Device> = FXCollections.observableArrayList();

    val deviceManagerProperty = objectBinding(contextProperty) {
        context.optFeature(DeviceManager::class.java).orElse(null)
    }.apply {
        onChange {
            value?.let {
                devices.setAll(it.devices.toList());
            }
        }
    }

//    val deviceViews: ObservableList<DeviceDisplay<*>> = object : ListBinding<DeviceDisplay<*>>() {
//        init {
//            bind(devices)
//        }
//
//        override fun computeValue(): ObservableList<DeviceDisplay<*>> {
//            val manager = deviceManagerProperty.value
//            return if (manager == null) {
//                FXCollections.emptyObservableList();
//            } else {
//                manager.deviceNames()
//                        .filter { it.length == 1 } // select top level devices
//                        .map { manager.optDevice(it) }
//                        .filter { it.isPresent }
//                        .map { it.get().getDisplay() }
//                        .toList().observable()
//            }
//        }
//    }

    fun configure(meta: Meta) {
        Context.build("NUMASS", Global.instance(), meta.getMeta("context", meta)).apply {
            meta.useMeta("storage") {
                pluginManager.getOrLoad(StorageManager::class.java).configure(it);
            }
            meta.useMetaList("device") {
                it.forEach {
                    pluginManager.getOrLoad(DeviceManager::class.java).buildDevice(it)
                }
            }
            meta.useMeta("numass") {
                numassRun = ClientUtils.getRunName(it)
            }
        }.also {
            runLater {
                context = it
            }
        }
    }

    override fun close() {
        context.close()
        //Global.terminate()
    }
//    val devices: ObservableList<DeviceDisplay<*>> = FXCollections.observableArrayList<DeviceDisplay<*>>();
//
//    val contextProperty = SimpleObjectProperty<Context>(Global.instance())
//    var context: Context by contextProperty
//        private set
//
//    val storageProperty = SimpleObjectProperty<Storage>()
//    var storage: Storage? by storageProperty
//        private set
//
//    val serverManagerProperty = SimpleObjectProperty<ServerManager>()
//    var serverManager: ServerManager? by serverManagerProperty
//        private set
//
//    fun load(app: Application) {
//        runAsync {
//            getConfig(app).ifPresent {
//                val context = Context.build("NUMASS", Global.instance(), it)
//                load(context, it)
//            }
//        }
//
//    }
//
//    private fun load(context: Context, meta: Meta) {
//        this.context = context;
//        devices.clear();
//        meta.getMetaList("device").forEach {
//            try {
//                Platform.runLater { devices.add(buildDeviceView(context, it)) };
//            } catch (ex: Exception) {
//                context.logger.error("Can't build device view", ex);
//            }
//        }
//
//        if (meta.hasMeta("storage")) {
//            val st = buildStorage(context, meta);
//            val storageConnection = StorageConnection(storage);
//            devices.forEach {
//                if (it.device.acceptsRole(Roles.STORAGE_ROLE)) {
//                    it.device.connect(storageConnection, Roles.STORAGE_ROLE);
//                }
//            }
//            Platform.runLater {
//                storage = st
//                meta.optMeta("server").ifPresent { serverMeta ->
//                    val sm = context.getPluginManager().getOrLoad(ServerManager::class.java);
//                    sm.configure(serverMeta)
//
//                    sm.bind(NumassStorageServerObject(serverManager, storage, "numass-storage"));
//                    serverManager = sm
//                }
//            }
//        }
//    }
//
//    private fun buildDeviceView(context: Context, deviceMeta: Meta): DeviceDisplay<*> {
//        context.logger.info("Building device with meta: {}", deviceMeta)
//        val device = context.loadFeature("devices", DeviceManager::class.java).buildDevice(deviceMeta)
//        device.init();
//        return device.getDisplay();
//    }
//
//    private fun buildStorage(context: Context, meta: Meta): Storage {
//        val storageMeta = meta.getMeta("storage").builder
//                .putValue("readOnly", false)
//                .putValue("monitor", true)
//
//        context.logger.info("Creating storage for server with meta {}", storageMeta)
//        var storage = StorageFactory.buildStorage(context, storageMeta);
//
//        val numassRun = ClientUtils.getRunName(meta)
//        if (!numassRun.isEmpty()) {
//            context.logger.info("Run information found. Selecting run {}", numassRun)
//            storage = storage.buildShelf(numassRun, Meta.empty());
//        }
//        return storage;
//    }
//
//    override fun close() {
//        devices.forEach {
//            it.close()
//        }
//        context.close();
//    }
}