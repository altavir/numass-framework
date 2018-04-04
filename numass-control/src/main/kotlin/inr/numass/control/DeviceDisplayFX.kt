package inr.numass.control

import hep.dataforge.connections.Connection
import hep.dataforge.control.connections.Roles
import hep.dataforge.control.devices.Device
import hep.dataforge.control.devices.DeviceListener
import hep.dataforge.control.devices.PortSensor
import hep.dataforge.control.devices.Sensor
import hep.dataforge.exceptions.NameNotFoundException
import hep.dataforge.fx.bindWindow
import hep.dataforge.states.State
import hep.dataforge.states.ValueState
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
annotation class DeviceView(val value: KClass<out DeviceDisplayFX<*>>)

/**
 * Get existing view connection or create a new one
 */
fun Device.getDisplay(): DeviceDisplayFX<*> {
    val type = (this::class.annotations.find { it is DeviceView } as DeviceView?)?.value ?: DefaultDisplay::class
    return optConnection(Roles.VIEW_ROLE, DeviceDisplayFX::class.java).orElseGet {
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
abstract class DeviceDisplayFX<D : Device> : Component(), Connection, DeviceListener {

    private val bindings = HashMap<String, ObjectBinding<*>>()

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

    protected abstract fun buildView(device: D): UIComponent?;

    /**
     * Create a binding for specific state and register it in update listener
     */
    private fun <T : Any> bindState(state: State<T>): ObjectBinding<T> {
        val binding = object : ObjectBinding<T>() {
            override fun computeValue(): T {
                return state.value
            }
        }
        bindings.putIfAbsent(state.name, binding)
        return binding
    }

    fun valueBinding(state: ValueState): ObjectBinding<Value>{
        return bindState(state)
    }

    fun valueBinding(stateName: String): ObjectBinding<Value> {
        val state: ValueState = device.states.filterIsInstance(ValueState::class.java).find { it.name == stateName }
                ?: throw NameNotFoundException("State with name $stateName not found")
        return valueBinding(state)
    }

    fun booleanBinding(stateName: String): BooleanBinding {
        return valueBinding(stateName).booleanBinding { it?.booleanValue() ?: false }
    }

    /**
     * Bind existing boolean property to writable device state

     * @param state
     * @param property
     */
    protected fun bindBooleanToState(state: String, property: BooleanProperty) {
        valueBinding(state).addListener { _, oldValue, newValue ->
            if (isOpen && oldValue != newValue) {
                runLater { property.value = newValue.booleanValue() }
            }
        }
        property.addListener { _, oldValue, newValue ->
            if (isOpen && oldValue != newValue) {
                device.states[state] = newValue
            }
        }
    }

    override fun notifyStateChanged(device: Device, name: String, state: Any) {
        bindings[name]?.invalidate()
    }

    open fun getBoardView(): Parent {
        return HBox().apply {
            alignment = Pos.CENTER_LEFT
            vgrow = Priority.ALWAYS;
            deviceStateIndicator(this@DeviceDisplayFX, Device.INITIALIZED_STATE)
            deviceStateIndicator(this@DeviceDisplayFX, PortSensor.CONNECTED_STATE)
            deviceStateIndicator(this@DeviceDisplayFX, Sensor.MEASURING_STATE)
            deviceStateIndicator(this@DeviceDisplayFX, "storing")
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
class DefaultDisplay : DeviceDisplayFX<Device>() {
    override fun buildView(device: Device): UIComponent? = null
}
