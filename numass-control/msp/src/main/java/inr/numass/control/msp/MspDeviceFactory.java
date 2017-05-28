package inr.numass.control.msp;

import hep.dataforge.context.Context;
import hep.dataforge.control.devices.Device;
import hep.dataforge.meta.Meta;
import inr.numass.control.DeviceViewConnection;
import inr.numass.control.DeviceViewFactory;
import inr.numass.control.msp.fx.MspView;

/**
 * Created by darksnake on 09-May-17.
 */
public class MspDeviceFactory implements DeviceViewFactory {
    @Override
    public String getType() {
        return MspDevice.MSP_DEVICE_TYPE;
    }

    @Override
    public MspDevice build(Context context, Meta config) {
        MspDevice device = new MspDevice(context,config);
        return device;
    }

    @Override
    public DeviceViewConnection buildView(Device device) {
        return MspView.build(device.getContext());
    }
}
