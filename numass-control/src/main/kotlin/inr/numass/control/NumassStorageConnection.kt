package inr.numass.control

import hep.dataforge.control.connections.DeviceConnection
import hep.dataforge.control.connections.Roles
import hep.dataforge.control.devices.Device
import hep.dataforge.kodex.nullable
import hep.dataforge.storage.api.Storage
import hep.dataforge.storage.api.TableLoader
import hep.dataforge.storage.commons.LoaderFactory
import hep.dataforge.storage.commons.StorageConnection
import hep.dataforge.tables.TableFormat
import hep.dataforge.tables.ValuesListener
import hep.dataforge.utils.DateTimeUtils
import hep.dataforge.values.Values
import java.util.*

class NumassStorageConnection(private val loaderName: String? = null, private val formatBuilder: (Device) -> TableFormat) : DeviceConnection(), ValuesListener {
    private val loaderMap = HashMap<Storage, TableLoader>()


    @Synchronized
    override fun accept(point: Values) {
        if (device.states.optBoolean("storing").nullable == true) {
            val format = formatBuilder(device)
            val suffix = DateTimeUtils.fileSuffix()
            val loaderName = "${loaderName ?: device.name}_$suffix"
            device.forEachConnection(Roles.STORAGE_ROLE, StorageConnection::class.java) { connection ->
                try {
                    //create a loader instance for each connected storage
                    val pl = loaderMap.computeIfAbsent(connection.storage){storage ->
                        LoaderFactory.buildPointLoader(storage, loaderName, "", "timestamp", format)
                    }
                    pl.push(point)
                } catch (ex: Exception) {
                    device.logger.error("Push to loader failed", ex)
                }
            }
        }
    }

    fun reset() = close()

    @Synchronized
    override fun close() {
        loaderMap.values.forEach { it ->
            try {
                it.close()
            } catch (ex: Exception) {
                device.logger.error("Failed to close Loader", ex)
            }
        }
        loaderMap.clear()
    }
}