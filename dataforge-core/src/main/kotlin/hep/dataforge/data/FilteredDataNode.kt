package hep.dataforge.data

import hep.dataforge.meta.Meta
import java.util.stream.Stream

/**
 * Filtered node does not change structure of underlying node, just filters output
 *
 * @param <T>
 */
class FilteredDataNode<T: Any>(private val node: DataNode<T>, private val predicate: (String, Data<out T>) -> Boolean) : DataNode<T> {
    override val name: String = node.name

    override val meta: Meta
        get() = node.meta

    override val isEmpty: Boolean
        get() = dataStream(true).count() == 0L


    override fun optData(key: String): Data<out T>? {
        return node.optData(key)?.let { d ->
            if (predicate(key, d)) {
                d
            } else {
                null
            }
        }
    }

    override fun optNode(nodeName: String): DataNode<out T>? {
        return node.optNode(nodeName)?.let { it -> FilteredDataNode(it, predicate) }
    }

    override fun dataStream(recursive: Boolean): Stream<NamedData<out T>> {
        return node.dataStream(recursive).filter { d -> predicate(d.name, d.cast(type)) }
    }

    override fun nodeStream(recursive: Boolean): Stream<DataNode<out T>> {
        return node.nodeStream(recursive).map { n ->
            n.filter { name, data -> predicate(name, data) }
        }
    }

    override val type: Class<T> = node.type
}
