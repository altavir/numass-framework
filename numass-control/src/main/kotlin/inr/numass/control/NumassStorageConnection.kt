package inr.numass.control

import hep.dataforge.control.connections.DeviceConnection
import hep.dataforge.control.connections.Roles
import hep.dataforge.control.devices.Device
import hep.dataforge.nullable
import hep.dataforge.storage.MutableStorage
import hep.dataforge.storage.MutableTableLoader
import hep.dataforge.storage.Storage
import hep.dataforge.storage.StorageConnection
import hep.dataforge.storage.files.createTable
import hep.dataforge.tables.TableFormat
import hep.dataforge.tables.ValuesListener
import hep.dataforge.utils.DateTimeUtils
import hep.dataforge.values.Values
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class NumassStorageConnection(private val loaderName: String? = null, private val formatBuilder: (Device) -> TableFormat) : DeviceConnection(), ValuesListener {
    private val loaderMap = HashMap<Storage, MutableTableLoader>()


    @Synchronized
    override fun accept(point: Values) {
        if (device.states.optBoolean("storing").nullable == true) {
            val format = formatBuilder(device)
            val suffix = DateTimeUtils.fileSuffix()
            val loaderName = "${loaderName ?: device.name}_$suffix"
            device.forEachConnection(Roles.STORAGE_ROLE, StorageConnection::class.java) { connection ->
                try {
                    connection.context.launch(Dispatchers.IO) {
                        //create a loader instance for each connected storage
                        val pl = loaderMap.getOrPut(connection.storage) {
                            (connection.storage as MutableStorage).createTable(loaderName, format)
                        }
                        pl.append(point)
                    }
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