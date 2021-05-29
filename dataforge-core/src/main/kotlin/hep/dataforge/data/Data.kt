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
package hep.dataforge.data

import hep.dataforge.data.binary.Binary
import hep.dataforge.goals.AbstractGoal
import hep.dataforge.goals.GeneratorGoal
import hep.dataforge.goals.Goal
import hep.dataforge.goals.StaticGoal
import hep.dataforge.io.envelopes.Envelope
import hep.dataforge.meta.Meta
import hep.dataforge.meta.Metoid
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.function.Supplier
import java.util.stream.Stream

/**
 * A piece of data which is basically calculated asynchronously
 *
 * @param <T>
 * @author Alexander Nozik
 * @version $Id: $Id
 */
open class Data<T>(val type: Class<T>,
                   val goal: Goal<out T>,
                   override val meta: Meta = Meta.empty()) : Metoid {

    /**
     * Asynchronous data handler. Computation could be canceled if needed
     *
     * @return
     */
    val future: CompletableFuture<out T>
        get() = goal.asCompletableFuture()

    /**
     * @return false if goal is canceled or completed exceptionally
     */
    val isValid: Boolean
        get() = !future.isCancelled && !future.isCompletedExceptionally

    /**
     * Compute underlying goal and return sync result.
     *
     * @return
     */
    fun get(): T {
        return goal.get()
    }

    /**
     * Upcast the data type
     *
     * @param type
     * @param <R>
     * @return
     */
    @Suppress("UNCHECKED_CAST")
    open fun <R> cast(type: Class<R>): Data<R> {
        if (type.isAssignableFrom(this.type)) {
            return this as Data<R>
        } else {
            throw IllegalArgumentException("Invalid type to upcast data")
        }
    }

    companion object {

        @JvmStatic
        fun <T:Any> buildStatic(content: T, meta: Meta = Meta.empty()): Data<T> {
            //val nonNull = content as? Any ?: throw RuntimeException("Can't create data from null object")
            return Data(content.javaClass, StaticGoal<T>(content), meta)
        }

        @JvmStatic
        fun <T : Any> buildStatic(content: T): Data<T> {
            var meta = Meta.empty()
            if (content is Metoid) {
                meta = (content as Metoid).meta
            }
            return buildStatic(content, meta)
        }

        fun <T> empty(type: Class<T>, meta: Meta): Data<T> {
            val emptyGoal = StaticGoal<T>(null)
            return Data(type, emptyGoal, meta)
        }

        /**
         * Build data from envelope using given lazy binary transformation
         *
         * @param envelope
         * @param type
         * @param transform
         * @param <T>
         * @return
        </T> */
        fun <T> fromEnvelope(envelope: Envelope, type: Class<T>, transform: (Binary) -> T): Data<T> {
            val goal = object : AbstractGoal<T>() {
                @Throws(Exception::class)
                override fun compute(): T {
                    return transform(envelope.data)
                }

                override fun dependencies(): Stream<Goal<*>> {
                    return Stream.empty()
                }
            }
            return Data(type, goal, envelope.meta)
        }

        fun <T> generate(type: Class<T>, meta: Meta, executor: Executor, sup: () -> T): Data<T> {
            return Data(type, GeneratorGoal(executor, Supplier(sup)), meta)
        }

        fun <T> generate(type: Class<T>, meta: Meta, sup: () -> T): Data<T> {
            return Data(type, GeneratorGoal(sup), meta)
        }
    }

}
