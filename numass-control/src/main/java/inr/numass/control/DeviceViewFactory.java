package inr.numass.control;

import hep.dataforge.control.devices.Device;
import hep.dataforge.control.devices.DeviceFactory;

public interface DeviceViewFactory extends DeviceFactory {
    /**
     * Create but do not connect view connection for the device
     * @return
     */
    DeviceViewConnection buildView(Device device);
}
