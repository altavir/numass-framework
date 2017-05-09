package inr.numass.control.msp;

import hep.dataforge.context.Context;
import hep.dataforge.control.devices.DeviceFactory;
import hep.dataforge.meta.Meta;

/**
 * Created by darksnake on 09-May-17.
 */
public class MspDeviceFactory implements DeviceFactory<MspDevice> {
    @Override
    public String getType() {
        return MspDevice.MSP_DEVICE_TYPE;
    }

    @Override
    public MspDevice build(Context context, Meta config) {
        MspDevice device = new MspDevice();
        device.setContext(context);
        device.configure(config);
        return device;
    }
}
