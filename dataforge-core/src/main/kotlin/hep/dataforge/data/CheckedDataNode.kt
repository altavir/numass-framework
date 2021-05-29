package hep.dataforge.data

import hep.dataforge.meta.Meta
import org.slf4j.LoggerFactory
import java.util.stream.Stream

/**
 * A wrapper for DataNode that allowes to access speciffically typed content.
 * Created by darksnake on 07-Sep-16.
 */
class CheckedDataNode<T : Any>(private val node: DataNode<*>, override val type: Class<T>) : DataNode<T> {

    override val meta: Meta
        get() = node.meta

    override val isEmpty: Boolean
        get() = dataStream(true).count() == 0L

    override val name: String = node.name

    init {
        //TODO add warning for incompatible types
        if (isEmpty) {
            LoggerFactory.getLogger(javaClass).warn("The checked node is empty")
        }
    }

    override fun optData(key: String): Data<T>? {
        return node.optData(key)?.let { d ->
            if (type.isAssignableFrom(d.type)) {
                d.cast(type)
            } else {
                null
            }
        }
    }

    override fun optNode(nodeName: String): DataNode<T>? {
        return node.optNode(nodeName)?.checked(type)
    }

    override fun dataStream(recursive: Boolean): Stream<NamedData<out T>> {
        return node.dataStream(recursive).filter { d -> type.isAssignableFrom(d.type) }.map { d -> d.cast(type) }
    }

    override fun nodeStream(recursive: Boolean): Stream<DataNode<out T>> {
        return node.nodeStream(recursive).filter { n -> type.isAssignableFrom(n.type) }.map { n -> n.checked(type) }
    }

}
