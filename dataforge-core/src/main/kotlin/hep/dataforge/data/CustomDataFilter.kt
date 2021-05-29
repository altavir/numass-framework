/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.data

import hep.dataforge.description.NodeDef
import hep.dataforge.description.NodeDefs
import hep.dataforge.description.ValueDef
import hep.dataforge.description.ValueDefs
import hep.dataforge.meta.Meta
import hep.dataforge.meta.SimpleMetaMorph
import hep.dataforge.values.ValueType

import java.util.function.BiPredicate

/**
 * A meta-based data filter
 *
 * @author Alexander Nozik
 */
@NodeDefs(
        NodeDef(key = "include", info = "Define inclusion rule for data and/or dataNode. If not inclusion rule is present, everything is included by default.", descriptor = "method::hep.dataforge.data.CustomDataFilter.applyMeta"),
        NodeDef(key = "exclude", info = "Define exclusion rule for data and/or dataNode. Exclusion rules are allied only to included items.", descriptor = "method::hep.dataforge.data.CustomDataFilter.applyMeta")
)
class CustomDataFilter(meta: Meta) : SimpleMetaMorph(meta), DataFilter {

    private var nodeCondition: BiPredicate<String, DataNode<*>>? = null
    private var dataCondition: BiPredicate<String, Data<*>>? = null

    private fun applyMask(pattern: String): String {
        return pattern.replace(".", "\\.").replace("?", ".").replace("*", ".*?")
    }

    init {
        applyMeta(meta)
    }

    fun acceptNode(nodeName: String, node: DataNode<*>): Boolean {
        return this.nodeCondition == null || this.nodeCondition!!.test(nodeName, node)
    }

    fun acceptData(dataName: String, data: Data<*>): Boolean {
        return this.dataCondition == null || this.dataCondition!!.test(dataName, data)
    }

    override fun <T: Any> filter(node: DataNode<T>): DataNode<T> {
        return DataSet.edit(node.type).apply {
            node.dataStream(true).forEach { d ->
                if (acceptData(d.name, d)) {
                    add(d)
                }
            }
        }.build()

    }

    private fun includeData(dataCondition: BiPredicate<String, Data<*>>) {
        if (this.dataCondition == null) {
            this.dataCondition = dataCondition
        } else {
            this.dataCondition = this.dataCondition!!.or(dataCondition)
        }
    }

    private fun includeData(namePattern: String, type: Class<*>?) {
        val limitingType: Class<*> = type ?: Any::class.java
        val predicate = BiPredicate<String, Data<*>> { name, data -> name.matches(namePattern.toRegex()) && limitingType.isAssignableFrom(data.type) }
        includeData(predicate)
    }

    private fun excludeData(dataCondition: BiPredicate<String, Data<*>>) {
        if (this.dataCondition != null) {
            this.dataCondition = this.dataCondition!!.and(dataCondition.negate())
        }
    }

    private fun excludeData(namePattern: String) {
        excludeData(BiPredicate { name, _ -> name.matches(namePattern.toRegex()) })
    }

    private fun includeNode(namePattern: String, type: Class<*>?) {
        val limitingType: Class<*> = type ?: Any::class.java
        val predicate = BiPredicate<String, DataNode<*>> { name, data -> name.matches(namePattern.toRegex()) && limitingType.isAssignableFrom(data.type) }
        includeNode(predicate)
    }

    private fun includeNode(nodeCondition: BiPredicate<String, DataNode<*>>) {
        if (this.nodeCondition == null) {
            this.nodeCondition = nodeCondition
        } else {
            this.nodeCondition = this.nodeCondition!!.or(nodeCondition)
        }
    }

    private fun excludeNode(nodeCondition: BiPredicate<String, DataNode<*>>) {
        if (this.nodeCondition != null) {
            this.nodeCondition = this.nodeCondition!!.and(nodeCondition.negate())
        }
    }

    private fun excludeNode(namePattern: String) {
        excludeNode(BiPredicate { name, _ -> name.matches(namePattern.toRegex()) })
    }

    private fun getPattern(node: Meta): String {
        return when {
            node.hasValue("mask") -> applyMask(node.getString("mask"))
            node.hasValue("pattern") -> node.getString("pattern")
            else -> ".*"
        }
    }

    @ValueDefs(
            ValueDef(key = "mask", info = "Add rule using glob mask"),
            ValueDef(key = "pattern", info = "Add rule rule using regex pattern"),
            ValueDef(key = "forData", type = arrayOf(ValueType.BOOLEAN), def = "true", info = "Apply this rule to individual data"),
            ValueDef(key = "forNodes", type = arrayOf(ValueType.BOOLEAN), def = "true", info = "Apply this rule to data nodes")
    )
    private fun applyMeta(meta: Meta) {
        if (meta.hasMeta("include")) {
            meta.getMetaList("include").forEach { include ->
                val namePattern = getPattern(include)
                var type: Class<*> = Any::class.java
                if (include.hasValue("type")) {
                    try {
                        type = Class.forName(include.getString("type"))
                    } catch (ex: ClassNotFoundException) {
                        throw RuntimeException("type not found", ex)
                    }

                }
                if (include.getBoolean("forData", true)) {
                    includeData(namePattern, type)
                }
                if (include.getBoolean("forNodes", true)) {
                    includeNode(namePattern, type)
                }
            }
        }

        if (meta.hasMeta("exclude")) {
            meta.getMetaList("exclude").forEach { exclude ->
                val namePattern = getPattern(exclude)

                if (exclude.getBoolean("forData", true)) {
                    excludeData(namePattern)
                }
                if (exclude.getBoolean("forNodes", true)) {
                    excludeNode(namePattern)
                }
            }
        }
    }
}
