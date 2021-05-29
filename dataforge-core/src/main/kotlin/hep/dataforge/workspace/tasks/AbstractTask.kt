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

package hep.dataforge.workspace.tasks

import hep.dataforge.data.DataNode
import hep.dataforge.data.DataNodeBuilder
import hep.dataforge.data.DataTree
import hep.dataforge.description.NodeDescriptor
import hep.dataforge.meta.Meta
import hep.dataforge.workspace.Workspace


/**
 * @param type the upper boundary type of a node, returned by this task.
 * @param descriptor the descriptor override for this task. If null, construct descriptor from annotations.
 * Created by darksnake on 21-Aug-16.
 */
abstract class AbstractTask<R : Any>(override val type: Class<R>, descriptor: NodeDescriptor? = null) : Task<R> {

    override val descriptor = descriptor?:super.descriptor


    protected open fun gather(model: TaskModel): DataNode<Any> {
        val builder: DataNodeBuilder<Any> = DataTree.edit()
        model.dependencies.forEach { dep ->
            dep.apply(builder, model.workspace)
        }
        return builder.build()
    }

    override fun run(model: TaskModel): DataNode<R> {
        //validate model
        validate(model)

        // gather data
        val input = gather(model)

        //execute
        val output = run(model, input)

        //handle result
        output.handle(model.context.dispatcher) { this.handle(it) }

        return output
    }

    /**
     * Result handler for the task
     */
    protected open fun handle(output: DataNode<R>) {
        //do nothing
    }

    protected abstract fun run(model: TaskModel, data: DataNode<Any>): DataNode<R>

    /**
     * Apply model transformation to include custom dependencies or change
     * existing ones.
     *
     * @param model the model to be transformed
     * @param meta  the whole configuration (not only for this particular task)
     */
    protected abstract fun buildModel(model: TaskModel.Builder, meta: Meta)

    /**
     * Build new TaskModel and apply specific model transformation for this
     * task. By default model uses the meta node with the same node as the name of the task.
     *
     * @param workspace
     * @param taskConfig
     * @return
     */
    override fun build(workspace: Workspace, taskConfig: Meta): TaskModel {
        val taskMeta = taskConfig.getMeta(name, taskConfig)
        val builder = TaskModel.builder(workspace, name, taskMeta)
        buildModel(builder, taskConfig)
        return builder.build()
    }
}
