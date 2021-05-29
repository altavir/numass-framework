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
package hep.dataforge.actions

import hep.dataforge.context.Context
import hep.dataforge.data.DataNode
import hep.dataforge.data.DataSet
import hep.dataforge.data.NamedData
import hep.dataforge.description.ActionDescriptor
import hep.dataforge.description.TypedActionDef
import hep.dataforge.io.output.Output
import hep.dataforge.io.output.Output.Companion.TEXT_TYPE
import hep.dataforge.io.output.SelfRendered
import hep.dataforge.meta.Laminate
import hep.dataforge.meta.Meta
import hep.dataforge.names.Name
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.ExecutorService
import java.util.stream.Stream

/**
 * A basic implementation of Action interface
 *
 * @param <T>
 * @param <R>
 * @author Alexander Nozik
 */
abstract class GenericAction<T : Any, R : Any>(
        override val name: String,
        val inputType: Class<T>,
        val outputType: Class<R>
) : Action<T, R>, SelfRendered {

    private val definition: TypedActionDef?
        get() = if (javaClass.isAnnotationPresent(TypedActionDef::class.java)) {
            javaClass.getAnnotation(TypedActionDef::class.java)
        } else {
            null
        }

    protected val isEmptyInputAllowed: Boolean
        get() = false


    /**
     * {@inheritDoc}
     *
     * @return
     */
    override val descriptor: ActionDescriptor
        get() = ActionDescriptor.build(this)

    protected fun isParallelExecutionAllowed(meta: Meta): Boolean {
        return meta.getBoolean("@allowParallel", true)
    }

    /**
     * Generate the name of the resulting data based on name of input data and action meta
     *
     * @param inputName
     * @param actionMeta
     * @return
     */
    protected fun getResultName(inputName: String, actionMeta: Meta): String {
        var res = inputName
        if (actionMeta.hasValue(RESULT_GROUP_KEY)) {
            res = Name.joinString(actionMeta.getString(RESULT_GROUP_KEY, ""), res)
        }
        return res
    }

    /**
     * Wrap result of single or separate executions into DataNode
     *
     * @return
     */
    protected fun wrap(name: String, meta: Meta, result: Stream<out NamedData<R>>): DataNode<R> {
        val builder = DataSet.edit<R>(outputType)
        result.forEach { builder.add(it) }
        builder.name = name
        builder.meta = meta
        return builder.build()
    }

    protected fun checkInput(input: DataNode<*>) {
        if (!inputType.isAssignableFrom(input.type)) {
            //FIXME add specific exception
            throw RuntimeException(String.format("Type mismatch on action %s start. Expected %s but found %s.",
                    name, inputType.simpleName, input.type.name))
        }
    }

    /**
     * Get common singleThreadExecutor for this action
     *
     * @return
     */
    protected fun getExecutorService(context: Context, meta: Meta): ExecutorService {
        return if (isParallelExecutionAllowed(meta)) {
            context.executors.defaultExecutor
        } else {
            context.dispatcher
        }

    }

    /**
     * Return the root process name for this action
     *
     * @return
     */
    protected fun getThreadName(actionMeta: Meta): String {
        return actionMeta.getString("@action.thread", "action::$name")
    }

    protected fun getLogger(context: Context, actionMeta: Meta): Logger {
        return LoggerFactory.getLogger(context.name + "." + actionMeta.getString("@action.logger", getThreadName(actionMeta)))
    }

    override fun render(output: Output, meta: Meta) {
        output.render(descriptor, meta)
    }

    protected fun inputMeta(context: Context, vararg meta: Meta): Laminate {
        return Laminate(*meta).withDescriptor(descriptor)
    }

    /**
     * Push given object to output
     *
     * @param context
     * @param dataName
     * @param obj
     * @param meta
     */
    @JvmOverloads
    protected fun render(context: Context, dataName: String, obj: Any, meta: Meta = Meta.empty()) {
        context.output[dataName, name, TEXT_TYPE].render(obj, meta)
    }

    protected fun report(context: Context, reportName: String, entry: String, vararg params: Any) {
        context.history.getChronicle(reportName).report(entry, *params)
    }

    companion object {
        const val RESULT_GROUP_KEY = "@action.resultGroup"
    }
}
