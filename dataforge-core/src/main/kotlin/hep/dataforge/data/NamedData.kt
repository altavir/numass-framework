/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.data

import hep.dataforge.Named
import hep.dataforge.exceptions.AnonymousNotAlowedException
import hep.dataforge.goals.Goal
import hep.dataforge.goals.StaticGoal
import hep.dataforge.meta.Laminate
import hep.dataforge.meta.Meta
import hep.dataforge.names.AnonymousNotAlowed
import hep.dataforge.names.Name

/**
 * A data with name
 *
 * @author Alexander Nozik
 */
@AnonymousNotAlowed
open class NamedData<T>(final override val name: String, type: Class<T>, goal: Goal<out T>, meta: Meta) : Data<T>(type, goal, meta), Named {

    init {
        if (name.isEmpty()) {
            throw AnonymousNotAlowedException()
        }
    }


    operator fun component1() = name
    operator fun component2(): T = goal.get()

    /**
     * Return unnamed data corresponding to this named one
     *
     * @return
     */
    fun anonymize(): Data<T> {
        return Data(this.type, this.goal, this.meta)
    }

    override fun <R> cast(type: Class<R>): NamedData<R> {
        return if (type.isAssignableFrom(type)) {
            @Suppress("UNCHECKED_CAST")
            NamedData(name, type, goal as Goal<R>, meta)
        } else {
            throw IllegalArgumentException("Invalid type to upcast data")
        }
    }

    companion object {

        fun <T: Any> buildStatic(name: String, content: T, meta: Meta): NamedData<T> {
            return NamedData(name, content.javaClass, StaticGoal<T>(content), meta)
        }

        /**
         * Wrap existing data using name and layers of external meta if it is available
         *
         * @param name
         * @param data
         * @param externalMeta
         * @param <T>
         * @return
         */
        fun <T> wrap(name: String, data: Data<T>, vararg externalMeta: Meta): NamedData<T> {
            val newMeta = Laminate(data.meta).withLayer(*externalMeta)
            return NamedData(name, data.type, data.goal, newMeta)
        }

        fun <T> wrap(name: Name, data: Data<T>, externalMeta: Laminate): NamedData<T> {
            val newMeta = externalMeta.withFirstLayer(data.meta)
            return NamedData(name.toString(), data.type, data.goal, newMeta)
        }
    }
}
