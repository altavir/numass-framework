package inr.numass.control

import hep.dataforge.control.devices.AbstractDevice
import hep.dataforge.nullable
import hep.dataforge.storage.StorageConnection
import hep.dataforge.storage.tables.TableLoader

import hep.dataforge.values.Values
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

/**
 * A helper to store points in multiple loaders
 * Created by darksnake on 16-May-17.
 */
@Deprecated("To be replaced by connection")
class StorageHelper(private val device: AbstractDevice, private val loaderFactory: (StorageConnection) -> TableLoader) : AutoCloseable {
    private val loaderMap = HashMap<StorageConnection, TableLoader>()

    fun push(point: Values) {
        if (device.states.optBoolean("storing").nullable == true) {
            device.forEachConnection("storage", StorageConnection::class.java) { connection ->
                try {
                    val pl = loaderMap.computeIfAbsent(connection, loaderFactory).mutable()
                    device.context.launch(Dispatchers.IO) {
                        pl.append(point)
                    }
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
