/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.workspace

import hep.dataforge.cache.CachePlugin
import hep.dataforge.context.Context
import hep.dataforge.data.DataNode
import hep.dataforge.meta.Laminate
import hep.dataforge.meta.Meta
import hep.dataforge.workspace.tasks.Task
import hep.dataforge.workspace.tasks.TaskModel

/**
 * @author Alexander Nozik
 */
abstract class AbstractWorkspace(
        override val context: Context,
        protected val taskMap: Map<String, Task<*>>,
        protected val targetMap: Map<String, Meta>
) : Workspace {


    override fun optTask(taskName: String): Task<*>? {
        return taskMap[taskName]
    }

    override val tasks: Collection<Task<*>>
        get() = taskMap.values

    override val targets: Collection<Meta>
        get() = targetMap.values

    /**
     * Automatically constructs a laminate if `@parent` value if defined
     * @param name
     * @return
     */
    override fun optTarget(name: String): Meta? {
        val target = targetMap[name]
        return if (target == null) {
            null
        } else {
            if (target.hasValue(PARENT_TARGET_KEY)) {
                Laminate(target, optTarget(target.getString(PARENT_TARGET_KEY)) ?: Meta.empty())
            } else {
                target
            }
        }
    }

    private val cache: CachePlugin by lazy {
        context.getOrLoad(CachePlugin::class.java)
    }

    private val cacheEnabled: Boolean
        get() = context[CachePlugin::class.java] != null && context.getBoolean("cache.enabled", true)

    override fun runTask(model: TaskModel): DataNode<*> {
        //Cache result if cache is available and caching is not blocked by task
        val result = getTask(model.name).run(model)
        return if (cacheEnabled && model.meta.getBoolean("cache.enabled", true)) {
            cacheTaskResult(model, result)
        } else {
            result
        }
    }

    /**
     * Put given data node into cache one by one
     */
    private fun <R : Any> cacheTaskResult(model: TaskModel, node: DataNode<out R>): DataNode<out R> {
        return cache.cacheNode(model.name, node) { model.getID(it) }
    }

    override fun clean() {
        logger.info("Cleaning up cache...")
        invalidateCache()
    }

    private fun invalidateCache() {
        if (cacheEnabled) {
            cache.invalidate()
        }
    }

    companion object {

        /**
         * The key in the meta designating parent target. The resulting target is obtained by overlaying parent with this one
         */
        const val PARENT_TARGET_KEY = "@parent"

    }

}
