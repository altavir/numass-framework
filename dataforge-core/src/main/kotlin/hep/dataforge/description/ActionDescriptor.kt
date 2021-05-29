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
package hep.dataforge.description

import hep.dataforge.actions.Action
import hep.dataforge.actions.GenericAction
import hep.dataforge.actions.ManyToOneAction
import hep.dataforge.io.output.Output
import hep.dataforge.io.output.SelfRendered
import hep.dataforge.io.output.TextOutput
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder
import java.awt.Color

/**
 *
 *
 * ActionDescriptor class.
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
class ActionDescriptor(meta: Meta) : NodeDescriptor(meta), SelfRendered {

    override val info: String
        get() = meta.getString("actionDef.description", "")

    val inputType: String
        get() = meta.getString("actionDef.inputType", "")

    val outputType: String
        get() = meta.getString("actionDef.outputType", "")

    override val name: String
        get() = meta.getString("actionDef.name", super.name)

    override fun render(output: Output, meta: Meta) {
        if (output is TextOutput) {
            output.renderText(name, Color.GREEN)
            output.renderText(" {input : ")
            output.renderText(inputType, Color.CYAN)
            output.renderText(", output : ")
            output.renderText(outputType, Color.CYAN)
            output.renderText(String.format("}: %s", info))

        } else {
            output.render(String.format("Action %s (input: %s, output: %s): %s%n", name, inputType, outputType, info), meta)
        }
    }

    companion object {

        fun build(action: Action<*, *>): ActionDescriptor {
            val builder = Descriptors.forType(action.name, action::class).meta.builder

            val actionDef = MetaBuilder("actionDef").putValue("name", action.name)
            if (action is GenericAction<*, *>) {
                actionDef
                        .putValue("inputType", action.inputType.simpleName)
                        .putValue("outputType", action.outputType.simpleName)
                if (action is ManyToOneAction<*, *>) {
                    actionDef.setValue("inputType", (action as GenericAction<*, *>).outputType.simpleName + "[]")
                }
            }

            val def = action.javaClass.getAnnotation(TypedActionDef::class.java)

            if (def != null) {
                actionDef.putValue("description", def.info)
            }
            builder.putNode(actionDef)
            return ActionDescriptor(builder)
        }

        fun build(actionClass: Class<out Action<*, *>>): ActionDescriptor {
            val builder = Descriptors.forType("action", actionClass.kotlin).meta.builder

            val def = actionClass.getAnnotation(TypedActionDef::class.java)
            if (def != null) {
                val actionDef = MetaBuilder("actionDef")
                        .putValue("name", def.name)
                        .putValue("inputType", def.inputType.simpleName)
                        .putValue("outputType", def.outputType.simpleName)
                        .putValue("description", def.info)

                if (actionClass.isAssignableFrom(ManyToOneAction::class.java)) {
                    actionDef.setValue("inputType", def.inputType.simpleName + "[]")
                }

                builder.putNode(actionDef)

            }
            return ActionDescriptor(builder)
        }
    }
}
