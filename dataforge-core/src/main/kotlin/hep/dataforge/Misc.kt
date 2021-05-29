package hep.dataforge

import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaNode
import hep.dataforge.names.Name
import java.util.stream.Collectors
import java.util.stream.Stream

fun <T> Stream<T>.toList(): List<T> {
    return collect(Collectors.toList())
}

fun String?.asName(): Name {
    return Name.of(this)
}

fun <T : MetaNode<T>> MetaNode<T>.findNode(path: String, predicate: Meta.() -> Boolean): MetaNode<T>? {
    return this.getMetaList(path).firstOrNull(predicate)
}