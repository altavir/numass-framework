/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.data

import hep.dataforge.meta.Meta
import java.util.stream.Stream


class EmptyDataNode<T: Any>(override val name: String, override val type: Class<T>) : DataNode<T> {

    override val isEmpty = true

    override val meta: Meta = Meta.empty()

    override fun optData(key: String): Data<T>? = null

    override fun dataStream(recursive: Boolean): Stream<NamedData<out T>> = Stream.empty()

    override fun nodeStream(recursive: Boolean): Stream<DataNode<out T>> = Stream.empty()

    override fun optNode(nodeName: String): DataNode<out T>? = null

}
