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

package hep.dataforge.workspace

import hep.dataforge.context.Context
import hep.dataforge.data.Data
import hep.dataforge.data.DataNode
import hep.dataforge.meta.Meta
import hep.dataforge.utils.ReferenceRegistry
import hep.dataforge.workspace.tasks.Task

typealias ActiveStateListener = () -> Unit

class ActiveState<T : Any>(val producer: () -> DataNode<T>) {
    private val listeners = ReferenceRegistry<ActiveStateListener>()

    val data: DataNode<T>
        get() = producer.invoke()

    fun addListener(listener: ActiveStateListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: ActiveStateListener) {
        listeners.remove { listener }
    }

}

class ActiveWorkspace(
        context: Context,
        taskMap: Map<String, Task<*>>,
        targetMap: Map<String, Meta>,
        initialData: DataNode<Any>? = null
) : AbstractWorkspace(context, taskMap, targetMap) {

    override var data: DataNode<Any> = initialData ?: DataNode.empty()
        set(value) {
            field = value
            dataChanged()
        }

    private fun dataChanged() {

    }

    fun pushData(map: Map<String, Data<Any>?>) {
        data = data.edit().apply {
            map.forEach { key, value ->
                if (value == null) {
                    this.removeData(key)
                } else {
                    this.putData(key, value, true)
                }
            }
        }.build()
    }

}