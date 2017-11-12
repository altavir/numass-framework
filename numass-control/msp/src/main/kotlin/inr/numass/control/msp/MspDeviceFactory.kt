package inr.numass.control.msp

import hep.dataforge.context.Context
import hep.dataforge.control.devices.DeviceFactory
import hep.dataforge.meta.Meta

/**
 * Created by darksnake on 09-May-17.
 */
class MspDeviceFactory: DeviceFactory {
    override fun getType(): String {
        return MspDevice.MSP_DEVICE_TYPE
    }

    override fun build(context: Context, config: Meta): MspDevice {
        return MspDevice(context, config)
    }
}
