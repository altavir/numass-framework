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

package hep.dataforge.maths.chain

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.map
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A not-necessary-Markov chain of some type
 * @param S - the state of the chain
 * @param R - the chain element type
 */
interface Chain<out R> : Sequence<R> {
    /**
     * Last value of the chain
     */
    val value: R

    /**
     * Generate next value, changing state if needed
     */
    suspend fun next(): R

    /**
     * Create a copy of current chain state. Consuming resulting chain does not affect initial chain
     */
    fun fork(): Chain<R>

    /**
     * Chain as a coroutine receive channel
     */
    val channel: ReceiveChannel<R>
        get() = GlobalScope.produce { while (true) send(next()) }

    override fun iterator(): Iterator<R> {
        return object : Iterator<R> {
            override fun hasNext(): Boolean = true

            override fun next(): R = runBlocking { this@Chain.next() }
        }
    }

    /**
     * Map the chain result using suspended transformation. Initial chain result can no longer be safely consumed
     * since mapped chain consumes tokens.
     */
    fun <T> map(func: suspend (R) -> T): Chain<T> {
        val parent = this;
        return object : Chain<T> {
            override val value: T
                get() = runBlocking { func.invoke(parent.value) }

            override suspend fun next(): T {
                return func(parent.next())
            }

            override fun fork(): Chain<T> {
                return parent.fork().map(func)
            }

            override val channel: ReceiveChannel<T>
                get() {
                    return parent.channel.map { func.invoke(it) }
                }
        }
    }
}

private class TransientValue<R : Any> {
    val mutex = Mutex()

    var value: R? = null
        private set

    suspend fun update(value: R) {
        mutex.withLock { this.value = value }
    }
}

//TODO force forks on mapping operations?

/**
 * A simple chain of independent tokens
 */
class SimpleChain<out R : Any>(private val gen: suspend () -> R) : Chain<R> {
    private val _value = TransientValue<R>()
    override val value: R
        get() = _value.value ?: runBlocking { next() }

    override suspend fun next(): R {
        _value.update(gen())
        return value
    }

    override fun fork(): Chain<R> = this
}

/**
 * A stateless Markov chain
 */
class MarkovChain<out R : Any>(private val seed: () -> R, private val gen: suspend (R) -> R) : Chain<R> {

    constructor(seed: R, gen: suspend (R) -> R) : this({ seed }, gen)

    private val _value = TransientValue<R>()
    override val value: R
        get() = _value.value ?: runBlocking { next() }

    override suspend fun next(): R {
        _value.update(gen(_value.value ?: seed()))
        return value
    }

    override fun fork(): Chain<R> {
        return MarkovChain(value, gen)
    }
}

/**
 * A chain with possibly mutable state. The state must not be changed outside the chain. Two chins should never share the state
 */
class StatefulChain<S, out R : Any>(val state: S, private val seed: S.() -> R, private val gen: suspend S.(R) -> R) : Chain<R> {
    constructor(state: S, seed: R, gen: suspend S.(R) -> R) : this(state, { seed }, gen)

    private val _value = TransientValue<R>()
    override val value: R
        get() = _value.value ?: runBlocking { next() }

    override suspend fun next(): R {
        _value.update(gen(state,_value.value ?: seed(state)))
        return value
    }

    override fun fork(): Chain<R> {
        throw RuntimeException("Fork not supported for stateful chain")
    }
}

/**
 * A chain that repeats the same value
 */
class ConstantChain<out T>(override val value: T) : Chain<T> {
    override suspend fun next(): T {
        return value
    }

    override fun fork(): Chain<T> {
        return this
    }
}