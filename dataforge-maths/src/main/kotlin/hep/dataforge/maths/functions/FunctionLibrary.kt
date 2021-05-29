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

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.maths.functions

import hep.dataforge.context.*
import hep.dataforge.meta.Meta
import org.apache.commons.math3.analysis.BivariateFunction
import org.apache.commons.math3.analysis.MultivariateFunction
import org.apache.commons.math3.analysis.UnivariateFunction

/**
 * Mathematical plugin. Stores function library and other useful things.
 *
 * @author Alexander Nozik
 */
@PluginDef(name = "functions", group = "hep.dataforge", info = "A library of pre-compiled functions")
class FunctionLibrary : BasicPlugin() {

    private val univariateFactory = MultiFactory<UnivariateFunction>()
    private val bivariateFactory = MultiFactory<BivariateFunction>()
    private val multivariateFactory = MultiFactory<MultivariateFunction>()

    fun buildUnivariateFunction(key: String, meta: Meta): UnivariateFunction {
        return univariateFactory.build(key, meta)
    }

    fun addUnivariateFactory(type: String, factory: (Meta) -> UnivariateFunction) {
        this.univariateFactory.addFactory(type, factory)
    }

    fun addUnivariate(type: String, function: (Double) -> Double) {
        this.univariateFactory.addFactory(type) { UnivariateFunction(function) }
    }

    fun buildBivariateFunction(key: String, meta: Meta): BivariateFunction {
        return bivariateFactory.build(key, meta)
    }

    fun buildBivariateFunction(key: String): BivariateFunction {
        return bivariateFactory.build(key, Meta.empty())
    }

    fun addBivariateFactory(key: String, factory: (Meta) -> BivariateFunction) {
        this.bivariateFactory.addFactory(key, factory)
    }

    fun addBivariate(key: String, function: BivariateFunction) {
        this.bivariateFactory.addFactory(key) { function }
    }

    fun addBivariate(key: String, function: (Double, Double) -> Double) {
        this.bivariateFactory.addFactory(key) { BivariateFunction(function) }
    }


//    override fun respond(message: Envelope): Envelope {
//        val action = message.meta.getString("action", "getValue");
//        if (action == "getValue") {
//            val builder = EnvelopeBuilder().setDataType("hep.dataforge.function.response")
//            message.meta.getMetaList("request").forEach { request ->
//                val functionKey = request.getString("key")
//                val functionMeta = message.meta.getMetaOrEmpty("meta")
//                val arguments = request.getValue("argument").list.map { it.double }
//                val requestID = request.getValue("id", -1)
//
//
//                val result = when (arguments.size) {
//                    0 -> throw RuntimeException("No arguments found")
//                    1 -> {
//                        val univariateFunction = univariateFactory.build(functionKey, functionMeta)
//                        univariateFunction.value(arguments[0])
//                    }
//                    2 -> {
//                        val bivariateFunction = bivariateFactory.build(functionKey, functionMeta)
//                        bivariateFunction.value(arguments[0], arguments[1])
//                    }
//                    else -> {
//                        val multivariateFunction = multivariateFactory.build(functionKey, functionMeta)
//                        multivariateFunction.value(arguments.toDoubleArray())
//                    }
//                }
//                buildMeta("response", "result" to result, "id" to requestID)
//            }
//            return builder.build()
//        } else {
//            throw RuntimeException("Unknown action $action")
//        }
//    }

    class Factory : PluginFactory() {

        override val type: Class<out Plugin> = FunctionLibrary::class.java

        override fun build(meta: Meta): Plugin {
            return FunctionLibrary()
        }
    }

    companion object {
        fun buildFrom(context: Context): FunctionLibrary {
            return context.plugins.load(FunctionLibrary::class.java)
        }
    }

}
