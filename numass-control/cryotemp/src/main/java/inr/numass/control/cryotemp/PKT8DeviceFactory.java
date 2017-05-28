package inr.numass.control.cryotemp;

import hep.dataforge.context.Context;
import hep.dataforge.control.devices.Device;
import hep.dataforge.meta.Meta;
import inr.numass.control.DeviceViewConnection;
import inr.numass.control.DeviceViewFactory;

/**
 * Created by darksnake on 09-May-17.
 */
public class PKT8DeviceFactory implements DeviceViewFactory {
    @Override
    public String getType() {
        return PKT8Device.PKT8_DEVICE_TYPE;
    }

    @Override
    public PKT8Device build(Context context, Meta meta) {
        return new PKT8Device(context, meta);
    }

    @Override
    public DeviceViewConnection buildView(Device device) {
        return PKT8View.build(device.getContext());
    }
}
