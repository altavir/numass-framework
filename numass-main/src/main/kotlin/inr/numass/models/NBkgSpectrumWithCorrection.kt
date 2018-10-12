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

package inr.numass.models

import hep.dataforge.names.NamesUtils.combineNamesWithEquals
import hep.dataforge.stat.parametric.AbstractParametricFunction
import hep.dataforge.stat.parametric.ParametricFunction
import hep.dataforge.values.Values

class NBkgSpectrumWithCorrection(val source: ParametricFunction) : AbstractParametricFunction(*combineNamesWithEquals(source.namesAsArray(), *parameters)) {

    private val Values.bkg get() = getDouble("bkg")
    private val Values.n get() = getDouble("N")
    private val Values.l get() = getDouble("L")
    private val Values.q get() = getDouble("Q")

    override fun derivValue(parName: String, x: Double, set: Values): Double {
        return when (parName) {
            "bkg" -> 1.0
            "N" -> source.value(x, set)
            "L" -> x / 1e3 * source.value(x, set)
            "Q" -> x * x /1e6 * source.value(x, set)
            else -> (set.n + x/1e3 * set.l + x * x /1e6 * set.q) * source.derivValue(parName, x, set)
        }
    }

    override fun value(x: Double, set: Values): Double {
        return  (set.n + x * set.l / 1e3 + x * x / 1e6 * set.q) * source.value(x, set) + set.bkg
    }

    override fun providesDeriv(name: String): Boolean {
        return name in parameters || source.providesDeriv(name)
    }

    override fun getDefaultParameter(name: String): Double {
        return when (name) {
            "bkg" -> 0.0
            "N" -> 1.0
            "L" -> 0.0
            "Q" -> 0.0
            else -> super.getDefaultParameter(name)
        }
    }

    companion object {
        val parameters = arrayOf("bkg, N, L, Q")
    }
}