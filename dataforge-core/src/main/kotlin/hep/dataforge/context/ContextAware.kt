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
package hep.dataforge.context

import hep.dataforge.Named
import kotlinx.coroutines.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * The interface for something that encapsulated in context
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
interface ContextAware {
    /**
     * Get context for this object
     *
     * @return
     */
    val context: Context

    @JvmDefault
    val logger: Logger
        get() = if (this is Named) {
            LoggerFactory.getLogger(context.name + "." + (this as Named).name)
        } else {
            context.logger
        }
}

fun ContextAware.launch(
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> Unit): Job = this.context.launch(context, start, block)

fun <R> ContextAware.async(
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> R): Deferred<R> = this.context.async(context, start, block)

