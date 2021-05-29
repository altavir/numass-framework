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

import hep.dataforge.description.Described
import hep.dataforge.description.Descriptors
import hep.dataforge.description.NodeDescriptor
import hep.dataforge.io.XMLMetaWriter
import hep.dataforge.nullable
import hep.dataforge.values.Value
import hep.dataforge.values.ValueFactory
import org.jetbrains.annotations.Contract
import java.time.Instant
import java.util.*
import java.util.stream.Collector
import java.util.stream.Collectors
import java.util.stream.Stream

/**
 * A chain of immutable meta. The value is taken from the first meta in list
 * that contains it. The list itself is immutable.
 *
 * @author darksnake
 */
class Laminate(layers: Iterable<Meta?>, descriptor: NodeDescriptor? = null) : Meta(), Described {

    //TODO consider descriptor merging
    override val descriptor: NodeDescriptor = descriptor ?: layers.asSequence()
            .filter { it -> it is Laminate }.map { Laminate::class.java.cast(it) }
            .map { it.descriptor }
            .firstOrNull() ?: NodeDescriptor(Meta.empty())


    /**
     * Create laminate from layers. Deepest first.
     *
     * @param layers
     */
    constructor(vararg layers: Meta?) : this(Arrays.asList<Meta>(*layers)) {}


    val layers: List<Meta> = layers.filterNotNull().flatMap {
        when {
            it.isEmpty -> emptyList()
            it is Laminate -> it.layers // no need to check deeper since laminate is always has only direct members
            else -> listOf(it)
        }
    }

    private val descriptorLayer: Meta? by lazy {
        descriptor?.let { Descriptors.buildDefaultNode(it) }
    }

    override val name: String
        get() = layers.stream().map { it.name }.findFirst().orElse(MetaNode.DEFAULT_META_NAME)


    @Contract(pure = true)
    fun hasDescriptor(): Boolean {
        return !this.descriptor.meta.isEmpty
    }

    /**
     * Attach descriptor to this laminate to use for default values and aliases
     * (ALIASES NOT IMPLEMENTED YET!).
     */
    fun withDescriptor(descriptor: NodeDescriptor): Laminate {
        return Laminate(this.layers, descriptor)
    }

    /**
     * Add primary (first layer)
     *
     * @param layer
     * @return
     */
    fun withFirstLayer(layer: Meta): Laminate {
        return if (layer.isEmpty) {
            this
        } else {
            return Laminate(listOf(layer) + this.layers, this.descriptor)
        }
    }

    /**
     * Add layer to stack
     *
     * @param layers
     * @return
     */
    fun withLayer(vararg layers: Meta): Laminate {
        return Laminate(this.layers + layers, descriptor)
    }

    /**
     * Add layer to the end of laminate
     */
    operator fun plus(layer: Meta): Laminate {
        return withLayer(layer)
    }

    /**
     * Get laminate layers in inverse order
     *
     * @return
     */
    fun layersInverse(): List<Meta> {
        return layers.reversed()
    }

    override fun optMeta(path: String): Optional<Meta> {
        val childLayers = ArrayList<Meta>()
        layers.stream().filter { layer -> layer.hasMeta(path) }.forEach { m ->
            //FIXME child elements are not chained!
            childLayers.add(m.getMeta(path))
        }
        return if (!childLayers.isEmpty()) {
            Optional.of(Laminate(childLayers, descriptor.childrenDescriptors()[path]))
        } else {
            //if node not found, using descriptor layer if it is defined
            descriptorLayer?.optMeta(path) ?: Optional.empty()
        }
    }


    /**
     * Get the first occurrence of meta node with the given name without merging. If not found, uses description.
     *
     * @param path
     * @return
     */
    override fun getMetaList(path: String): List<Meta> {
        val stream: Stream<Meta> = if (descriptorLayer == null) {
            layers.stream()
        } else {
            Stream.concat(layers.stream(), Stream.of<Meta>(descriptorLayer))
        }

        return stream
                .filter { m -> m.hasMeta(path) }
                .map { m -> m.getMetaList(path) }.findFirst()
                .orElse(emptyList())
    }

    /**
     * Node names includes descriptor nodes
     *
     * @return
     */
    override fun getNodeNames(includeHidden: Boolean): Stream<String> {
        return getNodeNames(includeHidden, true)
    }


    fun getNodeNames(includeHidden: Boolean, includeDefaults: Boolean): Stream<String> {
        val names = layers.stream().flatMap { layer -> layer.getNodeNames(includeHidden) }
        return if (includeDefaults && descriptorLayer != null) {
            Stream.concat(names, descriptorLayer!!.getNodeNames(includeHidden)).distinct()
        } else {
            names.distinct()
        }
    }

    /**
     * Value names includes descriptor values,
     *
     * @return
     */
    override fun getValueNames(includeHidden: Boolean): Stream<String> {
        return getValueNames(includeHidden, true)
    }

