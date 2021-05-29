/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.workspace.tasks

import hep.dataforge.Named
import hep.dataforge.context.Context
import hep.dataforge.context.ContextAware
import hep.dataforge.data.DataNode
import hep.dataforge.data.DataNodeBuilder
import hep.dataforge.data.NamedData
import hep.dataforge.exceptions.AnonymousNotAlowedException
import hep.dataforge.exceptions.NameNotFoundException
import hep.dataforge.meta.*
import hep.dataforge.meta.MetaNode.DEFAULT_META_NAME
import hep.dataforge.utils.GenericBuilder
import hep.dataforge.utils.NamingUtils
import hep.dataforge.values.Value
import hep.dataforge.values.ValueProvider
import hep.dataforge.workspace.Workspace
import org.slf4j.LoggerFactory
import java.util.*
import java.util.stream.Stream

/**
 * The model for task execution. Is computed without actual task invocation.
 *
 * @param workspace The workspace this model belongs to
 * @param name The unique name of the task
 * @param taskMeta
 * @param dependencies
 * @author Alexander Nozik
 */
class TaskModel private constructor(
        val workspace: Workspace,
        override var name: String,
        taskMeta: Meta,
        dependencies: Set<Dependency> = emptySet()) : Named, Metoid, ValueProvider, MetaID, ContextAware {

    /**
     * Meta for this specific task
     */
    override var meta: Meta = taskMeta
        private set

    /**
     * A set of dependencies
     */
    private val _dependencies: MutableSet<Dependency> = LinkedHashSet(dependencies)

    /**
     * An ordered collection of dependencies
     *
     * @return
     */
    val dependencies: Collection<Dependency> = _dependencies

    override val context: Context = workspace.context

    /**
     * Create a copy of this model an delegate it to builder
     *
     * @return
     */
    fun builder(): Builder {
        return Builder(workspace, name, meta)
    }

    /**
     * Shallow copy
     *
     * @return
     */
    fun copy(): TaskModel {
        return TaskModel(workspace, name, meta, _dependencies)
    }

    override fun toMeta(): Meta {
        val id = MetaBuilder("task")
                .setNode(context.toMeta())
                .setValue("name", name)
                .setNode(DEFAULT_META_NAME, meta)

        val depNode = MetaBuilder("dependencies")

        dependencies.forEach { dependency -> depNode.putNode(dependency.toMeta()) }
        id.putNode(depNode)

        return id
    }

    /**
     * Convenience method. Equals `meta().getValue(path)`
     *
     * @param path
     * @return
     */
    override fun optValue(path: String): Optional<Value> {
        return meta.optValue(path)
    }

    /**
     * Convenience method. Equals `meta().hasValue(path)`
     *
     * @param path
     * @return
     */
    override fun hasValue(path: String): Boolean {
        return meta.hasValue(path)
    }


    /**
     * Generate an unique ID for output data for caching
     */
    fun getID(data: NamedData<*>): Meta{
        //TODO calculate dependencies
        return data.id
    }

//    /**
//     * Find all data that is used to construct data with given name
//     */
//    fun resolveData(name: Name): List<NamedData<*>>{
//        return dependencies.flatMap {
//
//        }
//    }


    /**
     * A rule to add calculate dependency data from workspace
     */
    interface Dependency : MetaID {

        /**
         * Apply data to data dree. Could throw exceptions caused by either
         * calculation or placement procedures.
         *
         * @param tree
         * @param workspace
         */
        fun apply(tree: DataNodeBuilder<Any>, workspace: Workspace)
    }

    /**
     * Data dependency
     */
    internal class DataDependency : Dependency {

        /**
         * The gathering function for data
         */
        private val gatherer: (Workspace) -> Stream<out NamedData<out Any>>
        @Transient
        private val id: Meta

        /**
         * The rule to andThen from workspace data name to DataTree path
         */
        private val pathTransformationRule: (String) -> String

        //        public DataDependency(Function<Workspace, Stream<NamedData<?>>> gatherer, UnaryOperator<String> rule) {
        //            this.gatherer = gatherer;
        //            this.pathTransformationRule = rule;
        //        }

        constructor(mask: String, rule: (String) -> String) {
            this.gatherer = { workspace ->
                workspace.data.dataStream()
                        .filter { data ->
                            NamingUtils.wildcardMatch(mask, data.name)
                        }
            }
            this.pathTransformationRule = rule
            id = MetaBuilder("data").putValue("mask", mask)
        }

        /**
         * Data dependency w
         *
         * @param type
         * @param mask
         * @param rule
         */
        constructor(type: Class<*>, mask: String, rule: (String) -> String) {
            this.gatherer = { workspace ->
                workspace.data.dataStream().filter { data ->
                    NamingUtils.wildcardMatch(mask, data.name) && type.isAssignableFrom(data.type)
                }
            }
            this.pathTransformationRule = rule
            id = MetaBuilder("data").putValue("mask", mask).putValue("type", type.name)
        }

        /**
         * Place data
         *
         * @param tree
         * @param workspace
         */
        override fun apply(tree: DataNodeBuilder<Any>, workspace: Workspace) {
            gatherer(workspace).forEach { data ->
                tree.putData(pathTransformationRule(data.name), data.anonymize())
            }
        }

        override fun toMeta(): Meta {
            return id
        }
    }

    internal class DataNodeDependency<T : Any>(private val type: Class<T>, private val sourceNodeName: String, private val targetNodeName: String) : Dependency {

        override fun apply(tree: DataNodeBuilder<Any>, workspace: Workspace) {
            tree.putNode(targetNodeName, workspace.data.getCheckedNode<T>(sourceNodeName, type))
        }

        override fun toMeta(): Meta {
            return MetaBuilder("dataNode")
                    .putValue("source", sourceNodeName)
                    .putValue("target", targetNodeName)
                    .putValue("type", type.name)
        }
    }

    /**
     * Task dependency
     * @param taskModel The model of task
     */
    internal class TaskDependency(var taskModel: TaskModel, val key: String) : Dependency {
        //TODO make meta configurable

        /**
         * Attach result of task execution to the data tree
         *
         * @param tree
         * @param workspace
         */
        override fun apply(tree: DataNodeBuilder<Any>, workspace: Workspace) {
            val result = workspace.runTask(taskModel)
            if (key.isEmpty()) {
                if (!result.meta.isEmpty) {
                    if (tree.meta.isEmpty()) {
                        tree.meta = result.meta
                    } else {
                        LoggerFactory.getLogger(javaClass).error("Root node meta already exists.")
                    }
                }
                result.dataStream().forEach { tree.add(it) }
            } else {
                tree.putNode(key, result)
            }
        }

        override fun toMeta(): Meta {
            return taskModel.toMeta()
        }

    }

    /**
     * A builder for immutable model
     */
    class Builder(workspace: Workspace, taskName: String, taskMeta: Meta) : GenericBuilder<TaskModel, Builder> {
        private val model: TaskModel = TaskModel(workspace, taskName, Meta.empty())
        private var taskMeta = taskMeta.builder

        val workspace: Workspace
            get() = model.workspace

        val name: String?
            get() = this.model.name


        //        constructor(model: TaskModel) {
//            this.model = model.copy()
//            this.taskMeta = model.meta.builder
//        }

        override fun self(): Builder {
            return this
        }

        override fun build(): TaskModel {
            model.meta = taskMeta.build()
            return model
        }

        //        public Meta getMeta() {
        //            return this.model.getMeta();
        //        }

        /**
         * Apply meta transformation to model meta
         *
         * @param transform
         * @return
         */
        fun configure(transform: MetaBuilder.() -> Unit): Builder {
            transform(taskMeta)
            return self()
        }

        /**
         * replace model meta
         *
         * @param meta
         * @return
         */
        fun configure(meta: Meta): Builder {
            this.taskMeta = meta.builder
            return self()
        }

        /**
         * Rename model
         *
         * @param name
         * @return
         */
        fun rename(name: String): Builder {
            if (name.isEmpty()) {
                throw AnonymousNotAlowedException()
            } else {
                model.name = name
                return self()
            }
        }

        /**
         * Add dependency on Model with given task
         *
         * @param dep
         * @param `as`
         */
        @JvmOverloads
        fun dependsOn(dep: TaskModel, key: String = dep.name): Builder {
            model._dependencies.add(TaskDependency(dep, key))
            return self()
        }

        /**
         * dependsOn(new TaskModel(taskName, taskMeta), as);
         *
         * @param taskName
         * @param taskMeta
         * @param `as`
         */
        @JvmOverloads
        fun dependsOn(taskName: String, taskMeta: Meta, key: String = ""): Builder {
            try {
                return dependsOn(model.workspace.getTask(taskName).build(model.workspace, taskMeta), key)
            } catch (ex: NameNotFoundException) {
                throw RuntimeException("Task with name " + ex.name + " not found", ex)
            }

        }

        @JvmOverloads
        fun dependsOn(task: Task<*>, taskMeta: Meta, `as`: String = ""): Builder {
            return dependsOn(task.build(model.workspace, taskMeta), `as`)
        }

        /**
         * Add data dependency rule using data path mask and name transformation
         * rule.
         *
         *
         * Name change rule should be "pure" to avoid runtime model changes
         *
         * @param mask
         * @param rule
         */
        @JvmOverloads
        fun data(mask: String, rule: (String) -> String = { it }): Builder {
            model._dependencies.add(DataDependency(mask, rule))
            return self()
        }

        /**
         * Type checked data dependency
         *
         * @param type
         * @param mask
         * @param rule
         * @return
         */
        fun data(type: Class<*>, mask: String, rule: (String) -> String): Builder {
            model._dependencies.add(DataDependency(type, mask, rule))
            return self()
        }

        /**
         * data(mask, `str -> as`);
         *
         * @param mask
         * @param `as`
         */
        fun data(mask: String, key: String): Builder {
            //FIXME make smart name transformation here
            return data(mask) { key }
        }

        /**
         * Add all data in the workspace as a dependency
         *
         * @return
         */
        fun allData(): Builder {
            model._dependencies.add(DataDependency("*") { it })
            return self()
        }

        /**
         * Add a dependency on a type checked node
         *
         * @param type
         * @param sourceNodeName
         * @param targetNodeName
         * @return
         */
        fun dataNode(type: Class<*>, sourceNodeName: String, targetNodeName: String): Builder {
            model._dependencies.add(DataNodeDependency(type, sourceNodeName, targetNodeName))
            return this
        }

        /**
         * Source and target node have the same name
         *
         * @param type
         * @param nodeName
         * @return
         */
        fun dataNode(type: Class<*>, nodeName: String): Builder {
            model._dependencies.add(DataNodeDependency(type, nodeName, nodeName))
            return this
        }

    }
    /**
     * dependsOn(model, model.getName());
     *
     * @param dep
     */
    /**
     * dependsOn(new TaskModel(workspace, taskName, taskMeta))
     *
     * @param name
     * @param taskMeta
     */
    /**
     * data(mask, UnaryOperator.identity());
     *
     * @param mask
     */

    companion object {

        /**
         * Create an empty model builder
         *
         * @param workspace
         * @param taskName
         * @param taskMeta
         * @return
         */
        fun builder(workspace: Workspace, taskName: String, taskMeta: Meta): TaskModel.Builder {
            return TaskModel.Builder(workspace, taskName, taskMeta)
        }

        /**
         * Generate id for NamedData
         */
        val NamedData<*>.id: Meta
            get() = meta.builder.apply {
                "name" to name
                "type" to this@id.type
            }

        /**
         * Generate id for the DataNode, describing its content
         */
        val DataNode<*>.id: Meta
            get() = buildMeta {
                if (!meta.isEmpty) {
                    "name" to name
                    "meta" to meta
                }
                nodeStream(false).forEach {
                    "node" to it.id
                }
                dataStream(false).forEach {
                    "data" to it.id
                }
            }
    }
}
