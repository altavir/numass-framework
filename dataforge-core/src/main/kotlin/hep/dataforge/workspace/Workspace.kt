/* 
 * Copyright 2015 Alexander Nozik.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package hep.dataforge.workspace

import hep.dataforge.context.Context
import hep.dataforge.context.ContextAware
import hep.dataforge.context.ContextBuilder
import hep.dataforge.context.Global
import hep.dataforge.data.*
import hep.dataforge.exceptions.NameNotFoundException
import hep.dataforge.meta.Laminate
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaNode.DEFAULT_META_NAME
import hep.dataforge.providers.Provider
import hep.dataforge.providers.Provides
import hep.dataforge.providers.ProvidesNames
import hep.dataforge.utils.GenericBuilder
import hep.dataforge.workspace.tasks.Task
import hep.dataforge.workspace.tasks.TaskModel

/**
 * A place to store tasks and their results
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
interface Workspace : ContextAware, Provider {

    /**
     * Get the whole data tree
     *
     * @return
     */
    val data: DataNode<*>

    /**
     * The stream of available tasks
     *
     * @return
     */
    @get:ProvidesNames(Task.TASK_TARGET)
    val tasks: Collection<Task<*>>

    /**
     * Get stream of meta objects stored in the Workspace. Not every target is valid for every task.
     *
     * @return
     */
    @get:ProvidesNames(Meta.META_TARGET)
    val targets: Collection<Meta>

    //    String DATA_STAGE_NAME = "@data";

    /**
     * Get specific static data. Null if no data with given name is found
     *
     * @param dataPath Fully qualified data name
     * @return
     */

    fun getData(dataPath: String): Data<*> {
        return data.getData(dataPath)
    }

    /**
     * Opt task by name
     *
     * @param taskName
     * @return
     */
    @Provides(Task.TASK_TARGET)
    fun optTask(taskName: String): Task<*>?

    /**
     * Get task by name. Throw [hep.dataforge.exceptions.NameNotFoundException] if task with given name does not exist.
     *
     * @param taskName
     * @return
     */

    fun getTask(taskName: String): Task<*> {
        return optTask(taskName) ?: throw NameNotFoundException(taskName)
    }

    /**
     * Check task dependencies and run it with given configuration or load
     * result from cache if it is available
     *
     * @param taskName
     * @param config
     * @param overlay  use given meta as overaly for existing meta with the same name
     * @return
     */

    fun runTask(taskName: String, config: Meta, overlay: Boolean): DataNode<*> {
        val task = getTask(taskName)
        val taskConfig = if (overlay && hasTarget(config.name)) {
            Laminate(config, getTarget(config.name))
        } else {
            config
        }
        val model = task.build(this, taskConfig)
        return runTask(model)
    }


    fun runTask(taskName: String, config: Meta): DataNode<*> {
        return this.runTask(taskName, config, true)
    }

    /**
     * Use config root node name as task name
     *
     * @param config
     * @return
     */

    fun runTask(config: Meta): DataNode<*> {
        return runTask(config.name, config)
    }

    /**
     * Run task using meta previously stored in workspace.
     *
     * @param taskName
     * @param target
     * @return
     */

    fun runTask(taskName: String, target: String = taskName): DataNode<*> {
        return runTask(taskName, optTarget(target) ?: Meta.empty())
    }

    /**
     * Run task with given model.
     *
     * @param model
     * @return
     */

    fun runTask(model: TaskModel): DataNode<*> {
        return this.getTask(model.name).run(model)
    }

    /**
     * Opt a predefined meta with given name
     *
     * @return
     */
    @Provides(Meta.META_TARGET)
    fun optTarget(name: String): Meta?

    /**
     * Get a predefined meta with given name
     *
     * @param name
     * @return
     */

    fun getTarget(name: String): Meta {
        return optTarget(name) ?: throw NameNotFoundException(name)
    }

    /**
     * Check if workspace contains given target
     *
     * @param name
     * @return
     */

    fun hasTarget(name: String): Boolean {
        return optTarget(name) != null
    }

    /**
     * Clean up workspace. Invalidate caches etc.
     */
    fun clean()

    interface Builder : GenericBuilder<Workspace, Workspace.Builder>, ContextAware {

        override var context: Context

    
        fun loadFrom(meta: Meta): Workspace.Builder {
            if (meta.hasValue("context")) {
                context = Global.getContext(meta.getString("context"))
            }
            if (meta.hasMeta("data")) {
                meta.getMetaList("data").forEach { dataMeta: Meta ->
                    val factory: DataFactory<out Any> = if (dataMeta.hasValue("dataFactoryClass")) {
                        try {
                            Class.forName(dataMeta.getString("dataFactoryClass")).newInstance() as DataFactory<*>
                        } catch (ex: Exception) {
                            throw RuntimeException("Error while initializing data factory", ex)
                        }
                    } else {
                        FileDataFactory()
                    }
                    val key = dataMeta.getString("as", "")
                    data(key, factory.build(context, dataMeta))
                }
            }
            if (meta.hasMeta("target")) {
                meta.getMetaList("target").forEach { configMeta: Meta ->
                    target(configMeta.getString("name"),
                        configMeta.getMeta(DEFAULT_META_NAME))
                }
            }

            return self()
        }

        fun data(key: String, data: Data<out Any>): Workspace.Builder

        /**
         * Load a data node to workspace data tree.
         *
         * @param key       path to the new node in the data tree could be empty
         * @param dataNode
         * @return
         */
        fun data(key: String?, dataNode: DataNode<out Any>): Workspace.Builder


        /**
         * Load data using universal data loader
         *
         * @param place
         * @param dataConfig
         * @return
         */
    
        fun data(place: String, dataConfig: Meta): Workspace.Builder {
            return data(place, DataLoader.SMART.build(context, dataConfig))
        }

        /**
         * Load a data node generated by given DataLoader
         *
         * @param place
         * @param factory
         * @param dataConfig
         * @return
         */
    
        fun data(place: String, factory: DataLoader<out Any>, dataConfig: Meta): Workspace.Builder {
            return data(place, factory.build(context, dataConfig))
        }

        /**
         * Add static data to the workspace
         *
         * @param name
         * @param `obj`
         * @param meta
         * @return
         */
    
        fun staticData(name: String, obj: Any, meta: Meta): Workspace.Builder {
            return data(name, Data.buildStatic(obj, meta))
        }

    
        fun staticData(name: String, obj: Any): Workspace.Builder {
            return data(name, Data.buildStatic(obj))
        }

    
        fun fileData(place: String, filePath: String, meta: Meta): Workspace.Builder {
            return data(place, DataUtils.readFile(context.getFile(filePath), meta))
        }

    
        fun fileData(dataName: String, filePath: String): Workspace.Builder {
            return fileData(dataName, filePath, Meta.empty())
        }

        fun target(name: String, meta: Meta): Workspace.Builder

    
        fun target(meta: Meta): Workspace.Builder {
            return target(meta.name, meta)
        }

        fun task(task: Task<*>): Workspace.Builder

        @Throws(IllegalAccessException::class, InstantiationException::class)
    
        fun task(type: Class<Task<*>>): Workspace.Builder {
            return task(type.getConstructor().newInstance())
        }
    }
}

inline fun <reified T : Any> Workspace.Builder.data(block: DataNodeBuilder<*>.() -> Unit) {
    data(null, DataTree<T>(block))
}

fun Workspace.Builder.context(name: String = "WORKSPACE", builder: ContextBuilder.() -> Unit) {
    context = ContextBuilder(name, context).apply(builder).build()
}

fun Workspace(context: Context = Global, block: Workspace.Builder.() -> Unit): Workspace {
    return BasicWorkspace.Builder(context).apply(block).build()
}