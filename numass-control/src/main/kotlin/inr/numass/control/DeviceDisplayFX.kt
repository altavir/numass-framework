package inr.numass.control

import hep.dataforge.connections.Connection
import hep.dataforge.control.connections.Roles
import hep.dataforge.control.devices.Device
import hep.dataforge.control.devices.PortSensor
import hep.dataforge.control.devices.Sensor
import hep.dataforge.exceptions.NameNotFoundException
import hep.dataforge.fx.asBooleanProperty
import hep.dataforge.fx.asProperty
import hep.dataforge.fx.bindWindow
import hep.dataforge.states.ValueState
import hep.dataforge.values.Value
import javafx.beans.property.BooleanProperty
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.geometry.Pos
import javafx.scene.Parent
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import tornadofx.*
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
            connect(it, Roles.VIEW_ROLE);
        }
    }
}


/**
 *
 * An FX View to represent the device
 * Created by darksnake on 14-May-17.
 */
abstract class DeviceDisplayFX<D : Device> : Component(), Connection {

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

    fun valueStateProperty(stateName: String): ObjectProperty<Value> {
        val state: ValueState = device.states.filterIsInstance(ValueState::class.java).find { it.name == stateName }
                ?: throw NameNotFoundException("State with name $stateName not found")
        return state.asProperty()
    }

    fun booleanStateProperty(stateName: String): BooleanProperty {
        val state: ValueState = device.states.filterIsInstance(ValueState::class.java).find { it.name == stateName }
                ?: throw NameNotFoundException("State with name $stateName not found")
        return state.asBooleanProperty()
    }

    open fun getBoardView(): Parent {
        return HBox().apply {
            alignment = Pos.CENTER_LEFT
            vgrow = Priority.ALWAYS;
            deviceStateIndicator(this@DeviceDisplayFX, Device.INITIALIZED_STATE)
            if(device is PortSensor) {
                deviceStateIndicator(this@DeviceDisplayFX, PortSensor.CONNECTED_STATE)
            }
            if(device is Sensor) {
                deviceStateIndicator(this@DeviceDisplayFX, Sensor.MEASURING_STATE)
            }
            if(device.stateNames.contains("storing")) {
                deviceStateIndicator(this@DeviceDisplayFX, "storing")
            }
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

