package hep.dataforge.fx.table

import hep.dataforge.values.Value
import hep.dataforge.values.ValueMap
import hep.dataforge.values.Values
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty

/**
 * A mutable counterpart of {@link Values}. Does not inherit Values because Values contract states it is immutable
 */
class MutableValues() {
    private val valueMap: MutableMap<String, ObjectProperty<Value>> = LinkedHashMap();

    /**
     * Construct mutable values from regular one
     */
    constructor(values: Values) : this() {
        valueMap.putAll(values.asMap().mapValues { SimpleObjectProperty(it.value) });
    }

    /**
     * Get a JavaFX property corresponding to given key
     */
    fun getProperty(key: String): ObjectProperty<Value> {
        return valueMap.computeIfAbsent(key) { SimpleObjectProperty(Value.NULL) }
    }

    operator fun set(key: String, value: Any) {
        getProperty(key).set(Value.of(value))
    }

    operator fun get(key: String): Value {
        return getProperty(key).get()
    }

    /**
     * Convert this MutableValues to regular Values (data is copied so subsequent changes do not affect resulting object)
     */
    fun toValues(): Values {
        return ValueMap.ofMap(valueMap.mapValues { it.value.get() })
    }
}