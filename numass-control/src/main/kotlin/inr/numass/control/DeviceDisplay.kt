package inr.numass.control

import hep.dataforge.control.Connection
import hep.dataforge.control.connections.Roles
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
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance


@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class DeviceView(val value: KClass<out DeviceDisplay<*>>)

/**
 * Get existing view connection or create a new one
 */
fun Device.getDisplay(): DeviceDisplay<*> {
    val type = (this::class.annotations.find { it is DeviceView } as DeviceView?)?.value ?: DefaultDisplay::class
    return optConnection(Roles.VIEW_ROLE, DeviceDisplay::class.java).orElseGet {
        type.createInstance().also {
            connect(it, Roles.VIEW_ROLE, Roles.DEVICE_LISTENER_ROLE);
        }
    }
}


/**
 *
 * An FX View to represent the device
 * Created by darksnake on 14-May-17.
 */
abstract class DeviceDisplay<D : Device> : Component(), Connection, DeviceListener {

    private val bindings = HashMap<String, ObjectBinding<Value>>()

    private val deviceProperty = SimpleObjectProperty<D>(this, "device", null)
    val device: D by deviceProperty

    //    private val viewProperty = SimpleObjectProperty<UIComponent>(this, "view", null)
    val view: UIComponent? by lazy {
        buildView(device)
    }

    override fun isOpen(): Boolean = this.deviceProperty.get() != null

    override fun open(obj: Any) {
        if (!isOpen) {
            @Suppress("UNCHECKED_CAST")
            deviceProperty.set(obj as D)
        } else {
            log.warning("Connection already opened")
        }

    }

    override fun close() {
        if (isOpen) {
            view?.close()
            deviceProperty.set(null)
        }
    }

    abstract fun buildView(device: D): UIComponent?;

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

    fun getBooleanStateBinding(state: String): BooleanBinding =
            getStateBinding(state).booleanBinding { it?.booleanValue() ?: false }

    /**
     * Bind existing boolean property to writable device state

     * @param state
     * *
     * @param property
     */
    protected fun bindBooleanToState(state: String, property: BooleanProperty) {
        getStateBinding(state).addListener { _, oldValue, newValue ->
            if (isOpen && oldValue !== newValue) {
                runLater { property.value = newValue.booleanValue() }
            }
        }
        property.addListener { _, oldValue, newValue ->
            if (isOpen && oldValue != newValue) {
                runAsync {
                    if (!device.isInitialized) {
                        device.init()
                    }
                    device.setState(state, newValue)
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
            deviceStateIndicator(this@DeviceDisplay, Device.INITIALIZED_STATE)
            deviceStateIndicator(this@DeviceDisplay, PortSensor.CONNECTED_STATE)
            deviceStateIndicator(this@DeviceDisplay, Sensor.MEASURING_STATE)
            deviceStateIndicator(this@DeviceDisplay, "storing")
            pane {
                hgrow = Priority.ALWAYS
            }
            togglebutton("View") {
                isSelected = false
                if (view == null) {
                    isDisable = true
                }
                view?.bindWindow(selectedProperty())
            }
        }
    }
}


/**
 * Default display shows only board pane and nothing else
 */
class DefaultDisplay() : DeviceDisplay<Device>() {
    override fun buildView(device: Device): UIComponent? = null
}

