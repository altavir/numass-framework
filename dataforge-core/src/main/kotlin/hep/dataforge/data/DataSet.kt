/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.data

import hep.dataforge.exceptions.AnonymousNotAlowedException
import hep.dataforge.meta.Meta
import hep.dataforge.names.Name
import hep.dataforge.nullable
import org.slf4j.LoggerFactory
import java.util.*
import java.util.stream.Collectors
import java.util.stream.Stream
import kotlin.reflect.KClass

/**
 * A simple static representation of DataNode
 *
 * @param <T>
 * @author Alexander Nozik
 */
class DataSet<T: Any> internal constructor(
        override val name: String,
        override val type: Class<T>,
        private val dataMap: Map<String, Data<out T>>,
        override val meta: Meta = Meta.empty()
) : DataNode<T> {

    override val isEmpty: Boolean
        get() = dataMap.isEmpty()

    override fun dataStream(recursive: Boolean): Stream<NamedData<out T>> {
        return dataMap.entries.stream()
                .filter { it -> recursive || !it.key.contains(".") }
                .map { entry -> NamedData.wrap(entry.key, entry.value, meta) }
    }

    /**
     * Not very effective for flat data set
     * @return
     */
    override fun nodeStream(recursive: Boolean): Stream<DataNode<out T>> {
        if (recursive) {
            throw Error("Not implemented")
        }
        return dataStream()
                .map { data -> Name.of(data.name) } // converting strings to Names
                .filter { name -> name.length > 1 } //selecting only composite names
                .map { name -> name.first.toString() }
                .distinct()
                .map { DataSet(it, type, subMap("$it."), meta) }
    }

    private fun subMap(prefix: String): Map<String, Data<out T>> {
        return dataMap.entries.stream()
                .filter { entry -> entry.key.startsWith(prefix) }
                .collect(Collectors.toMap({ it.key.substring(prefix.length) }, { it.value }))
    }

    override fun optData(key: String): Data<out T>? {
        return Optional.ofNullable(dataMap[key])
                .map { it -> NamedData.wrap(key, it, meta) }
                .nullable
    }


    override fun optNode(nodeName: String): DataNode<out T>? {
        val builder = DataSetBuilder(type).apply {
            name = nodeName
            this.meta = this@DataSet.meta

            val prefix = "$nodeName."

            dataStream()
                    .filter { data -> data.name.startsWith(prefix) }
                    .forEach { data ->
                        val dataName = Name.of(data.name).cutFirst().toString()
                        set(dataName, data.anonymize())
                    }

        }
        return if (!builder.isEmpty) {
            builder.build()
        } else {
            null
        }
    }

    companion object {

        /**
         * The builder bound by type of data
         *
         * @param <T>
         * @param type
         * @return
         */
        @JvmStatic
        fun <T: Any> edit(type: Class<T>): DataSetBuilder<T> {
            return DataSetBuilder(type)
        }

        fun <T: Any> edit(type: KClass<T>): DataSetBuilder<T> {
            return DataSetBuilder(type.java)
        }


        /**
         * Unbound builder
         *
         * @return
         */
        @JvmStatic
        fun edit(): DataSetBuilder<Any> {
            return DataSetBuilder(Any::class.java)
        }
    }

}

class DataSetBuilder<T: Any> internal constructor(type: Class<T>) : DataNodeBuilder<T>(type) {
    private val dataMap = LinkedHashMap<String, Data<out T>>()
    override var name = ""

    override val isEmpty: Boolean
        get() = dataMap.isEmpty()

    override var meta: Meta = Meta.empty()

    override fun putData(key: String, data: Data<out T>, replace: Boolean) {
        if (key.isEmpty()) {
            throw AnonymousNotAlowedException()
        }
        if (type.isAssignableFrom(data.type)) {
            if (replace || !dataMap.containsKey(key)) {
                dataMap[key] = data
            } else {
                throw RuntimeException("The data with key $key already exists")
            }
        } else {
            throw RuntimeException("Data does not satisfy class boundary")
        }
    }

    override fun putNode(key: String, node: DataNode<out T>) {
        if (!node.meta.isEmpty) {
            LoggerFactory.getLogger(javaClass).warn("Trying to add node with meta to flat DataNode. " + "Node meta could be lost. Consider using DataTree instead.")
        }
        //PENDING rewrap data including meta?
        node.dataStream().forEach { data -> set("$key.${data.name}", data.anonymize()) }
    }




    override fun removeNode(nodeName: String) {
        this.dataMap.entries.removeIf { entry -> entry.key.startsWith(nodeName) }
    }

    override fun removeData(dataName: String) {
        this.dataMap.remove(dataName)
    }

    override fun build(): DataSet<T> {
        return DataSet(name, type, dataMap, meta)
    }

}
