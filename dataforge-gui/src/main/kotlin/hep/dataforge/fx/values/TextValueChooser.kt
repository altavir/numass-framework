/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.fx.values

import hep.dataforge.values.Value
import hep.dataforge.values.ValueFactory
import hep.dataforge.values.ValueType
import javafx.beans.value.ObservableValue
import javafx.scene.control.TextField
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import tornadofx.*

class TextValueChooser : ValueChooserBase<TextField>() {

    private val displayText: String
        get() = currentValue().let {
            if (it.isNull) {
                ""
            } else {
                it.string
            }
        }


    override fun buildNode(): TextField {
        val node = TextField()
        val defaultValue = currentValue()
        node.text = displayText
        node.style = String.format("-fx-text-fill: %s;", textColor(defaultValue))

        // commit on enter
        node.setOnKeyPressed { event: KeyEvent ->
            if (event.code == KeyCode.ENTER) {
                commit()
            }
        }
        // restoring value on click outside
        node.focusedProperty().addListener { _: ObservableValue<out Boolean>, oldValue: Boolean, newValue: Boolean ->
            if (oldValue && !newValue) {
                node.text = displayText
            }
        }

        // changing text color while editing
        node.textProperty().onChange { newValue ->
            if(newValue!= null) {
                val value = ValueFactory.parse(newValue)
                if (!validate(value)) {
                    node.style = String.format("-fx-text-fill: %s;", "red")
                } else {
                    node.style = String.format("-fx-text-fill: %s;", textColor(value))
                }
            }
        }

        return node
    }

    private fun commit() {
        val newValue = ValueFactory.parse(node.text)
        if (validate(newValue)) {
            value = newValue
        } else {
            resetValue()
            displayError("Value not allowed")
        }

    }

    private fun textColor(item: Value): String {
        return when (item.type) {
            ValueType.BOOLEAN -> if (item.boolean) {
                "blue"
            } else {
                "salmon"
            }
            ValueType.STRING -> "magenta"
            else -> "black"
        }
    }

    private fun validate(value: Value): Boolean {
        return descriptor?.isValueAllowed(value) ?: true
    }

    //    @Override
    //    protected void displayError(String error) {
    //        //TODO ControlsFX decorator here
    //    }

    override fun setDisplayValue(value: Value) {
        node.text = if (value.isNull) {
            ""
        } else {
            value.string
        }
    }
}
