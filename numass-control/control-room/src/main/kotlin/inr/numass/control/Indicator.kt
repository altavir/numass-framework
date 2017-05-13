package inr.numass.control

import hep.dataforge.control.connections.DeviceConnection
import hep.dataforge.control.devices.Device
import hep.dataforge.control.devices.DeviceListener
import hep.dataforge.fx.FXObject
import hep.dataforge.values.Value
import javafx.beans.binding.ObjectBinding
import javafx.scene.Node
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import javafx.scene.shape.Circle

/**
 * Lamp-like indicator
 *
 * TODO move to general kotlin FX utils
 * Created by darksnake on 12-May-17.
 */
open class Indicator(val state: String) : FXObject, DeviceConnection<Device>(), DeviceListener {
    private val color = object : ObjectBinding<Paint>() {
        override fun computeValue(): Paint {
            val value = device.getState(state);
            return compute(value);
        }
    }
    private val indicator = Circle();

    init {
        indicator.fillProperty().bind(color);
    }

    protected open fun compute(value: Value): Paint {
        if (value.booleanValue()) {
            return Color.GREEN;
        } else {
            return Color.GRAY;
        }
    }

    override fun getFXNode(): Node {
        return indicator;
    }

    override fun notifyDeviceStateChanged(device: Device?, name: String?, value: Value?) {
        if (name == state) {
            color.invalidate();
        }
    }

    companion object {
        /**
         * Build an indicator
         */
        fun build(device: Device, state: String): Indicator {
            val indicator = Indicator(state);
            device.connect(indicator);
            return indicator;
        }

        /**
         * Build an indicator with the custom color builder
         */
        fun build(device: Device, state: String, func: (Value)-> Paint): Indicator {
            val indicator = object:Indicator(state){
                override fun compute(value: Value): Paint {
                    return func(value);
                }
            };
            device.connect(indicator);
            return indicator;
        }
    }
}
