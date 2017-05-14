package inr.numass.control;

import hep.dataforge.control.devices.Device;
import hep.dataforge.control.devices.DeviceListener;
import hep.dataforge.fx.fragments.FXFragment;
import javafx.scene.Parent;

/**
 * Created by darksnake on 20-Oct-16.
 */
public abstract class DeviceFragment<T extends Device> extends FXFragment implements DeviceListener {

    private final T device;

    protected DeviceFragment(T device) {
        this.device = device;
    }

    @Override
    protected Parent buildRoot() {
        return buildRoot(device);
    }

    protected abstract Parent buildRoot(T device);


    @Override
    public void evaluateDeviceException(Device device, String message, Throwable exception) {
        //do something pretty
    }
}
