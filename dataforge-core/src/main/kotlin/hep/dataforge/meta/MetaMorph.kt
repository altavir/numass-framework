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

package hep.dataforge.meta

import java.io.ObjectStreamException
import java.io.Serializable
import java.lang.annotation.Inherited
import kotlin.reflect.KClass
import kotlin.reflect.full.*
import kotlin.reflect.jvm.javaType


/**
 * An object that could be identified by its meta. The contract is that two MetaID are equal if their {@code toMeta()} methods produce equal meta
 * Created by darksnake on 17.06.2017.
 */
interface MetaID {
    fun toMeta(): Meta
}

@MustBeDocumented
@Inherited
annotation class MorphTarget(val target: KClass<*>)

interface MorphProvider<T> {
    fun morph(meta: Meta): T
}

/**
 * An exception to be thrown when automatic class cast is failed
 */
class MorphException(val from: Class<*>, val to: Class<*>, message: String? = null, cause: Throwable? = null) : RuntimeException(message, cause) {
    override val message: String = String.format("Meta morph from %s to %s failed", from, to) + super.message
}

private class MetaMorphProxy(val type: Class<*>, val meta: Meta) : Serializable {
    private fun readResolve(): Any {
        return MetaMorph.morph(type, meta)
    }
}

/**
 * Ab object that could be represented as meta. Serialized via meta serializer and deserialized back
 * Created by darksnake on 12-Nov-16.
 */
interface MetaMorph : Serializable, MetaID {

    /**
     * Convert this object to Meta
     *
     * @return
     */
    override fun toMeta(): Meta

    companion object {
        /**
         * Create an instance of some class from its meta representation
         */
        @Suppress("UNCHECKED_CAST")
        fun <T : Any> morph(source: KClass<T>, meta: Meta): T {
            try {
                //Using morphing redirect for default implementation
                val type: KClass<out T> = source.findAnnotation<MorphTarget>()?.target as KClass<out T>? ?: source

                //trying to use constructor with single meta parameter
                val constructor = type.constructors.find { it.parameters.size == 1 && it.parameters.first().type.javaType == Meta::class.java }
                return when {
                    constructor != null -> constructor.call(meta)
                    type.companionObjectInstance is MorphProvider<*> -> (type.companionObjectInstance as MorphProvider<T>).morph(meta)
                    else -> throw RuntimeException("An instance of class $source could not be morphed")
                }
            } catch (ex: Exception) {
                throw MorphException(Meta::class.java, source.java, cause = ex)
            }
        }

        fun <T : Any> morph(source: Class<T>, meta: Meta): T {
            return morph(source.kotlin, meta)
        }
    }
}

/**
 * A simple metamorph implementation based on [MetaHolder].
 * It is supposed, that there is no state fields beside meta itself
 * Created by darksnake on 20-Nov-16.
 */
open class SimpleMetaMorph(meta: Meta) : MetaHolder(meta), MetaMorph {

    override fun toMeta(): Meta {
        return meta
    }

    @Throws(ObjectStreamException::class)
    fun writeReplace(): Any {
        return MetaMorphProxy(this::class.java, toMeta())
    }

    override fun hashCode(): Int {
        return meta.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return javaClass == other?.javaClass && (other as Metoid).meta == meta
    }
}

/**
 * A specific case of metamorph that could be mutated. If input meta is configuration, changes it on mutation.
 * Otherwise creates new configuration from meta
 * On deserialization converts to immutable [SimpleMetaMorph]
 */
open class ConfigMorph(meta: Meta) : SimpleMetaMorph(meta), Configurable {
    private val _config = (meta as? Configuration)?: Configuration(meta)

    override fun getConfig(): Configuration  = _config
}

/**
 * Convert a meta to given MetaMorph type. It is preferable to directly call the MetaMorph constructor.
 */
inline fun <reified T : Any> Meta.morph(): T {
    return MetaMorph.morph(T::class, this);
}


fun <T : Any> MetaMorph.morph(type: KClass<T>): T {
    return when {
        type.isSuperclassOf(this::class) -> type.cast(this)
        type.isSuperclassOf(Meta::class) -> type.cast(toMeta())
        type.isSubclassOf(MetaMorph::class) -> toMeta().morph(type)
        else -> throw MorphException(javaClass, type.java)
    }
}

/**
 * Converts MetaMorph to Meta or another metamorph using transformation to meta and back.
 * If the conversion is failed, catch the exception and rethrow it as [MorphException]
 *
 * @param type
 * @param <T>
 * @return
 */
inline fun <reified T : Any> MetaMorph.morph(): T {
    return this.morph(T::class)
}