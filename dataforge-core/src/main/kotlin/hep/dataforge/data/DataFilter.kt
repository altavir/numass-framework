package hep.dataforge.data

import hep.dataforge.meta.Meta
import hep.dataforge.nullable
import hep.dataforge.values.Value

/**
 * A filter that could select a subset from a DataNode without changing its type.
 */
interface DataFilter {

    /**
     * Perform a selection. Resulting node contains references to the data in the initial node.
     * Node structure and meta is maintained if possible.
     *
     * @param node
     * @param <T>
     * @return
     */
    fun <T : Any> filter(node: DataNode<T>): DataNode<T>

    companion object {
        val IDENTITY: DataFilter = object : DataFilter {
            override fun <T : Any> filter(node: DataNode<T>): DataNode<T> {
                return node
            }
        }

        fun byPattern(pattern: String): DataFilter {
            return object : DataFilter {
                override fun <T : Any> filter(node: DataNode<T>): DataNode<T> {
                    return DataSet.edit(node.type).apply {
                        name = node.name
                        node.dataStream(true)
                            .filter { d -> d.name.matches(pattern.toRegex()) }
                            .forEach { add(it) }

                    }.build()
                }
            }
        }


        fun byMeta(condition: (Meta) -> Boolean): DataFilter {
            return object : DataFilter {
                override fun <T : Any> filter(node: DataNode<T>): DataNode<T> {
                    return DataSet.edit(node.type).apply {
                        name = node.name
                        node.dataStream(true)
                            .filter { d ->
                                condition(d.meta)
                            }
                            .forEach { add(it) }

                    }.build()
                }
            }
        }

        fun byMetaValue(valueName: String, condition: (Value?) -> Boolean): DataFilter =
            byMeta { condition(it.optValue(valueName).nullable) }
    }
}

fun <T : Any> DataNode<T>.filter(filter: DataFilter): DataNode<T> = filter.filter(this)
