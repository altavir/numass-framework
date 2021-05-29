/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.control.connections

import hep.dataforge.connections.Connection
import hep.dataforge.control.devices.Device

abstract class DeviceConnection : Connection {

    private var _device: Device? = null
    val device: Device
        get() = _device ?: throw RuntimeException("Connection closed")

    override fun isOpen(): Boolean {
        return this._device != null
    }

    override fun open(device: Any) {
        this._device = Device::class.java.cast(device)
    }

    override fun close() {
        this._device = null
    }

}