    fun getValueNames(includeHidden: Boolean, includeDefaults: Boolean): Stream<String> {
        val names = layers.stream().flatMap { layer -> layer.getValueNames(includeHidden) }
        return if (includeDefaults && descriptorLayer != null) {
            Stream.concat(names, descriptorLayer!!.getValueNames(includeHidden)).distinct()
        } else {
            names.distinct()
        }
    }

    override fun optValue(path: String): Optional<Value> {
        //searching layers for value
        for (m in layers) {
            val opt = m.optValue(path)
            if (opt.isPresent) {
                return opt.map { it -> MetaUtils.transformValue(it) }
            }
        }

        // if descriptor layer is definded, searching it for value
        return if (descriptorLayer != null) {
            descriptorLayer!!.optValue(path).map { it -> MetaUtils.transformValue(it) }
        } else Optional.empty()

    }

    override fun isEmpty(): Boolean {
        return this.layers.isEmpty() && (this.descriptorLayer == null || this.descriptorLayer!!.isEmpty)
    }

    /**
     * Combine values in layers using provided collector. Default values from provider and description are ignored
     *
     * @param valueName
     * @param collector
     * @return
     */
    fun collectValue(valueName: String, collector: Collector<Value, *, Value>): Value {
        return layers.stream()
                .filter { layer -> layer.hasValue(valueName) }
                .map { layer -> layer.getValue(valueName) }
                .collect(collector)
    }

    /**
     * Merge nodes using provided collector (good idea to use [MergeRule]).
     *
     * @param nodeName
     * @param collector
     * @param <A>
     * @return
    </A> */
    fun <A> collectNode(nodeName: String, collector: Collector<Meta, A, Meta>): Meta {
        return layers.stream()
                .filter { layer -> layer.hasMeta(nodeName) }
                .map { layer -> layer.getMeta(nodeName) }
                .collect(collector)
    }

    /**
     * Merge node lists grouping nodes by provided classifier and then merging each group independently
     *
     * @param nodeName   the name of node
     * @param classifier grouping function
     * @param collector  used to each group
     * @param <A>        intermediate collector accumulator type
     * @param <K>        classifier key type
     * @return
     */
    fun <A, K> collectNodes(nodeName: String, classifier: (Meta) -> K, collector: Collector<Meta, A, Meta>): Collection<Meta> {
        return layers.stream()
                .filter { layer -> layer.hasMeta(nodeName) }
                .flatMap { layer -> layer.getMetaList(nodeName).stream() }
                .collect(Collectors.groupingBy(classifier, { LinkedHashMap<K, Meta>() }, collector)).values
        //linkedhashmap ensures ordering
    }

    /**
     * Same as above, but uses fixed replace rule to merge meta
     *
     * @param nodeName
     * @param classifier
     * @param <K>
     * @return
     */
    fun <K> collectNodes(nodeName: String, classifier: (Meta) -> K): Collection<Meta> {
        return collectNodes(nodeName, classifier, MergeRule.replace())
    }

    /**
     * Same as above but uses fixed meta value with given key as identity
     *
     * @param nodeName
     * @param key
     * @return
     */
    fun collectNodes(nodeName: String, key: String): Collection<Meta> {
        return collectNodes(nodeName) { getValue(key, ValueFactory.NULL) }
    }

    /**
     * Calculate sum of numeric values with given name. Values in all layers must be numeric.
     *
     * @param valueName
     * @return
     */
    fun sumValue(valueName: String): Double {
        return layers.stream().mapToDouble { layer -> layer.getDouble(valueName, 0.0) }.sum()
    }

    /**
     * Press all of the Laminate layers together creating single immutable meta
     *
     * @return
     */
    fun merge(): Meta {
        return SealedNode(this)
    }

    override fun toString(): String {
        return XMLMetaWriter().writeString(this.merge())
    }

    companion object {
        fun join(meta: Iterable<Meta?>, descriptor: NodeDescriptor? = null): Laminate {
            return Laminate(meta.toList(), descriptor)
        }

        fun join(vararg meta: Meta): Laminate = join(meta.asIterable())
    }
}

fun Iterable<Meta?>.toLaminate(descriptor: NodeDescriptor? = null) = Laminate.join(this, descriptor)

inline operator fun <reified T: Any> Meta.get(name: String) : T? = when(T::class){
    String::class -> optString(name).nullable as T?
    Double::class -> optNumber(name).map { it.toDouble() }.nullable as T?
    Int::class -> optNumber(name).map { it.toInt() }.nullable as T?
    Short::class -> optNumber(name).map { it.toShort() }.nullable as T?
    Long::class -> optNumber(name).map { it.toLong() }.nullable as T?
    Float::class -> optNumber(name).map { it.toFloat() }.nullable as T?
    Boolean::class -> optBoolean(name).nullable as T?
    Instant::class -> optTime(name).nullable as T?
    Meta::class -> optMeta(name).nullable as T?
    else -> error("Type ${T::class} is not recognized as a meta member")
}