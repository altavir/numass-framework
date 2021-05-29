/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.data

import hep.dataforge.Named
import hep.dataforge.context.Context
import hep.dataforge.exceptions.NameNotFoundException
import hep.dataforge.goals.GoalGroup
import hep.dataforge.meta.KMetaBuilder
import hep.dataforge.meta.Meta
import hep.dataforge.meta.Metoid
import hep.dataforge.meta.buildMeta
import hep.dataforge.nullable
import hep.dataforge.providers.Provider
import hep.dataforge.providers.Provides
import hep.dataforge.toList
import java.util.concurrent.Executor
import java.util.function.BiConsumer
import java.util.function.Consumer
import java.util.stream.Stream
import kotlin.streams.asSequence

/**
 * A universal data container
 *
 * @author Alexander Nozik
 */
interface DataNode<T : Any> : Iterable<NamedData<out T>>, Named, Metoid, Provider {

    /**
     * Get default data fragment. Access first data element in this node if it
     * is not present. Useful for single data nodes.
     *
     * @return
     */
    val data: Data<out T>?
        get() = optData(DEFAULT_DATA_FRAGMENT_NAME) ?: dataStream().findFirst().nullable

    /**
     * Shows if there is no data in this node
     *
     * @return
     */
    val isEmpty: Boolean

    val size: Long
        get() = count(true)

    /**
     * Get Data with given Name or null if name not present
     *
     * @param key
     * @return
     */
    @Provides(DATA_TARGET)
    fun optData(key: String): Data<out T>?

    fun <R : Any> getCheckedData(dataName: String, type: Class<R>): Data<R> {
        val data = getData(dataName)
        return if (type.isAssignableFrom(data.type)) {
            data.cast(type)
        } else {
            throw RuntimeException(String.format("Type check failed: expected %s but found %s",
                type.name,
                data.type.name))
        }
    }

    /**
     * Compute specific Data. Blocking operation
     *
     * @param key
     * @return
     */
    operator fun get(key: String): T {
        return getData(key).get()
    }

    fun getData(key: String): Data<out T> {
        return optData(key) ?: throw NameNotFoundException(key)
    }

    /**
     * Get descendant node in case of tree structure. In case of flat structure
     * returns node composed of all Data elements with names that begin with
     * `<nodename>.`. Child node inherits meta from parent node. In case
     * both nodes have meta, it is merged.
     *
     * @param nodeName
     * @return
     */
    @Provides(NODE_TARGET)
    fun optNode(nodeName: String): DataNode<out T>?

    fun getNode(nodeName: String): DataNode<out T> {
        return optNode(nodeName) ?: throw NameNotFoundException(nodeName)
    }

    /**
     * Get the node assuming it have specific type with type check
     *
     * @param nodeName
     * @param type
     * @param <R>
     * @return
     */
    fun <R : Any> getCheckedNode(nodeName: String, type: Class<R>): DataNode<out R> {
        val node: DataNode<out T> = if (nodeName.isEmpty()) {
            this
        } else {
            getNode(nodeName)
        }

        return node.checked(type)
    }

    /**
     * Named dataStream of data elements including subnodes if they are present.
     * Meta of each data is supposed to be laminate containing node meta.
     *
     * @return
     */
    fun dataStream(recursive: Boolean): Stream<NamedData<out T>>

    fun dataStream(): Stream<NamedData<out T>> {
        return dataStream(true)
    }

    /**
     * Iterate other all data pieces with given type with type check
     *
     * @param type
     * @param consumer
     */
    fun <R : Any> visit(type: Class<R>, consumer: (NamedData<R>) -> Unit) {
        dataStream().asSequence().filter { d -> type.isAssignableFrom(d.type) }
            .forEach { d -> consumer(d.cast(type)) }
    }

    /**
     * A stream of subnodes. Each node has composite name and Laminate meta including all higher nodes information
     *
     * @param recursive if true then recursive node stream is returned, otherwise only upper level children are used
     * @return
     */
    fun nodeStream(recursive: Boolean = true): Stream<DataNode<out T>>

    /**
     * Get border type for this DataNode
     *
     * @return
     */
    val type: Class<T>

    /**
     * The current number of data pieces in this node including subnodes
     *
     * @return
     */
    fun count(recursive: Boolean): Long {
        return dataStream(recursive).count()
    }

    /**
     * Force start data goals for all data and wait for completion
     */
    fun computeAll() {
        nodeGoal().get()
    }

    /**
     * Computation control for data
     *
     * @return
     */
    fun nodeGoal(): GoalGroup {
        return GoalGroup(this.dataStream().map { it.goal }.toList())
    }

