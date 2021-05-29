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

package hep.dataforge.stat

import hep.dataforge.context.Context
import hep.dataforge.io.history.Chronicle
import hep.dataforge.io.history.History
import hep.dataforge.meta.*
import hep.dataforge.stat.fit.*
import hep.dataforge.stat.models.Model
import hep.dataforge.tables.NavigableValuesSource
import hep.dataforge.values.Value


fun Context.fit(action: Kfit.() -> Unit): FitResult {
    return Kfit(this).apply(action).fit()
}


class Kfit(val context: Context) {
    val manager: FitManager = context.load()


    var data: NavigableValuesSource? = null
    var model: Model? = null
    var parameters: ParamSet? = null
    var history: History = Chronicle("fit", context.history)
    var engine: Fitter = QOWFitter

    var meta: Configuration = Configuration()
    var action: String by meta.mutableStringValue()
    var method: String by meta.mutableStringValue()
    var freePars: List<String> by meta.customMutableValue(
            def = emptyList(),
            read = { it.list.map { it.string } },
            write = { Value.of(it) }
    )

    var listener: (FitResult) -> Unit = {}

    fun engine(engineName: String) {
        engine = manager.buildEngine(engineName)
    }

    fun model(modelName: String) {
        model = manager.buildModel(modelName)
    }

    fun model(transform: KMetaBuilder.() -> Unit) {
        model = manager.buildModel(buildMeta("model", transform))
    }

    fun fit(): FitResult {
        if (data == null) {
            throw IllegalStateException("Data not set")
        }

        if (model == null) {
            throw IllegalStateException("Model not set")
        }

        if (parameters == null) {
            throw IllegalStateException("Starting parameters are not defined not set")
        }

        val state = FitState(data, model, parameters)
        return engine.run(state, history, meta).also { listener.invoke(it) }
    }
}