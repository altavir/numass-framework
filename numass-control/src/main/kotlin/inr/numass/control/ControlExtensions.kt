package inr.numass.control

import hep.dataforge.control.devices.Stateful
import hep.dataforge.values.Value
import java.time.Instant
import kotlin.reflect.KProperty

class StateDelegate(private val stateName: String?) {
    operator fun getValue(thisRef: Stateful, property: KProperty<*>): Value? =
            thisRef.getState(stateName ?: property.name)

    operator fun setValue(thisRef: Stateful, property: KProperty<*>, value: Value?) {
        thisRef.setState(stateName ?: property.name, value);
    }
}

class StringStateDelegate(private val valueName: String?) {
    operator fun getValue(thisRef: Stateful, property: KProperty<*>): String? =
            thisRef.getState(valueName ?: property.name).stringValue()

    operator fun setValue(thisRef: Stateful, property: KProperty<*>, value: String?) {
        thisRef.setState(valueName ?: property.name, value);
    }
}

class BooleanStateDelegate(private val valueName: String?) {
    operator fun getValue(thisRef: Stateful, property: KProperty<*>): Boolean? =
            thisRef.getState(valueName ?: property.name).booleanValue()

    operator fun setValue(thisRef: Stateful, property: KProperty<*>, value: Boolean?) {
        thisRef.setState(valueName ?: property.name, value);
    }
}

class TimeStateDelegate(private val valueName: String?) {
    operator fun getValue(thisRef: Stateful, property: KProperty<*>): Instant? =
            thisRef.getState(valueName ?: property.name).timeValue()

    operator fun setValue(thisRef: Stateful, property: KProperty<*>, value: Instant?) {
        thisRef.setState(valueName ?: property.name, value);
    }
}

class NumberStateDelegate(private val valueName: String?) {
    operator fun getValue(thisRef: Stateful, property: KProperty<*>): Number? =
            thisRef.getState(valueName ?: property.name).numberValue()

    operator fun setValue(thisRef: Stateful, property: KProperty<*>, value: Number?) {
        thisRef.setState(valueName ?: property.name, value);
    }
}


/**
 * Delegate states to read/write property
 */
fun Stateful.state(valueName: String? = null) = StateDelegate(valueName)

fun Stateful.stringState(valueName: String? = null) = StringStateDelegate(valueName)
fun Stateful.booleanState(valueName: String? = null) = BooleanStateDelegate(valueName)
fun Stateful.timeState(valueName: String? = null) = TimeStateDelegate(valueName)
fun Stateful.numberState(valueName: String? = null) = NumberStateDelegate(valueName)
