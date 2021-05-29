/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.fx.meta

import hep.dataforge.meta.ConfigChangeListener
import hep.dataforge.meta.Configuration
import hep.dataforge.values.Value
import javafx.beans.InvalidationListener
import javafx.beans.binding.ObjectBinding
import javafx.beans.property.ObjectProperty
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue

/**
 * Configuration value represented as JavaFX property. Using slightly modified
 * JavaFx ObjectPropertyBase code.
 *
 * @author Alexander Nozik
 */
class ConfigValueProperty<T>(val config: Configuration, val valueName: String, val getter: (Value) -> T) : ObjectProperty<T>() {

    private val cfgListener = ConfigChangeListener { name, oldItem, newItem ->
        if (valueName == name.unescaped && oldItem != newItem) {
            cachedValue.invalidate()
        }
    }

    /**
     * current value cached to avoid call of configuration parsing
     */
    private val cachedValue: ObjectBinding<T> = object : ObjectBinding<T>() {
        override fun computeValue(): T {
            return getter.invoke(config.getValue(valueName))
        }
    }

    init {
        //adding a weak observer to configuration
        config.addListener(false, cfgListener)
    }

    override fun getBean(): Configuration? {
        return config
    }

    override fun getName(): String? {
        return valueName
    }

    override fun addListener(listener: InvalidationListener?) {
        cachedValue.addListener(listener)
    }

    override fun addListener(listener: ChangeListener<in T>?) {
        cachedValue.addListener(listener)
    }

    override fun isBound(): Boolean {
        return false
    }

    override fun get(): T {
        return cachedValue.get()
    }

    override fun removeListener(listener: InvalidationListener?) {
        return cachedValue.removeListener(listener)
    }

    override fun removeListener(listener: ChangeListener<in T>?) {
        return cachedValue.removeListener(listener)
    }

    override fun unbind() {
        throw UnsupportedOperationException("Configuration property could not be unbound")
    }

    override fun set(value: T) {
        config.setValue(valueName, value)
        //invalidation not required since it obtained automatically via listener
    }

    override fun bind(observable: ObservableValue<out T>?) {
        throw UnsupportedOperationException("Configuration property could not be bound")
    }


    /**
     * Returns a string representation of this `ObjectPropertyBase`
     * object.
     *
     * @return a string representation of this `ObjectPropertyBase`
     * object.
     */
    override fun toString(): String {
        val bean = bean
        val name = name
        val result = StringBuilder("ConfigurationValueProperty [")
        if (bean != null) {
            result.append("bean: ").append(bean).append(", ")
        }
        if (name != null && name != "") {
            result.append("name: ").append(name).append(", ")
        }
        result.append("value: ").append(get())
        result.append("]")
        return result.toString()
    }

}