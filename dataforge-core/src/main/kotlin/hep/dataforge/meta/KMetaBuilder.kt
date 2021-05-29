/*
 * Copyright  2018 Alexander Nozik.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package hep.dataforge.meta

import hep.dataforge.meta.MetaNode.DEFAULT_META_NAME
import hep.dataforge.values.NamedValue

@DslMarker
annotation class MetaDSL

/**
 * Kotlin meta builder extension
 */
@MetaDSL
class KMetaBuilder(name: String = MetaBuilder.DEFAULT_META_NAME) : MetaBuilder(name) {

    operator fun Meta.unaryPlus() {
        putNode(this);
    }

    operator fun String.unaryMinus() {
        removeNode(this);
        removeValue(this);
    }

    operator fun NamedValue.unaryPlus() {
        putValue(this.name, this.anonymous)
    }

    /**
     * Add value
     */
    infix fun String.to(value: Any?) {
        setValue(this, value);
    }

    infix fun String.to(metaBuilder: KMetaBuilder.() -> Unit) {
        setNode(this, KMetaBuilder(this).apply(metaBuilder))
    }

    infix fun String.to(meta: Meta) {
        setNode(this, meta)
    }

//    /**
//     * Short infix notation to put value
//     */
//    infix fun String.v(value: Any) {
//        putValue(this, value);
//    }
//
//    /**
//     * Short infix notation to put node
//     */
//    infix fun String.n(node: Meta) {
//        putNode(this, node)
//    }
//
//    /**
//     * Short infix notation  to put any object that could be converted to meta
//     */
//    infix fun String.n(node: MetaID) {
//        putNode(this, node.toMeta())
//    }

    fun putNode(node: MetaID) {
        putNode(node.toMeta())
    }

    fun putNode(key: String, node: MetaID) {
        putNode(key, node.toMeta())
    }

    /**
     * Attach new node
     */
    @MetaDSL
    fun node(name: String, vararg values: Pair<String, Any>, transform: (KMetaBuilder.() -> Unit)? = null) {
        val node = KMetaBuilder(name);
        values.forEach {
            node.putValue(it.first, it.second)
        }
        transform?.invoke(node)
        attachNode(node)
    }
}

fun buildMeta(name: String = DEFAULT_META_NAME, transform: (KMetaBuilder.() -> Unit)? = null): KMetaBuilder {
    val node = KMetaBuilder(name);
    transform?.invoke(node)
    return node
}

fun buildMeta(name: String, vararg values: Pair<String, Any>, transform: (KMetaBuilder.() -> Unit)? = null): KMetaBuilder {
    val node = KMetaBuilder(name);
    values.forEach {
        node.putValue(it.first, it.second)
    }
    transform?.invoke(node)
    return node
}