/*
 * Copyright  2018 Alexander Nozik.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package hep.dataforge.control

import hep.dataforge.connections.Connection
import hep.dataforge.context.*
import hep.dataforge.control.devices.Device
import hep.dataforge.control.devices.DeviceFactory
import hep.dataforge.control.devices.DeviceHub
import hep.dataforge.exceptions.ControlException
import hep.dataforge.meta.Meta
import hep.dataforge.names.Name
import java.util.*

/**
 * A plugin for creating and using different devices
 * Created by darksnake on 11-Oct-16.
 */
@PluginDef(name = "devices", info = "Management plugin for devices an their interaction")
class DeviceManager : BasicPlugin(), DeviceHub {

    /**
     * Registered devices
     */
    private val _devices = HashMap<Name, Device>()

    /**
     * the list of top level devices
     */
    val devices: Collection<Device> = _devices.values

    override val deviceNames: List<Name>
        get() = _devices.entries.flatMap { entry ->
            if (entry.value is DeviceHub) {
                (entry.value as DeviceHub).deviceNames.map { it -> entry.key.plus(it) }
            } else {
                listOf(entry.key)
            }
        }


    fun add(device: Device) {
        val name = Name.ofSingle(device.name)
        if (_devices.containsKey(name)) {
            logger.warn("Replacing existing device in device manager!")
            remove(name)
        }
        _devices[name] = device
    }

    fun remove(name: Name) {
        Optional.ofNullable(this._devices.remove(name)).ifPresent { it ->
            try {
                it.shutdown()
            } catch (e: ControlException) {
                logger.error("Failed to stop the device: " + it.name, e)
            }
        }
    }


    fun buildDevice(deviceMeta: Meta): Device {
        val factory = context
                .findService(DeviceFactory::class.java) { it.type == ControlUtils.getDeviceType(deviceMeta) }
                ?: throw RuntimeException("Can't find factory for given device type")
        val device = factory.build(context, deviceMeta)

        deviceMeta.getMetaList("connection").forEach { connectionMeta -> device.connectionHelper.connect(context, connectionMeta) }

        add(device)
        return device
    }

    override fun optDevice(name: Name): Optional<Device> {
        return when {
            name.isEmpty() -> throw IllegalArgumentException("Can't provide a device with zero name")
            name.length == 1 -> Optional.ofNullable(_devices[name])
            else -> Optional.ofNullable(_devices[name.first]).flatMap { hub ->
                if (hub is DeviceHub) {
                    (hub as DeviceHub).optDevice(name.cutFirst())
                } else {
                    Optional.empty()
                }
            }
        }
    }

    override fun detach() {
        _devices.values.forEach { it ->
            try {
                it.shutdown()
            } catch (e: ControlException) {
                logger.error("Failed to stop the device: " + it.name, e)
            }
        }
        super.detach()
    }

    override fun connectAll(connection: Connection, vararg roles: String) {
        this._devices.values.forEach { device -> device.connect(connection, *roles) }
    }

    override fun connectAll(context: Context, meta: Meta) {
        this._devices.values.forEach { device -> device.connectionHelper.connect(context, meta) }
    }

    class Factory : PluginFactory() {

        override val type: Class<out Plugin>
            get() = DeviceManager::class.java

        override fun build(meta: Meta): Plugin {
            return DeviceManager()
        }
    }
}
