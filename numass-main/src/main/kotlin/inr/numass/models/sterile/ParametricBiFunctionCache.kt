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

package inr.numass.models.sterile

import hep.dataforge.names.NameList
import hep.dataforge.stat.parametric.ParametricBiFunction
import hep.dataforge.values.Values

class ParametricBiFunctionCache(val function: ParametricBiFunction): ParametricBiFunction {
    override fun derivValue(parName: String?, x: Double, y: Double, set: Values?): Double {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getNames(): NameList {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun value(x: Double, y: Double, set: Values?): Double {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun providesDeriv(name: String?): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}