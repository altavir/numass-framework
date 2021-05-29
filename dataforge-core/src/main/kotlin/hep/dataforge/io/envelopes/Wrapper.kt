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
package hep.dataforge.io.envelopes

import hep.dataforge.Named
import hep.dataforge.context.Context
import hep.dataforge.context.Global

/**
 * The class to unwrap object of specific type from envelope. Generally, T is
 * supposed to be Wrappable, but it is not guaranteed.
 *
 * @author Alexander Nozik
 */
interface Wrapper<T : Any> : Named {

    val type: Class<T>

    fun wrap(obj: T): Envelope

    fun unWrap(envelope: Envelope): T

    companion object {
        const val WRAPPER_CLASS_KEY = "@wrapper"

        @Suppress("UNCHECKED_CAST")
        @Throws(Exception::class)
        fun <T : Any> unwrap(context: Context, envelope: Envelope): T {
            val wrapper: Wrapper<T> = when {
                envelope.meta.hasValue(WRAPPER_CLASS_KEY) ->
                    Class.forName(envelope.meta.getString(WRAPPER_CLASS_KEY)).getConstructor().newInstance() as Wrapper<T>
                envelope.meta.hasValue(Envelope.ENVELOPE_TYPE_KEY) ->
                    context.findService(Wrapper::class.java) { it -> it.name == envelope.meta.getString(Envelope.ENVELOPE_TYPE_KEY) } as Wrapper<T>?
                            ?: throw RuntimeException("Unwrapper not found")
                else -> throw IllegalArgumentException("Not a wrapper envelope")
            }
            return wrapper.unWrap(envelope)
        }

        @Throws(Exception::class)
        fun <T : Any> unwrap(envelope: Envelope): T {
            return unwrap(Global, envelope)
        }

        @Suppress("UNCHECKED_CAST")
        fun wrap(context: Context, obj: Any): Envelope {
            val wrapper: Wrapper<Any> = context.findService(Wrapper::class.java) { it -> it.type != Any::class.java && it.type.isInstance(obj) } as Wrapper<Any>?
                    ?: JavaObjectWrapper
            return wrapper.wrap(obj)
        }
    }
}
