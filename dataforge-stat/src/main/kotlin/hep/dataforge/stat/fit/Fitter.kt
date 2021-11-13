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

import hep.dataforge.Named
import hep.dataforge.io.history.History
import hep.dataforge.meta.KMetaBuilder
import hep.dataforge.meta.Meta
import hep.dataforge.meta.buildMeta
import hep.dataforge.stat.fit.FitStage.FREE_PARAMETERS

/**
 *
 *
 * Fitter interface.
 *
 * @author Alexander Nozik
 */
interface Fitter : Named {

    fun run(state: FitState, parentLog: History?, meta: Meta): FitResult


    fun run(state: FitState, parentLog: History? = null, meta: KMetaBuilder.() -> Unit): FitResult {
        return run(state, parentLog, buildMeta("fit", meta))
    }

    companion object {


        const val FITTER_TARGET = "fitter"
        /**
         *
         * @param state
         * @param task
         * @return
         */
        fun getFitPars(state: FitState, meta: Meta): Array<String> {
            return meta.getStringArray(FREE_PARAMETERS, state.model.namesAsArray())
        }
    }
}