    /**
     * Handle result when the node is evaluated. Does not trigger node evaluation. Ignores exceptional completion
     *
     * @param consumer
     */
    fun handle(consumer: Consumer<DataNode<in T>>) {
        nodeGoal().onComplete { _, _ -> consumer.accept(this@DataNode) }
    }

    /**
     * Same as above but with custom executor
     *
     * @param executor
     * @param consumer
     */
    fun handle(executor: Executor, consumer: (DataNode<T>) -> Unit) {
        nodeGoal().onComplete(executor, BiConsumer { _, _ -> consumer(this@DataNode) })
    }

    /**
     * Return a type checked node containing this one
     *
     * @param checkType
     * @param <R>
     * @return
     */
    @Suppress("UNCHECKED_CAST")
    fun <R : Any> checked(checkType: Class<R>): DataNode<R> {
        return if (checkType.isAssignableFrom(this.type)) {
            this as DataNode<R>
        } else {
            CheckedDataNode(this, checkType)
        }
    }

    fun filter(predicate: (String, Data<out T>) -> Boolean): DataNode<T> {
        return FilteredDataNode(this, predicate)
    }

    override operator fun iterator(): Iterator<NamedData<T>> {
        return dataStream().map { it -> it.cast(type) }.iterator()
    }

    /**
     * Create a deep copy of this node and edit it
     */
    fun edit(): DataNodeBuilder<T> {
        return DataTree.edit(this.type).also {
            it.name = this.name
            it.meta = this.meta
            this.dataStream().forEach { d -> it.add(d) }
        }
    }

    companion object {

        const val DATA_TARGET = "data"
        const val NODE_TARGET = "node"
        const val DEFAULT_DATA_FRAGMENT_NAME = "@default"

        fun <T : Any> empty(name: String, type: Class<T>): DataNode<T> {
            return EmptyDataNode(name, type)
        }

        fun empty(): DataNode<Any> {
            return EmptyDataNode("", Any::class.java)
        }

        /**
         * A data node wrapping single data
         *
         * @param <T>
         * @param dataName
         * @param data
         * @param nodeMeta
         * @return
         */
        fun <T : Any> of(dataName: String, data: Data<T>, nodeMeta: Meta): DataNode<T> {
            return DataSet.edit(data.type).apply {
                name = dataName
                meta = nodeMeta
                putData(dataName, data)
            }.build()
        }

        inline fun <reified T : Any> build(noinline transform: DataNodeBuilder<T>.() -> Unit): DataNode<T> {
            return DataTree.edit(T::class).apply(transform).build()
        }
    }

}

abstract class DataNodeBuilder<T : Any>(val type: Class<T>) {

    abstract var name: String

    abstract var meta: Meta

    abstract val isEmpty: Boolean

    abstract fun putData(key: String, data: Data<out T>, replace: Boolean = false)

    operator fun set(key: String, data: Data<out T>) {
        putData(key, data, false)
    }

    fun putData(key: String, data: T, meta: Meta) {
        return putData(key, Data.buildStatic(data, meta))
    }

    operator fun set(key: String, node: DataNode<out T>) {
        putNode(key, node)
    }

    abstract fun putNode(key: String, node: DataNode<out T>)

    abstract fun removeNode(nodeName: String)

    abstract fun removeData(dataName: String)

    fun add(node: DataNode<out T>) {
        set(node.name, node)
    }

    operator fun DataNode<out T>.unaryPlus() {
        set(this.name, this)
    }

    fun add(data: NamedData<out T>) {
        putData(data.name, data.anonymize())
    }

    operator fun NamedData<out T>.unaryPlus() {
        putData(this.name, this.anonymize())
    }

    /**
     * Update this node mirroring the argument
     */
    fun update(node: DataNode<out T>) {
        node.dataStream(true).forEach { this.add(it) }
    }

    fun putAll(dataCollection: Collection<NamedData<out T>>) {
        dataCollection.forEach { +it }
    }

    fun putAll(map: Map<String, Data<out T>>) {
        map.forEach { key, data -> this[key] = data }
    }

    @JvmOverloads
    fun putStatic(key: String, staticData: T, meta: Meta = Meta.empty()) {
        if (!type.isInstance(staticData)) {
            throw IllegalArgumentException("The data mast be instance of " + type.name)
        }
        +NamedData.buildStatic(key, staticData, meta)
    }

    abstract fun build(): DataNode<T>
}

fun DataNodeBuilder<Any>.load(context: Context, meta: Meta) {
    val newNode = SmartDataLoader().build(context, meta)
    update(newNode)
}

fun DataNodeBuilder<Any>.load(context: Context, metaBuilder: KMetaBuilder.() -> Unit) =
    load(context, buildMeta(transform = metaBuilder))