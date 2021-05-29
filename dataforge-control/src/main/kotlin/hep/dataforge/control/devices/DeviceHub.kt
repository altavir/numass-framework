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

package hep.dataforge.control.devices

import hep.dataforge.connections.Connection
import hep.dataforge.context.Context
import hep.dataforge.meta.Meta
import hep.dataforge.names.Name
import hep.dataforge.providers.Provider
import hep.dataforge.providers.Provides
import hep.dataforge.providers.ProvidesNames
import java.util.*
import java.util.stream.Stream

/**
 * A hub containing several devices
 */
interface DeviceHub : Provider {

    val deviceNames: List<Name>

    fun optDevice(name: Name): Optional<Device>

    @Provides(DEVICE_TARGET)
    fun optDevice(name: String): Optional<Device> {
        return optDevice(Name.of(name))
    }

    @ProvidesNames(DEVICE_TARGET)
    fun listDevices(): Stream<String> {
        return deviceNames.stream().map{ it.toString() }
    }

    fun getDevices(recursive: Boolean): Stream<Device> {
        return if (recursive) {
            deviceNames.stream().map { it -> optDevice(it).get() }
        } else {
            deviceNames.stream().filter { it -> it.length == 1 }.map { it -> optDevice(it).get() }
        }
    }

    /**
     * Add a connection to each of child devices
     *
     * @param connection
     * @param roles
     */
    fun connectAll(connection: Connection, vararg roles: String) {
        deviceNames.stream().filter { it -> it.length == 1 }
                .map<Optional<Device>>{ this.optDevice(it) }
                .map<Device>{ it.get() }
                .forEach { it ->
                    if (it is DeviceHub) {
                        (it as DeviceHub).connectAll(connection, *roles)
                    } else {
                        it.connect(connection, *roles)
                    }
                }
    }

    fun connectAll(context: Context, meta: Meta) {
        deviceNames.stream().filter { it -> it.length == 1 }
                .map<Optional<Device>>{ this.optDevice(it) }
                .map<Device>{ it.get() }
                .forEach { it ->
                    if (it is DeviceHub) {
                        (it as DeviceHub).connectAll(context, meta)
                    } else {
                        it.connectionHelper.connect(context, meta)
                    }
                }
    }

    companion object {
        const val DEVICE_TARGET = "device"
    }
}
