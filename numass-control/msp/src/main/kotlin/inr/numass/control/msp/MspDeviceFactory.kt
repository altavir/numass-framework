package inr.numass.control.msp

import hep.dataforge.context.Context
import hep.dataforge.control.devices.Device
import hep.dataforge.meta.Meta
import inr.numass.control.DeviceViewConnection
import inr.numass.control.DeviceViewFactory

/**
 * Created by darksnake on 09-May-17.
 */
class MspDeviceFactory : DeviceViewFactory {
    override fun getType(): String {
        return MspDevice.MSP_DEVICE_TYPE
    }

    override fun build(context: Context, config: Meta): MspDevice {
        val device = MspDevice(context, config)
        return device
    }

    override fun buildView(device: Device): DeviceViewConnection<*> {
        return MspViewConnection()
    }
}
