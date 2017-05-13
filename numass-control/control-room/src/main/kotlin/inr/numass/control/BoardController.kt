package inr.numass.control

import hep.dataforge.context.Context
import hep.dataforge.context.Global
import hep.dataforge.control.devices.Device
import hep.dataforge.meta.Meta
import hep.dataforge.storage.api.Storage
import hep.dataforge.storage.commons.StorageFactory
import inr.numass.client.ClientUtils
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.Node
import tornadofx.*

/**
 * Created by darksnake on 12-May-17.
 */
class BoardController(val context: Context = Global.instance(), val meta: Meta) : Controller() {
    val devices: ObservableList<Pair<Device, Node?>> = FXCollections.observableArrayList<Pair<Device, Node?>>();

    val storage: Storage? by lazy {
        if (meta.hasMeta("storage")) {
            val numassRun = ClientUtils.getRunName(meta)
            var storage = StorageFactory.buildStorage(context, meta.getMeta("storage"));
            if(! numassRun.isEmpty()){
                storage = storage.buildShelf(numassRun, Meta.empty());
            }
            return@lazy storage;
        } else {
            return@lazy null;
        }
    }
}