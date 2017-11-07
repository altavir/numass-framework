package inr.numass.server

import hep.dataforge.context.Context
import hep.dataforge.context.Global
import hep.dataforge.control.DeviceManager
import hep.dataforge.meta.Meta
import hep.dataforge.server.KodexServer
import hep.dataforge.storage.commons.StorageManager

fun main(args: Array<String>) {
    val meta = Meta.empty();
    val context = Context.build("SERVER", Global.instance(), meta)
    val server = KodexServer(context, meta)

    context.optFeature(StorageManager::class.java).ifPresent{
        server.intercept(storageInterceptor)
    }

    context.optFeature(DeviceManager::class.java).ifPresent{
        server.intercept(deviceInterceptor)
    }

    server.start()
}