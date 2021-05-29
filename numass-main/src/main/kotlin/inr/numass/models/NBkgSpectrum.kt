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
package inr.numass.models

import hep.dataforge.names.NamesUtils.combineNamesWithEquals
import hep.dataforge.stat.parametric.AbstractParametricFunction
import hep.dataforge.stat.parametric.ParametricFunction
import hep.dataforge.utils.MultiCounter
import hep.dataforge.values.ValueProvider
import hep.dataforge.values.Values

/**
 *
 * @author Darksnake
 */
open class NBkgSpectrum(private val source: ParametricFunction) : AbstractParametricFunction(*combineNamesWithEquals(source.namesAsArray(), *list)) {

    var counter = MultiCounter(this.javaClass.name)

    override fun derivValue(parName: String, x: Double, set: Values): Double {
        this.counter.increase(parName)
        return when (parName) {
            "N" -> source.value(x, set)
            "bkg" -> 1.0
            else -> getN(set) * source.derivValue(parName, x, set)
        }
    }

    private fun getBkg(set: ValueProvider): Double {
        return set.getDouble("bkg")
    }

    private fun getN(set: ValueProvider): Double {
        return set.getDouble("N")
    }

    override fun providesDeriv(name: String): Boolean {
        return when (name) {
            "N","bkg" -> true
            else -> this.source.providesDeriv(name)
        }
    }

    override fun value(x: Double, set: Values): Double {
        this.counter.increase("value")
        return getN(set) * source.value(x, set) + getBkg(set)
    }

    override fun getDefaultParameter(name: String): Double {
        return when (name) {
            "bkg" -> 0.0
            "N" -> 1.0
            else -> super.getDefaultParameter(name)
        }
    }

    companion object {

        private val list = arrayOf("N", "bkg")
    }

}
