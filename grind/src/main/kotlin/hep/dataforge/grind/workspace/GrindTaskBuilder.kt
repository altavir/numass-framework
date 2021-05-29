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

package hep.dataforge.grind.workspace

import groovy.lang.Closure
import hep.dataforge.actions.Action
import hep.dataforge.actions.ActionEnv
import hep.dataforge.meta.Meta
import hep.dataforge.workspace.tasks.KTaskBuilder
import hep.dataforge.workspace.tasks.Task
import hep.dataforge.workspace.tasks.TaskModel

/**
 * A simplified wrapper class on top of KTaskBuilder to allow access from groovy
 */
class GrindTaskBuilder(name: String) {
    private val builder = KTaskBuilder(name)


    fun model(modelTransform: (TaskModel.Builder, Meta) -> Unit) {
        builder.model(modelTransform)
    }

    fun model(params: Map<String, Any>) {
        builder.model { meta ->
            params["data"]?.let {
                if (it is List<*>) {
                    it.forEach { this.data(it as String) }
                } else {
                    data(it as String)
                }
            }
            params["dependsOn"]?.let {
                dependsOn(it as String, meta)
            }
            if (params["data"] == null && params["dependsOn"] == null) {
                data("*")
            }
        }
    }

    fun pipe(action: (ActionEnv) -> Closure<Any>) {
        builder.pipe<Any, Any> { action(this).call(it) }
    }

    fun join(action: (ActionEnv) -> Closure<Any>) {
        builder.join<Any, Any> { action(this).call(it) }
    }

    fun action(action: Action<Any, Any>) {
        builder.action(action)
    }

    fun build(): Task<Any> {
        return builder.build()
    }
}