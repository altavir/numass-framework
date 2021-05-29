/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.fx.values

import hep.dataforge.values.Value
import hep.dataforge.values.ValueFactory
import javafx.collections.FXCollections
import javafx.scene.control.ComboBox
import javafx.util.StringConverter
import java.util.*

class ComboBoxValueChooser : ValueChooserBase<ComboBox<Value>>() {

    //    @Override
    //    protected void displayError(String error) {
    //        //TODO ControlsFX decorator here
    //    }

    private fun allowedValues(): Collection<Value> {
        return descriptor?.allowedValues ?: Collections.emptyList();
    }

    override fun buildNode(): ComboBox<Value> {
        val node = ComboBox(FXCollections.observableArrayList(allowedValues()))
        node.maxWidth = java.lang.Double.MAX_VALUE
        node.isEditable = false
        node.selectionModel.select(currentValue())
        node.converter = object : StringConverter<Value>() {
            override fun toString(value: Value?): String {
                return value?.string ?: ""
            }

            override fun fromString(string: String?): Value {
                return ValueFactory.parse(string ?: "")
            }

        }
        this.valueProperty.bind(node.valueProperty())
        return node
    }

    override fun setDisplayValue(value: Value) {
        node.selectionModel.select(value)
    }

}
