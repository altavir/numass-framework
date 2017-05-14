package inr.numass.control;

import hep.dataforge.control.connections.DeviceConnection;
import hep.dataforge.control.devices.Device;
import hep.dataforge.control.devices.DeviceListener;
import hep.dataforge.fx.FXObject;
import hep.dataforge.values.Value;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by darksnake on 14-May-17.
 */
public abstract class DeviceViewConnection<D extends Device> extends DeviceConnection<D> implements DeviceListener, FXObject {
    private Map<String, ObjectBinding<Value>> bindings = new HashMap<>();

    /**
     * Get binding for a given device state
     *
     * @param state
     * @return
     */
    protected ObjectBinding<Value> getStateBinding(String state) {
        return bindings.computeIfAbsent(state, stateName ->
                new ObjectBinding<Value>() {
                    @Override
                    protected Value computeValue() {
                        return getDevice().getState(stateName);
                    }
                }
        );
    }

    protected BooleanBinding getStateBooleanBinding(String state) {
        ObjectBinding<Value> b = getStateBinding(state);
        return Bindings.createBooleanBinding(() -> b.get().booleanValue(), b);
    }

    /**
     * Bind writable state change to given observable value
     *
     * @param state
     * @param observable
     */
    protected void bindStateTo(String state, ObservableValue<?> observable) {
        observable.addListener((ChangeListener<Object>) (observable1, oldValue, newValue) -> {
            if (oldValue != newValue) {
                getDevice().setState(state, newValue);
            }
        });
    }

    @Override
    public void notifyDeviceStateChanged(Device device, String name, Value state) {
        if (bindings.containsKey(name)) {
            bindings.get(name).invalidate();
        }
    }
}
