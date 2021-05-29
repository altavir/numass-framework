/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.control.connections

import hep.dataforge.connections.Connection
import hep.dataforge.context.Context
import hep.dataforge.context.ContextAware
import hep.dataforge.context.launch
import hep.dataforge.storage.tables.MutableTableLoader
import hep.dataforge.tables.ValuesListener
import hep.dataforge.values.Values

/**
 *
 * @author Alexander Nozik
 */
class LoaderConnection(private val loader: MutableTableLoader) : Connection, ValuesListener, ContextAware {

    override val context: Context
        get() = loader.context

    override fun accept(point: Values) {
        launch {
            loader.append(point)
        }
    }

    override fun isOpen(): Boolean {
        return true
    }

    override fun open(`object`: Any) {

    }

    @Throws(Exception::class)
    override fun close() {
        loader.close()
    }

}
