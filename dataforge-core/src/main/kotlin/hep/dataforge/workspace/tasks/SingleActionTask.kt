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

import hep.dataforge.actions.Action
import hep.dataforge.actions.GenericAction
import hep.dataforge.data.DataNode
import hep.dataforge.meta.Meta
import org.jetbrains.annotations.Contract

/**
 * A task wrapper for single action
 * Created by darksnake on 21-Aug-16.
 */
abstract class SingleActionTask<T : Any, R : Any>(type: Class<R>) : AbstractTask<R>(type) {

    protected open fun gatherNode(data: DataNode<out Any>): DataNode<T> {
        return data as DataNode<T>
    }

    protected abstract fun getAction(model: TaskModel): Action<T, R>

    protected open fun transformMeta(model: TaskModel): Meta {
        return model.meta
    }

    override fun run(model: TaskModel, data: DataNode<Any>): DataNode<R> {
        val actionMeta = transformMeta(model)
        val checkedData = gatherNode(data)
        return getAction(model).run(model.context, checkedData, actionMeta)
    }

    companion object {

        @Contract(pure = true)
        fun <T : Any, R : Any> from(action: GenericAction<T, R>, dependencyBuilder: (TaskModel.Builder, Meta) -> Unit): Task<R> {
            return object : SingleActionTask<T, R>(action.outputType) {
                override val name: String = action.name

                override fun buildModel(model: TaskModel.Builder, meta: Meta) {
                    dependencyBuilder(model, meta)
                }

                override fun getAction(model: TaskModel): Action<T, R> {
                    return action
                }
            }
        }

        @Contract(pure = true)
        fun <T : Any, R : Any> from(action: GenericAction<T, R>): Task<R> {
            return from(action) { model, meta -> model.allData() }
        }
    }

}
