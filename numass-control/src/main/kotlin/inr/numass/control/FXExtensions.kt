package inr.numass.control

import hep.dataforge.values.Value
import javafx.beans.value.ObservableValue
import javafx.event.EventTarget
import javafx.geometry.Orientation
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import javafx.scene.shape.Circle
import javafx.scene.shape.StrokeType
import tornadofx.*


/**
 * A pin like indicator fx node
 */
class Indicator(radius: Double = 10.0) : Circle(radius, Color.GRAY) {
    private var binding: ObservableValue<*>? = null;

    init {
        stroke = Color.BLACK;
        strokeType = StrokeType.INSIDE;
    }

    /**
     * bind this indicator color to given observable
     */
    fun <T> bind(observable: ObservableValue<T>, transform: (T) -> Paint) {
        if (binding != null) {
            throw RuntimeException("Indicator already bound");
        } else {
            binding = observable;
            fill = transform(observable.value)
            observable.addListener { _, _, value ->
                fill = transform(value);
            }
        }
    }

    /**
     * bind indicator to the boolean value using default colours
     */
    fun bind(booleanValue: ObservableValue<Boolean?>) {
        bind(booleanValue) {
            if (it == null) {
                Color.GRAY
            } else if (it) {
                Color.GREEN;
            } else {
                Color.RED;
            }
        }
    }

    fun unbind() {
        this.binding = null;
        neutralize();
    }

    /**
     * return indicator to the neutral state but do not unbind
     */
    fun neutralize() {
        fill = Color.GRAY;
    }
}

fun EventTarget.indicator(radius: Double = 10.0, op: (Indicator.() -> Unit)? = null) = opcr(this, Indicator(radius), op)

fun Indicator.bind(connection: DeviceViewConnection<*>, state: String, transform: ((Value) -> Paint)? = null) {
    tooltip(state)
    if (transform != null) {
        bind(connection.getStateBinding(state), transform);
    } else {
        bind(connection.getStateBinding(state)) {
            if (it.isNull) {
                Color.GRAY
            } else if (it.booleanValue()) {
                Color.GREEN;
            } else {
                Color.RED;
            }
        }
    }
}

/**
 * State name + indicator
 */
fun EventTarget.deviceStateIndicator(connection: DeviceViewConnection<*>, state: String, transform: ((Value) -> Paint)? = null) {
    if (connection.device.hasState(state)) {
        text("${state.toUpperCase()}: ")
        indicator {
            bind(connection, state, transform);
        }
        separator(Orientation.VERTICAL)
    }
}
