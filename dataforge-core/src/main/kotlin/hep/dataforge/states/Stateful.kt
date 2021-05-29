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

package hep.dataforge.states

import hep.dataforge.exceptions.NameNotFoundException
import hep.dataforge.listAnnotations
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaMorph
import hep.dataforge.optional
import hep.dataforge.providers.Provider
import hep.dataforge.providers.Provides
import hep.dataforge.providers.ProvidesNames
import hep.dataforge.values.Value
import hep.dataforge.values.ValueProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.selects.select
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import java.util.stream.Stream
import kotlin.collections.HashMap
import kotlin.reflect.KClass


/**
 * An object that could have a set of readonly or read/write states
 */
interface Stateful : Provider {

    val logger: Logger
    val states: StateHolder

    @Provides(STATE_TARGET)
    fun optState(stateName: String): State<*>? {
        return states[stateName]
    }

    @get:ProvidesNames(STATE_TARGET)
    val stateNames: Collection<String>
        get() = states.names

    companion object {
        const val STATE_TARGET = "state"
    }
}

/**
 * Create a new meta state using class MetaState annotation if it is present and register it
 */
fun Stateful.metaState(
    name: String,
    owner: Stateful? = null,
    getter: (suspend () -> Meta)? = null,
    setter: (suspend State<Meta>.(Meta?, Meta) -> Unit)? = null
): MetaState {
    val def: MetaStateDef? = this::class.listAnnotations<MetaStateDef>(true).find { it.value.key == name }
    return if (def == null) {
        MetaState(name = name, owner = this, getter = getter, setter = setter)
    } else {
        MetaState(def.value, this, getter, setter)
    }
}

fun Stateful.metaState(
    name: String,
    getter: (suspend () -> Meta)? = null,
    setter: (suspend (Meta) -> Unit)
): MetaState {
    return metaState(name, getter = getter, setter = { old, value -> if (old != value) setter.invoke(value) })
}

fun Stateful.valueState(
    name: String,
    getter: (suspend () -> Any)? = null,
    setter: (suspend State<Value>.(Value?, Value) -> Unit)? = null
): ValueState {
    val def: StateDef? = this::class.listAnnotations<StateDef>(true).find { it.value.key == name }
    return if (def == null) {
        ValueState(name = name, owner = this, getter = getter, setter = setter)
    } else {
        ValueState(def.value, this, getter, setter)
    }
}

/**
 * Simplified version of value state generator, applies setter only if value is changed
 */
fun Stateful.valueState(
    name: String,
    getter: (suspend () -> Any)? = null,
    setter: (suspend State<Value>.(Value) -> Unit)
): ValueState {
    return valueState(name, getter = getter, setter = { old, value -> if (old != value) setter.invoke(this, value) })
}

fun <T : MetaMorph> Stateful.morphState(
    name: String,
    type: KClass<T>,
    def: T? = null,
    getter: (suspend () -> T)? = null,
    setter: (suspend State<T>.(T?, T) -> Unit)? = null
): MorphState<T> {
    return MorphState(name, type, def, this, getter, setter)
}

class StateHolder(val logger: Logger = LoggerFactory.getLogger(StateHolder::class.java)) : Provider, Iterable<State<*>>,
    ValueProvider, AutoCloseable {
    private val stateMap: MutableMap<String, State<*>> = HashMap()

    operator fun get(stateName: String): State<*>? {
        return stateMap[stateName]
    }

    /**
     * Type checked version of the get method
     */
    inline fun <reified S : State<*>> getState(stateName: String): S? {
        return get(stateName) as? S
    }

    /**
     * null invalidates the state
     */
    operator fun set(stateName: String, value: Any?) {
        stateMap[stateName]?.set(value) ?: throw NameNotFoundException(stateName)
    }

    val names: Collection<String>
        get() = stateMap.keys

    fun stream(): Stream<State<*>> {
        return stateMap.values.stream()
    }

    override fun iterator(): Iterator<State<*>> {
        return stateMap.values.iterator()
    }

    /**
     * Register a new state
     */
    fun init(state: State<*>) {
        this.stateMap[state.name] = state
    }

    /**
     * Reset state to its default value if it is present
     */
    fun invalidate(stateName: String) {
        stateMap[stateName]?.invalidate()
    }

    /**
     * Update logical state if it is changed. If argument is Meta or MetaMorph, then redirect to {@link updateLogicalMetaState}
     *
     * @param stateName
     * @param stateValue
     */
    fun update(stateName: String, stateValue: Any?) {
        val state = stateMap.getOrPut(stateName) {
            logger.warn("State with name $stateName is not registered. Creating new logical state")
            when (stateValue) {
                is Meta -> MetaState(stateName).also { init(it) }
                is MetaMorph -> MorphState(stateName, (stateValue as MetaMorph)::class)
                else -> ValueState(stateName).also { init(it) }
            }
        }

        state.update(stateValue)
//        logger.info("State {} changed to {}", stateName, stateValue)
    }

    override fun optValue(path: String): Optional<Value> {
        return (get(path) as? ValueState)?.value.optional
    }

    /**
     * Subscribe on updates of specific states. By default subscribes on all updates.
     * Subscription is formed when the method is called, so states initialized after that are ignored.
     */
    fun changes(pattern: Regex = ".*".toRegex()): Flow<Pair<String, Any>> {
        val subscriptions = stateMap.filter { it.key.matches(pattern) }.mapValues { it.value.subscribe() }
        return flow {
            try {
                while (true) {
                    select<Unit> {
                        subscriptions.forEach { key, value ->
                            value.onReceive {
                                emit(Pair(key, it))
                            }
                        }
                    }
                }
            } catch (ex: CancellationException) {
                subscriptions.values.forEach {
                    it.cancel()
                }
            }
        }
    }

    override fun close() {

    }
}