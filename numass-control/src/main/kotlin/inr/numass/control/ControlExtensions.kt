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

class StringStateDelegate(private val stateName: String?) {
    operator fun getValue(thisRef: Stateful, property: KProperty<*>): String? =
            thisRef.getState(stateName ?: property.name).stringValue()

    operator fun setValue(thisRef: Stateful, property: KProperty<*>, value: String?) {
        thisRef.setState(stateName ?: property.name, value);
    }
}

class BooleanStateDelegate(private val stateName: String?) {
    operator fun getValue(thisRef: Stateful, property: KProperty<*>): Boolean? =
            thisRef.getState(stateName ?: property.name).booleanValue()

    operator fun setValue(thisRef: Stateful, property: KProperty<*>, value: Boolean?) {
        thisRef.setState(stateName ?: property.name, value);
    }
}

class TimeStateDelegate(private val stateName: String?) {
    operator fun getValue(thisRef: Stateful, property: KProperty<*>): Instant? =
            thisRef.getState(stateName ?: property.name).timeValue()

    operator fun setValue(thisRef: Stateful, property: KProperty<*>, value: Instant?) {
        thisRef.setState(stateName ?: property.name, value);
    }
}

class NumberStateDelegate(private val stateName: String?) {
    operator fun getValue(thisRef: Stateful, property: KProperty<*>): Number? =
            thisRef.getState(stateName ?: property.name).numberValue()

    operator fun setValue(thisRef: Stateful, property: KProperty<*>, value: Number?) {
        thisRef.setState(stateName ?: property.name, value);
    }
}

class DoubleStateDelegate(private val stateName: String?) {
    operator fun getValue(thisRef: Stateful, property: KProperty<*>): Double? =
            thisRef.getState(stateName ?: property.name).doubleValue()

    operator fun setValue(thisRef: Stateful, property: KProperty<*>, value: Double?) {
        thisRef.setState(stateName ?: property.name, value);
    }
}

class IntStateDelegate(private val stateName: String?) {
    operator fun getValue(thisRef: Stateful, property: KProperty<*>): Int? =
            thisRef.getState(stateName ?: property.name).intValue()

    operator fun setValue(thisRef: Stateful, property: KProperty<*>, value: Int?) {
        thisRef.setState(stateName ?: property.name, value);
    }
}



/**
 * Delegate states to read/write property
 */
fun Stateful.state(stateName: String? = null) = StateDelegate(stateName)

fun Stateful.stringState(stateName: String? = null) = StringStateDelegate(stateName)
fun Stateful.booleanState(stateName: String? = null) = BooleanStateDelegate(stateName)
fun Stateful.timeState(stateName: String? = null) = TimeStateDelegate(stateName)
fun Stateful.numberState(stateName: String? = null) = NumberStateDelegate(stateName)
fun Stateful.doubleState(stateName: String? = null) = DoubleStateDelegate(stateName)
fun Stateful.intState(stateName: String? = null) = IntStateDelegate(stateName)
