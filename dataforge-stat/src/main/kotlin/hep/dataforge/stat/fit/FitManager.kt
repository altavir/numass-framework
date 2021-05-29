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
package hep.dataforge.stat.fit

import hep.dataforge.actions.Action
import hep.dataforge.context.BasicPlugin
import hep.dataforge.context.Plugin
import hep.dataforge.context.PluginDef
import hep.dataforge.context.PluginFactory
import hep.dataforge.exceptions.NameNotFoundException
import hep.dataforge.io.history.History
import hep.dataforge.meta.Meta
import hep.dataforge.providers.Provides
import hep.dataforge.providers.ProvidesNames
import hep.dataforge.stat.fit.Fitter.Companion.FITTER_TARGET
import hep.dataforge.stat.models.Model
import hep.dataforge.stat.models.ModelFactory
import hep.dataforge.tables.Table
import java.util.*

/**
 * @author Alexander Nozik
 */
@PluginDef(group = "hep.dataforge", name = "fitting", dependsOn = arrayOf("hep.dataforge:models"), info = "Basic dataforge fitting plugin")
class FitManager : BasicPlugin() {

    @Provides(FITTER_TARGET)
    fun getFitter(fitterName: String): Fitter? {
        return when (fitterName) {
            QOWFitter.QOW_ENGINE_NAME -> QOWFitter
            CMFitter.CM_ENGINE_NAME -> CMFitter
            else -> null
        }
    }

    @ProvidesNames(FITTER_TARGET)
    fun listFitters(): List<String> = listOf(QOWFitter.QOW_ENGINE_NAME, CMFitter.CM_ENGINE_NAME)

    @Provides(Action.ACTION_TARGET)
    fun getFitAction(actionName: String): Action<*, *>? {
        return when (actionName) {
            FitAction.FIT_ACTION_NAME -> FitAction()
            else -> null
        }
    }

    @ProvidesNames(Action.ACTION_TARGET)
    fun listAction(): List<String> = listOf(FitAction.FIT_ACTION_NAME)


    fun buildEngine(name: String): Fitter {
        return context.provideAll(FITTER_TARGET, Fitter::class.java).filter { it -> it.name == name }.findAny().orElseThrow { NameNotFoundException(name) }
    }


    fun buildModel(name: String): Model {
        return context.provideAll(ModelFactory.MODEL_TARGET, ModelFactory::class.java)
                .filter { it.name == name }.findFirst()
                .orElseThrow { RuntimeException("Model with name $name not found in ${context.name}") }
                .build(context, Meta.empty())
    }

    fun buildModel(meta: Meta): Model {
        return context.provideAll(ModelFactory.MODEL_TARGET, ModelFactory::class.java)
                .filter { it.name == meta.getString("modelName") }.findFirst()
                .orElseThrow { RuntimeException("Model not defined") }
                .build(context, meta)
    }

    fun optModel(meta: Meta): Optional<Model> {
        return context.provideAll(ModelFactory.MODEL_TARGET, ModelFactory::class.java)
                .filter { it.name == meta.getString("name") }.findFirst().map { it.build() }
    }

    fun buildState(data: Table, modelName: String, pars: ParamSet): FitState {
        val model = buildModel(modelName)
        return FitState(data, model, pars)
    }

    fun buildState(data: Table, meta: Meta, pars: ParamSet): FitState {
        val model = buildModel(meta)
        return FitState(data, model, pars)
    }

    fun runDefaultStage(state: FitState, vararg freePars: String): FitResult {
        return runDefaultStage(state, context.history, *freePars)
    }

    fun runDefaultStage(state: FitState, log: History, vararg freePars: String): FitResult {
        val task = FitStage(QOWFitter.QOW_ENGINE_NAME, FitStage.TASK_RUN, freePars)
        return runStage(state, task, log)
    }

    fun runStage(state: FitState, engineName: String, taskName: String, vararg freePars: String): FitResult {
        val task = FitStage(engineName, taskName, freePars)
        return runStage(state, task, context.history)
    }

    fun runStage(state: FitState?, task: FitStage, log: History): FitResult {
        val engine = buildEngine(task.engineName)
        if (state == null) {
            throw IllegalArgumentException("The fit state is not defined")
        }

        log.report("Starting fit task {}", task.toString())
        val newState = engine.run(state, log, task.meta)

        if (!newState.isValid()) {
            log.reportError("The result of the task is not a valid state")
        }
        return newState
    }

    class Factory : PluginFactory() {

        override val type: Class<out Plugin>
            get() = FitManager::class.java

        override fun build(meta: Meta): Plugin {
            return FitManager()
        }
    }
}
