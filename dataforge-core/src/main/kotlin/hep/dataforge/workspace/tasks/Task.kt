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

import hep.dataforge.Named
import hep.dataforge.data.DataNode
import hep.dataforge.description.Described
import hep.dataforge.meta.Meta
import hep.dataforge.workspace.Workspace

/**
 * The main building block of "pull" data flow model.
 *
 * @param <R>
 * @author Alexander Nozik
 */
interface Task<out R : Any> : Named, Described {

    /**
     * If true, the task is designated as terminal.
     * Terminal task is executed immediately after `run` is called, without any lazy calculations.
     * @return
     */
    val isTerminal: Boolean
        get() = false

    /**
     * The type of the node returned by the task
     */
    val type: Class<out R>

    /**
     * Build a model for this task
     *
     * @param workspace
     * @param taskConfig
     * @return
     */
    fun build(workspace: Workspace, taskConfig: Meta): TaskModel

    /**
     * Check if the model is valid and is acceptable by the task. Throw exception if not.
     *
     * @param model
     */
    @JvmDefault
    fun validate(model: TaskModel) {
        //do nothing
    }

    /**
     * Run given task model. Type check expected to be performed before actual
     * calculation.
     *
     * @param model
     * @return
     */
    fun run(model: TaskModel): DataNode<out R>

    companion object {
        const val TASK_TARGET = "task"
    }
}