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
package hep.dataforge.data

import hep.dataforge.context.Context
import hep.dataforge.data.DataFactory.Companion.FILTER_KEY
import hep.dataforge.data.DataFactory.Companion.ITEM_KEY
import hep.dataforge.data.DataFactory.Companion.NODE_KEY
import hep.dataforge.data.DataFactory.Companion.NODE_META_KEY
import hep.dataforge.description.NodeDef
import hep.dataforge.description.NodeDefs
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaNode.DEFAULT_META_NAME

/**
 * A factory for data tree
 *
 * @author Alexander Nozik
 */
@NodeDefs(
        NodeDef(key = NODE_META_KEY, info = "Node meta-data"),
        NodeDef(key = NODE_KEY, info = "Recursively add node to the builder"),
        NodeDef(key = FILTER_KEY, descriptor = "hep.dataforge.data.CustomDataFilter", info = "Filter definition to be applied after node construction is finished"),
        NodeDef(key = ITEM_KEY, descriptor = "method::hep.dataforge.data.DataFactory.buildData", info = "A fixed context-based node with or without actual static data")
)
abstract class DataFactory<T: Any>(private val baseType: Class<T>) : DataLoader<T> {

    override fun build(context: Context, meta: Meta): DataNode<T> {
        //Creating filter
        val filter = CustomDataFilter(meta.getMetaOrEmpty(FILTER_KEY))

        val tree = builder(context, meta).build()
        //Applying filter if needed
        return if (filter.meta.isEmpty) {
            tree
        } else {
            filter.filter(tree)
        }
    }

    /**
     * Return DataTree.Builder after node fill but before filtering. Any custom logic should be applied after it.
     *
     * @param context
     * @param dataConfig
     * @return
     */
    protected fun builder(context: Context, dataConfig: Meta): DataTree<T>.Builder {
        val builder = DataTree.edit(baseType)

        // Apply node name
        if (dataConfig.hasValue(NODE_NAME_KEY)) {
            builder.name = dataConfig.getString(NODE_NAME_KEY)
        }

        // Apply node meta
        if (dataConfig.hasMeta(NODE_META_KEY)) {
            builder.meta = dataConfig.getMeta(NODE_META_KEY)
        }

        // Apply non-specific child nodes
        if (dataConfig.hasMeta(NODE_KEY)) {
            dataConfig.getMetaList(NODE_KEY).forEach { nodeMeta: Meta ->
                //FIXME check types for child nodes
                val node = build(context, nodeMeta)
                builder.add(node)
            }
        }

        //Add custom items
        if (dataConfig.hasMeta(ITEM_KEY)) {
            dataConfig.getMetaList(ITEM_KEY).forEach { itemMeta -> builder.add(buildData(context, itemMeta)) }
        }

        // Apply child nodes specific to this factory
        fill(builder, context, dataConfig)
        return builder
    }

    @NodeDef(key = NODE_META_KEY, info = "Meta for this item")
    private fun buildData(context: Context, itemMeta: Meta): NamedData<T> {
        val name = itemMeta.getString(NODE_NAME_KEY)

        val obj = itemMeta.optValue("path")
                .flatMap { path -> context.provide(path.string, baseType) }
                .orElse(null)

        val meta = itemMeta.optMeta(NODE_META_KEY).orElse(Meta.empty())

        return NamedData.buildStatic(name, obj, meta)
    }

    /**
     * Apply children nodes and data elements to the builder. Inheriting classes
     * can add their own children builders.
     *
     * @param builder
     * @param meta
     */
    protected abstract fun fill(builder: DataNodeBuilder<T>, context: Context, meta: Meta)

    override val name: String = "default"

    companion object {

        const val NODE_META_KEY = DEFAULT_META_NAME
        const val NODE_TYPE_KEY = "type"
        const val NODE_KEY = "node"
        const val ITEM_KEY = "item"
        const val NODE_NAME_KEY = "name"
        const val FILTER_KEY = "filter"
    }
}

class DummyDataFactory<T: Any>(baseType: Class<T>): DataFactory<T>(baseType){
    override fun fill(builder: DataNodeBuilder<T>, context: Context, meta: Meta) {
        // do nothing
    }

}
