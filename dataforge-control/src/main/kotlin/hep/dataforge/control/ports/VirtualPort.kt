/*
 * Copyright  2017 Alexander Nozik.
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
package hep.dataforge.control.ports

import hep.dataforge.exceptions.PortException
import hep.dataforge.meta.Configurable
import hep.dataforge.meta.Configuration
import hep.dataforge.meta.Meta
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.Duration
import java.util.concurrent.CopyOnWriteArraySet
import java.util.function.Supplier

/**
 * @author Alexander Nozik
 */
abstract class VirtualPort protected constructor(meta: Meta) : Port(), Configurable {

    private val futures = CopyOnWriteArraySet<TaggedFuture>()
    override var isOpen = false
    var meta = Configuration(meta)
    protected open val delimeter = meta.getString("delimenter", "\n")

    @Throws(PortException::class)
    override fun open() {
        //scheduler = Executors.newScheduledThreadPool(meta.getInt("numThreads", 4))
        isOpen = true
    }

    override fun getConfig(): Configuration {
        return meta
    }

    override fun configure(config: Meta): Configurable {
        meta.update(config)
        return this
    }

    override fun toString(): String {
        return meta.getString("id", javaClass.simpleName)
    }

    @Throws(PortException::class)
    public override fun send(message: ByteArray) {
        evaluateRequest(String(message, Charsets.US_ASCII))
    }

    /**
     * The device logic here
     *
     * @param request
     */
    protected abstract fun evaluateRequest(request: String)

    @Synchronized
    protected fun clearCompleted() {
        futures.stream().filter { future -> future.future.isCompleted }.forEach { futures.remove(it) }
    }

    @Synchronized
    protected fun cancelByTag(tag: String) {
        futures.stream().filter { future -> future.hasTag(tag) }.forEach { it.cancel() }
    }

    /**
     * Plan the response with given delay
     *
     * @param response
     * @param delay
     * @param tags
     */
    @Synchronized
    protected fun planResponse(response: String, delay: Duration, vararg tags: String) {
        clearCompleted()
        val future = launch {
            kotlinx.coroutines.time.delay(delay)
            receive((response + delimeter).toByteArray())
        }
        this.futures.add(TaggedFuture(future, *tags))
    }

    @Synchronized
    protected fun planRegularResponse(responseBuilder: Supplier<String>, delay: Duration, period: Duration, vararg tags: String) {
        clearCompleted()
        val future = launch {
            kotlinx.coroutines.time.delay(delay)
            while (true) {
                receive((responseBuilder.get() + delimeter).toByteArray())
                kotlinx.coroutines.time.delay(period)
            }
        }
        this.futures.add(TaggedFuture(future, *tags))
    }

    @Throws(Exception::class)
    override fun close() {
        futures.clear()
        isOpen = false
        super.close()
    }

    private inner class TaggedFuture(internal val future: Job, vararg tags: String) {
        internal val tags = setOf(*tags)

        fun hasTag(tag: String): Boolean {
            return tags.contains(tag)
        }

        fun cancel() {
            return future.cancel()
        }
    }
}
