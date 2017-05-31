package inr.numass.control

import hep.dataforge.control.devices.Device
import hep.dataforge.control.devices.DeviceFactory

interface DeviceViewFactory : DeviceFactory {
    /**
     * Create but do not connect view connection for the device
     * @return
     */
    fun buildView(device: Device): DeviceViewConnection<*>
}
