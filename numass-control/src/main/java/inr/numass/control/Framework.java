package inr.numass.control;

import hep.dataforge.control.devices.Device;
import hep.dataforge.fx.fragments.FXFragment;
import hep.dataforge.fx.fragments.LogFragment;
import hep.dataforge.utils.MetaFactory;

/**
 * Created by darksnake on 20-Oct-16.
 */
public interface Framework<T extends Device> {
    LogFragment getLogFragment();
    FXFragment getPlotFragment();
    DeviceFragment<T> getDeviceFragment();
    MetaFactory<T> getDeviceFactory();
}
