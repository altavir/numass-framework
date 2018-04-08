package inr.numass.control.cryotemp

import hep.dataforge.context.Context
import hep.dataforge.control.devices.DeviceFactory
import hep.dataforge.meta.Meta

/**
 * Created by darksnake on 09-May-17.
 */
class PKT8DeviceFactory : DeviceFactory {
    override val type: String = PKT8Device.PKT8_DEVICE_TYPE

    override fun build(context: Context, meta: Meta): PKT8Device {
        return PKT8Device(context, meta)
    }
}
