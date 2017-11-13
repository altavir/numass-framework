package inr.numass.control

import hep.dataforge.control.devices.AbstractDevice
import hep.dataforge.storage.api.TableLoader
import hep.dataforge.storage.commons.StorageConnection
import hep.dataforge.values.Values
import java.util.*

/**
 * A helper to store points in multiple loaders
 * Created by darksnake on 16-May-17.
 */
class StorageHelper(private val device: AbstractDevice, private val loaderFactory: (StorageConnection) -> TableLoader) : AutoCloseable {
    private val loaderMap = HashMap<StorageConnection, TableLoader>()

    fun push(point: Values) {
        if (!device.hasState("storing") || device.getState("storing").booleanValue()) {
            device.forEachConnection("storage", StorageConnection::class.java) { connection ->
                try {
                    val pl = loaderMap.computeIfAbsent(connection, loaderFactory)
                    pl.push(point)
                } catch (ex: Exception) {
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
