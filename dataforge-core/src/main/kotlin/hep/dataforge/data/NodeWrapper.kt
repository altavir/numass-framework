package hep.dataforge.data

import hep.dataforge.meta.Laminate
import hep.dataforge.meta.Meta
import hep.dataforge.names.Name
import java.util.stream.Stream

/**
 * Data node wrapper to add parent name and meta to existing node
 * Created by darksnake on 14-Aug-16.
 */
class NodeWrapper<T : Any>(private val node: DataNode<T>, parentName: String, parentMeta: Meta) : DataNode<T> {
    override val meta: Laminate
    override val name: String

    override val isEmpty: Boolean
        get() = node.isEmpty

    init {
        if (parentMeta is Laminate) {
            this.meta = parentMeta.withFirstLayer(node.meta)
        } else {
            this.meta = Laminate(node.meta, parentMeta)
        }
        this.name = if (parentName.isEmpty()) node.name else Name.joinString(parentName, node.name)
    }

    override fun optData(key: String): Data<out T>? {
        return node.optData(key)
    }

    override fun optNode(nodeName: String): DataNode<out T>? {
        return node.optNode(nodeName)
    }

    override fun dataStream(recursive: Boolean): Stream<NamedData<out T>> {
        return node.dataStream(recursive)
    }

    override fun nodeStream(recursive: Boolean): Stream<DataNode<out T>> {
        return node.nodeStream(recursive)
    }

    override val type: Class<T>
        get() {
            return node.type
        }

}
