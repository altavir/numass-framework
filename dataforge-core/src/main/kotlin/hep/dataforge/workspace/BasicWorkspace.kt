/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.workspace

import hep.dataforge.context.Context
import hep.dataforge.context.Global
import hep.dataforge.data.Data
import hep.dataforge.data.DataNode
import hep.dataforge.data.DataNodeBuilder
import hep.dataforge.data.DataTree
import hep.dataforge.meta.Meta
import hep.dataforge.workspace.tasks.Task

/**
 * A basic workspace with fixed data
 *
 * @author Alexander Nozik
 */
class BasicWorkspace(
        context: Context,
        taskMap: Map<String, Task<*>>,
        targetMap: Map<String, Meta>,
        override val data: DataNode<*>
) : AbstractWorkspace(context, taskMap, targetMap) {

    class Builder(override var context: Context = Global) : Workspace.Builder {
        private var data: DataNodeBuilder<Any> = DataTree.edit(Any::class.java).apply { name = "data" }

        private val taskMap: MutableMap<String, Task<*>> = HashMap()
        private val targetMap: MutableMap<String, Meta> = HashMap()

        //internal var workspace = BasicWorkspace()

        override fun self(): Builder {
            return this
        }

        override fun data(key: String, data: Data<out Any>): Builder {
//            if (this.data.optNode(key) != null) {
//                logger.warn("Overriding non-empty data during workspace data fill")
//            }
            this.data.putData(key, data)
            return self()
        }

        override fun data(key: String?, dataNode: DataNode<out Any>): Builder {
            if (key == null || key.isEmpty()) {
                if (!data.isEmpty) {
                    logger.warn("Overriding non-empty root data node during workspace construction")
                }
                data = dataNode.edit() as DataNodeBuilder<Any>
            } else {
                data.putNode(key, dataNode)
            }
            return self()
        }

        override fun task(task: Task<*>): Builder {
            taskMap[task.name] = task
            return self()
        }

        override fun target(name: String, meta: Meta): Builder {
            targetMap[name] = meta
            return self()
        }

        override fun build(): Workspace {
            context.plugins.stream(true)
                    .flatMap { plugin -> plugin.provideAll(Task.TASK_TARGET, Task::class.java) }
                    .forEach { taskMap.putIfAbsent(it.name, it) }
            return BasicWorkspace(context, taskMap, targetMap, data.build())
        }

    }

    companion object {

        fun builder(): Builder {
            return Builder()
        }

    }

}
