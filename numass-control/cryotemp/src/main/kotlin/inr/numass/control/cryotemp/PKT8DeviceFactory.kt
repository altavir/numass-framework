package inr.numass.control.cryotemp

import hep.dataforge.context.Context
import hep.dataforge.control.devices.Device
import hep.dataforge.meta.Meta
import inr.numass.control.DeviceViewConnection
import inr.numass.control.DeviceViewFactory

/**
 * Created by darksnake on 09-May-17.
 */
class PKT8DeviceFactory : DeviceViewFactory {
    override fun getType(): String {
        return PKT8Device.PKT8_DEVICE_TYPE
    }

    override fun build(context: Context, meta: Meta): PKT8Device {
        return PKT8Device(context, meta)
    }

    override fun buildView(device: Device): DeviceViewConnection<*> {
        return PKT8ViewConnection()
    }
}
