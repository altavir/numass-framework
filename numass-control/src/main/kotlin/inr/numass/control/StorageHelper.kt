package inr.numass.control

import hep.dataforge.control.connections.StorageConnection
import hep.dataforge.control.devices.AbstractDevice
import hep.dataforge.exceptions.StorageException
import hep.dataforge.storage.api.PointLoader
import hep.dataforge.tables.DataPoint
import java.util.*
import java.util.function.Function

/**
 * A helper to store points in multiple loaders
 * Created by darksnake on 16-May-17.
 */
class StorageHelper(private val device: AbstractDevice, private val loaderFactory: Function<StorageConnection, PointLoader>) : AutoCloseable {
    private val loaderMap = HashMap<StorageConnection, PointLoader>()

    fun push(point: DataPoint) {
        if (!device.hasState("storing") || device.getState("storing").booleanValue()) {
            device.forEachConnection("storage", StorageConnection::class.java) { connection ->
                val pl = loaderMap.computeIfAbsent(connection, loaderFactory)
                try {
                    pl.push(point)
                } catch (ex: StorageException) {
                    device.logger.error("Push to loader failed", ex)
                }
            }
        }
    }


    override fun close() {
        loaderMap.values.forEach { it ->
            try {
                it.close()
            } catch (ex: Exception) {
                device.logger.error("Failed to close Loader", ex)
            }
        }
    }
}
