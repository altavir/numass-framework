package inr.numass.control

import hep.dataforge.control.Connection
import hep.dataforge.control.devices.Device
import hep.dataforge.control.devices.DeviceListener
import hep.dataforge.control.devices.PortSensor
import hep.dataforge.control.devices.Sensor
import hep.dataforge.fx.bindWindow
import hep.dataforge.values.Value
import javafx.beans.binding.BooleanBinding
import javafx.beans.binding.ObjectBinding
import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.geometry.Pos
import javafx.scene.Parent
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import tornadofx.*
import java.util.*

/**
 * Created by darksnake on 14-May-17.
 */
abstract class DeviceViewConnection<D : Device> : Component(), Connection, DeviceListener {

    private val bindings = HashMap<String, ObjectBinding<Value>>()

    private val deviceProperty = SimpleObjectProperty<D>(this, "device", null)
    val device: D
        get() {
            val res = deviceProperty.get();
            if (res == null) {
                throw RuntimeException("Not connected!");
            } else {
                return res
            }
        }

    private val viewProperty = SimpleObjectProperty<View>(this, "view", null)
    val view: View
        get() {
            if (viewProperty.get() == null) {
                viewProperty.set(buildView(device))
            }
            return viewProperty.get();
        }

    override fun isOpen(): Boolean {
        return this.deviceProperty.get() != null
    }

    override fun open(obj: Any) {
        if (!isOpen) {
            @Suppress("UNCHECKED_CAST")
            deviceProperty.set(obj as D)
        } else {
            log.warning("Connection already opened")
        }

    }

    override fun close() {
        if (viewProperty.isNotNull.get()) {
            view.close()
        }
        deviceProperty.set(null)
    }

    abstract fun buildView(device: D): View;

    /**
     * Get binding for a given device state

     * @param state
     * *
     * @return
     */
    fun getStateBinding(state: String): ObjectBinding<Value> {
        return bindings.computeIfAbsent(state) { stateName ->
            object : ObjectBinding<Value>() {
                override fun computeValue(): Value {
                    return if (isOpen) {
                        device.getState(stateName)
                    } else {
                        Value.NULL
                    }
                }
            }
        }
    }

    fun getBooleanStateBinding(state: String): BooleanBinding {
        return getStateBinding(state).booleanBinding { it!!.booleanValue() }
    }

    /**
     * Bind existing boolean property to writable device state

     * @param state
     * *
     * @param property
     */
    protected fun bindBooleanToState(state: String, property: BooleanProperty) {
        getStateBinding(state).addListener { observable, oldValue, newValue ->
            if (isOpen && oldValue !== newValue) {
                property.value = newValue.booleanValue()
            }
        }
        property.addListener { observable, oldValue, newValue ->
            if (isOpen && oldValue != newValue) {
                runAsync {
                    device.setState(state, newValue).get().booleanValue();
                } ui {
                    property.set(it)
                }
            }
        }
    }

    override fun notifyDeviceStateChanged(device: Device, name: String, state: Value) {
        bindings[name]?.invalidate()
    }

    open fun getBoardView(): Parent {
        return HBox().apply {
            alignment = Pos.CENTER_LEFT
            vgrow = Priority.ALWAYS;
            deviceStateIndicator(this@DeviceViewConnection, Device.INITIALIZED_STATE)
            deviceStateIndicator(this@DeviceViewConnection, PortSensor.CONNECTED_STATE)
            deviceStateIndicator(this@DeviceViewConnection, Sensor.MEASURING_STATE)
            deviceStateIndicator(this@DeviceViewConnection, "storing")
            pane {
                hgrow = Priority.ALWAYS
            }
            togglebutton("View") {
                isSelected = false
                view.bindWindow(this.selectedProperty())
            }
        }
    }
}
