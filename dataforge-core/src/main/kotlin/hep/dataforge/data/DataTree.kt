/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.data

import hep.dataforge.meta.Laminate
import hep.dataforge.meta.Meta
import hep.dataforge.names.Name
import hep.dataforge.nullable
import java.util.stream.Stream
import kotlin.reflect.KClass

/**
 * A tree data structure
 *
 * @author Alexander Nozik
 */
class DataTree<T : Any> constructor(
    name: String,
    override val type: Class<T>,
    meta: Meta,
    parent: DataTree<in T>?
) : DataNode<T> {

    override var name: String = name
        private set

    private var selfMeta: Meta = meta
    var parent: DataTree<in T>? = parent
        private set

    private val nodeMap: MutableMap<String, DataTree<T>> = HashMap()
    private val dataMap: MutableMap<String, Data<T>> = HashMap()


    override val meta: Meta
        get() = parent?.let {
            Laminate(selfMeta, it.meta)
        } ?: selfMeta

    override val isEmpty: Boolean
        get() = dataMap.isEmpty() && nodeMap.isEmpty()


    /**
     * deep copy
     */
    fun copy(name: String? = null): DataTree<T> {
        return DataTree(name ?: this.name, this.type, this.meta, this.parent).also {
            this.nodeMap.forEach { key: String, tree: DataTree<T> -> it.nodeMap[key] = tree.copy() }
            it.dataMap.putAll(this.dataMap)
        }
    }


    fun nodeNames(): Collection<String> {
        return nodeMap.keys
    }

    override fun optData(key: String): Data<out T>? {
        return dataStream(true)
            .filter { it -> it.name == key }
            .findFirst()
            .nullable
    }

    override fun nodeStream(recursive: Boolean): Stream<DataNode<out T>> {
        return if (recursive) {
            nodeStream(Name.EMPTY, Laminate(meta))
        } else {
            nodeMap.values.stream().map { it -> it }
        }
    }

    private fun nodeStream(parentName: Name, parentMeta: Laminate): Stream<DataNode<out T>> {
        return nodeMap.entries.stream().flatMap { nodeEntry ->
            val nodeItself = Stream.of<DataNode<T>>(
                NodeWrapper(nodeEntry.value, parentName.toString(), parentMeta)
            )

            val childName = parentName.plus(nodeEntry.key)
            val childMeta = parentMeta.withFirstLayer(nodeEntry.value.meta)
            val childStream = nodeEntry.value.nodeStream(childName, childMeta)

            Stream.concat<DataNode<out T>>(nodeItself, childStream)
        }
    }


    override fun dataStream(recursive: Boolean): Stream<NamedData<out T>> {
        return dataStream(null, Laminate(selfMeta), recursive)
    }

    private fun dataStream(nodeName: Name?, nodeMeta: Laminate, recursive: Boolean): Stream<NamedData<out T>> {
        val dataStream = dataMap.entries.stream()
            .map<NamedData<out T>> { entry ->
                val dataName = if (nodeName == null) Name.of(entry.key) else nodeName.plus(entry.key)
                NamedData.wrap(dataName, entry.value, nodeMeta)
            }

        if (recursive) {
            // iterating over nodes including node name into dataStream
            val subStream = nodeMap.entries.stream()
                .flatMap { nodeEntry ->
                    val subNodeName = if (nodeName == null) Name.of(nodeEntry.key) else nodeName.plus(nodeEntry.key)
                    nodeEntry.value
                        .dataStream(subNodeName, nodeMeta.withFirstLayer(nodeEntry.value.meta), true)
                }
            return Stream.concat(dataStream, subStream)
        } else {
            return dataStream
        }
    }

    override fun optNode(nodeName: String): DataNode<out T>? {
        return getNode(Name.of(nodeName))
    }

    private fun getNode(nodeName: Name): DataTree<T>? {
        val child = nodeName.first.toString()
        return if (nodeName.length == 1) {
            nodeMap[nodeName.toString()]
        } else {
            this.nodeMap[child]?.getNode(nodeName.cutFirst())
        }
    }

    /**
     * Private editor object
     */
    private val editor = Builder()

    /**
     * Create a deep copy of this tree and edit it
     */
    override fun edit(): Builder {
        return copy().editor
    }

    inner class Builder : DataNodeBuilder<T>(type) {
        override var name: String
            get() = this@DataTree.name
            set(value) {
                this@DataTree.name = value
            }
        override var meta: Meta
            get() = this@DataTree.selfMeta
            set(value) {
                this@DataTree.selfMeta = value
            }


        override val isEmpty: Boolean
            get() = nodeMap.isEmpty() && dataMap.isEmpty()

        @Suppress("UNCHECKED_CAST")
        private fun checkedPutNode(key: String, node: DataNode<*>) {
            if (type.isAssignableFrom(node.type)) {
                if (!nodeMap.containsKey(key)) {
                    nodeMap[key] = node.edit().also { it.name = key }.build() as DataTree<T>
                } else {
                    throw RuntimeException("The node with key $key already exists")
                }
            } else {
                throw RuntimeException("Node does not satisfy class boundary")
            }
        }

        /**
         * Type checked put data method. Throws exception if types are not
         * compatible
         *
         * @param key
         * @param data
         */
        @Suppress("UNCHECKED_CAST")
        private fun checkedPutData(key: String, data: Data<*>, allowReplace: Boolean) {
            if (type.isAssignableFrom(data.type)) {
                if (!dataMap.containsKey(key) || allowReplace) {
                    dataMap[key] = data as Data<T>
                } else {
                    throw RuntimeException("The data with key $key already exists")
                }
            } else {
                throw RuntimeException("Data does not satisfy class boundary")
            }
        }

        /**
         * Private method to add data to the node
         *
         * @param keyName
         * @param data
         */
        private fun putData(keyName: Name, data: Data<out T>, replace: Boolean = false) {
            when {
                keyName.length == 0 -> throw IllegalArgumentException("Name must not be empty")
                keyName.length == 1 -> {
                    val key = keyName.toString()
                    checkedPutData(key, data, replace)
                }
                else -> {
                    val head = keyName.first.toString()
                    (nodeMap[head] ?: DataTree(head, type, Meta.empty(), this@DataTree)
                        .also { nodeMap[head] = it })
                        .editor.putData(keyName.cutFirst(), data, replace)
                }
            }
        }

        override fun putData(key: String, data: Data<out T>, replace: Boolean) {
            putData(Name.of(key), data, replace)
        }

        private fun putNode(keyName: Name, node: DataNode<out T>) {
            when {
                keyName.length == 0 -> throw IllegalArgumentException("Can't put node with empty name")
                keyName.length == 1 -> {
                    val key = keyName.toString()
                    checkedPutNode(key, node)
                }
                else -> {
                    val head = keyName.first.toString()
                    (nodeMap[head] ?: DataTree(head, type, Meta.empty(), this@DataTree)
                        .also { nodeMap[head] = it })
                        .editor.putNode(keyName.cutFirst(), node)
                }
            }
        }

        override fun putNode(key: String, node: DataNode<out T>) {
            putNode(Name.of(key), node)
        }

        override fun removeNode(nodeName: String) {
            val theName = Name.of(nodeName)
            val parentTree: DataTree<*>? = if (theName.length == 1) {
                this@DataTree
            } else {
                getNode(theName.cutLast())
            }
            parentTree?.nodeMap?.remove(theName.last.toString())
        }

        override fun removeData(dataName: String) {
            val theName = Name.of(dataName)
            val parentTree: DataTree<*>? = if (theName.length == 1) {
                this@DataTree
            } else {
                getNode(theName.cutLast())
            }
            parentTree?.dataMap?.remove(theName.last.toString())

        }


        override fun build(): DataTree<T> {
            return this@DataTree
        }

    }


    companion object {

        @JvmStatic
        fun <T : Any> edit(type: Class<T>): DataTree<T>.Builder {
            return DataTree("", type, Meta.empty(), null).editor
        }

        fun <T : Any> edit(type: KClass<T>): DataTree<T>.Builder {
            return edit(type.java)
        }

        /**
         * A general non-restricting tree builder
         *
         * @return
         */
        fun edit(): DataTree<Any>.Builder {
            return edit(Any::class.java)
        }

    }
}

inline fun <reified T : Any> DataTree(block: DataNodeBuilder<T>.() -> Unit): DataTree<T> {
    return DataTree("", T::class.java, Meta.empty(), null).edit().apply(block).build()
}



