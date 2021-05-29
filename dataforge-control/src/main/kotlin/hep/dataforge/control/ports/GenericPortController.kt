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

import hep.dataforge.context.Context
import hep.dataforge.context.ContextAware
import hep.dataforge.context.Global
import hep.dataforge.exceptions.ControlException
import hep.dataforge.exceptions.PortException
import hep.dataforge.utils.ReferenceRegistry
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit


/**
 * A port controller helper that allows both synchronous and asynchronous operations on port
 * @property port the port associated with this controller
 */
open class GenericPortController(
        override val context: Context,
        val port: Port,
        private val phraseCondition: (String) -> Boolean = { it.endsWith("\n") }
) : PortController, AutoCloseable, ContextAware {


    constructor(context: Context, port: Port, delimiter: String) : this(context, port, { it.endsWith(delimiter) })

    private val waiters = ReferenceRegistry<FuturePhrase>()
    private val listeners = ReferenceRegistry<PhraseListener>()
    private val exceptionListeners = ReferenceRegistry<ErrorListener>()
    private val buffer = ByteArrayOutputStream();


    fun open() {
        try {
            port.holdBy(this)
            if (!port.isOpen) {
                port.open()
            }
        } catch (e: PortException) {
            throw RuntimeException("Can't hold the port $port by generic handler", e)
        }
    }

    override val logger: Logger
        get() = LoggerFactory.getLogger("${context.name}.$port")

    override fun accept(byte: Byte) {
        synchronized(port) {
            buffer.write(byte.toInt())
            val string = buffer.toString("UTF8")
            if (phraseCondition(string)) {
                acceptPhrase(string)
                buffer.reset()
            }
        }
    }

    private fun acceptPhrase(message: String) {
        waiters.forEach { waiter -> waiter.acceptPhrase(message) }
        listeners.forEach { listener -> listener.acceptPhrase(message) }
    }

    override fun error(errorMessage: String, error: Throwable) {
        exceptionListeners.forEach { it ->
            context.executors.defaultExecutor.submit {
                try {
                    it.action(errorMessage, error)
                } catch (ex: Exception) {
                    logger.error("Failed to execute error listener action", ex)
                }
            }
        }
    }

    /**
     * Wait for next phrase matching condition and return its result
     *
     * @return
     */
    @JvmOverloads
    fun next(condition: (String) -> Boolean = { true }): CompletableFuture<String> {
        //No need for synchronization since ReferenceRegistry is synchronized
        waiters.removeIf { it.isDone }
        val res = FuturePhrase(condition)
        waiters.add(res)
        return res
    }

    /**
     * Get next phrase matching pattern
     *
     * @param pattern
     * @return
     */
    fun next(pattern: String): CompletableFuture<String> {
        return next { it -> it.matches(pattern.toRegex()) }
    }

    /**
     * Block until specific phrase is received
     *
     * @param timeout
     * @param predicate
     * @return
     * @throws PortException
     */
    @JvmOverloads
    fun waitFor(timeout: Duration, predicate: (String) -> Boolean = { true }): String {
        return next(predicate).get(timeout.toMillis(), TimeUnit.MILLISECONDS)
    }

    /**
     * Hook specific reaction to the specific phrase. Whenever it is possible, it is better to use `weakOnPhrase` to avoid memory leaks due to obsolete listeners.
     *
     * @param condition
     * @param action
     */
    fun onPhrase(condition: (String) -> Boolean, owner: Any? = null, action: (String) -> Unit) {
        val listener = PhraseListener(condition, owner, action)
        listeners.add(listener)
    }

    /**
     * Add weak phrase listener
     *
     * @param condition
     * @param action
     * @return
     */
    fun weakOnPhrase(condition: (String) -> Boolean, owner: Any? = null, action: (String) -> Unit) {
        val listener = PhraseListener(condition, owner, action)
        listeners.add(listener, false)
    }

    fun weakOnPhrase(pattern: String, owner: Any? = null, action: (String) -> Unit) {
        weakOnPhrase({ it.matches(pattern.toRegex()) }, owner, action)
    }

    fun weakOnPhrase(owner: Any? = null, action: (String) -> Unit) {
        weakOnPhrase({ true }, owner, action)
    }

    /**
     * Remove a specific phrase listener
     *
     * @param listener
     */
    fun removePhraseListener(owner: Any) {
        this.listeners.removeIf { it.owner == owner }
    }

    /**
     * Add action to phrase matching specific pattern
     *
     * @param pattern
     * @param action
     * @return
     */
    fun onPhrase(pattern: String, owner: Any? = null, action: (String) -> Unit) {
        onPhrase({ it.matches(pattern.toRegex()) }, owner, action)
    }

    /**
     * Add reaction to any phrase
     *
     * @param action
     * @return
     */
    fun onAnyPhrase(owner: Any? = null, action: (String) -> Unit) {
        onPhrase({ true }, owner, action)
    }

    /**
     * Add error listener
     *
     * @param listener
     * @return
     */
    fun onError(owner: Any? = null, listener: (String, Throwable?) -> Unit) {
        this.exceptionListeners.add(ErrorListener(owner, listener))
    }

    /**
     * Add weak error listener
     *
     * @param listener
     * @return
     */
    fun weakOnError(owner: Any? = null, listener: (String, Throwable?) -> Unit) {
        this.exceptionListeners.add(ErrorListener(owner, listener), false)
    }

    /**
     * remove specific error listener
     *
     */
    fun removeErrorListener(owner: Any) {
        this.exceptionListeners.removeIf { it.owner == owner }
    }

    /**
     * Send async message to port
     *
     * @param message
     */
    fun send(message: ByteArray) {
        try {
            open()
            port.send(this, message)
        } catch (e: PortException) {
            throw RuntimeException("Failed to send message to port $port")
        }

    }

    fun send(message: String, charset: Charset = Charsets.US_ASCII) {
        send(message.toByteArray(charset))
    }

    /**
     * Send and return the future with the result
     *
     * @param message
     * @param condition
     */
    fun sendAndGet(message: String, condition: (String) -> Boolean): CompletableFuture<String> {
        val res = next(condition) // in case of immediate reaction
        send(message)
        return res
    }

    /**
     * Send and block thread until specific result is obtained. All listeners and reactions work as usual.
     *
     * @param message
     * @param timeout
     * @param condition
     * @return
     */
    fun sendAndWait(message: String, timeout: Duration, condition: (String) -> Boolean = { true }): String {
        return sendAndGet(message, condition).get(timeout.toMillis(), TimeUnit.MILLISECONDS)
    }

    /**
     * Cancel all pending waiting actions and release the port. Does not close the port
     */
    @Throws(Exception::class)
    override fun close() {
        close(Duration.ofMillis(1000))
    }

    /**
     * Blocking close operation. Waits at most for timeout to finish all operations and then closes.
     *
     * @param timeout
     */
    @Throws(Exception::class)
    fun close(timeout: Duration) {
        CompletableFuture.allOf(*waiters.toTypedArray()).get(timeout.toMillis(), TimeUnit.MILLISECONDS)
        port.releaseBy(this)
    }

    private inner class FuturePhrase(internal val condition: (String) -> Boolean) : CompletableFuture<String>() {
        internal fun acceptPhrase(phrase: String) {
            if (condition(phrase)) {
                complete(phrase)
            }
        }
    }

    private inner class PhraseListener(private val condition: (String) -> Boolean, val owner: Any? = null, private val action: (String) -> Unit) {

        internal fun acceptPhrase(phrase: String) {
            if (condition(phrase)) {
                context.executors.defaultExecutor.submit {
                    try {
                        action(phrase)
                    } catch (ex: Exception) {
                        logger.error("Failed to execute hooked action", ex)
                    }
                }
            }
        }
    }

    private inner class ErrorListener(val owner: Any? = null, val action: (String, Throwable?) -> Unit)

    companion object {

        /**
         * Use temporary controller to safely send request and receive response
         *
         * @param port
         * @param request
         * @param timeout
         * @return
         * @throws ControlException
         */
        @Throws(ControlException::class)
        fun sendAndWait(port: Port, request: String, timeout: Duration): String {
            try {
                GenericPortController(Global, port).use { controller -> return controller.sendAndWait(request, timeout) { true } }
            } catch (e: Exception) {
                throw ControlException("Failed to close the port", e)
            }

        }
    }
}