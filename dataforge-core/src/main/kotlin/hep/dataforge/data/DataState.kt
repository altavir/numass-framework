package hep.dataforge.data

import hep.dataforge.names.Name


typealias DataStateListener = (DataNode<*>, List<Name>) -> Unit

/**
 * A mutable data storage that could be changed during computation via transactions.
 *
 * DataState does not inherit DataNode because DataNode contracts says it should be immutable
 */
interface DataState<T : Any> {
    /**
     * Add state change listener
     * @param if true, the listener will be persistent and associated object will be in memory until removed. Otherwise the reference will be weak
     */
    fun addListener(listener: DataStateListener, strong: Boolean = false)

    /**
     * Remove the listener if it is present
     */
    fun removeListener(listener: DataStateListener)

    /**
     * Current snapshot of data in this state. The produced data node is immutable
     */
    val node: DataNode<T>

    /**
     * Perform a transactional push into this state. For each data item there are 3 cases:
     * 1. Data already present  - it is replaced
     * 2. Data is not present - it is added
     * 3. Data is null - it is removed if present
     *
     */
    fun update(data: Map<Name, Data<T>?>)

}

private sealed class Entry<T>

private class Item<T>(val value: Data<T>) : Entry<T>()

private class Node<T>(val values: Map<Name, Entry<T>>) : Entry<T>(){

}

class TreeDataState<T: Any>: DataState<T>{
    override fun addListener(listener: DataStateListener, strong: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun removeListener(listener: DataStateListener) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override val node: DataNode<T>
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override fun update(data: Map<Name, Data<T>?>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}