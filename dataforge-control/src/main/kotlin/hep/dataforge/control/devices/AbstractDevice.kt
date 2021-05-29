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
package hep.dataforge.control.devices

import hep.dataforge.connections.Connection
import hep.dataforge.connections.ConnectionHelper
import hep.dataforge.context.Context
import hep.dataforge.context.Global
import hep.dataforge.control.connections.Roles
import hep.dataforge.control.devices.Device.Companion.INITIALIZED_STATE
import hep.dataforge.description.ValueDef
import hep.dataforge.events.Event
import hep.dataforge.events.EventHandler
import hep.dataforge.exceptions.ControlException
import hep.dataforge.listAnnotations
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaHolder
import hep.dataforge.names.AnonymousNotAlowed
import hep.dataforge.states.*
import hep.dataforge.values.ValueType
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.time.Duration
import java.util.concurrent.*

/**
 *
 *
 * State has two components: physical and logical. If logical state does not
 * coincide with physical, it should be invalidated and automatically updated on
 * next request.
 *
 *
 * @author Alexander Nozik
 */
@AnonymousNotAlowed
@StateDef(
    value = ValueDef(
        key = INITIALIZED_STATE,
        type = [ValueType.BOOLEAN],
        def = "false",
        info = "Initialization state of the device"
    ), writable = true
)
abstract class AbstractDevice(override final val context: Context = Global, meta: Meta) : MetaHolder(meta), Device {

    final override val states = StateHolder()

    val initializedState: ValueState = valueState(INITIALIZED_STATE) { old, value ->
        if (old != value) {
            if (value.boolean) {
                init()
            } else {
                shutdown()
            }
        }
    }

    /**
     * Initialization state
     */
    val initialized by initializedState.booleanDelegate

    private var stateListenerJob: Job? = null

    private val _connectionHelper: ConnectionHelper by lazy { ConnectionHelper(this) }

    override fun getConnectionHelper(): ConnectionHelper {
        return _connectionHelper
    }

    /**
     * A single thread executor for this device. All state changes and similar work must be done on this thread.
     *
     * @return
     */
    protected val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { r ->
        val res = Thread(r)
        res.name = "device::$name"
        res.priority = Thread.MAX_PRIORITY
        res.isDaemon = true
        res
    }

    init {
        //initialize states
        javaClass.listAnnotations(StateDef::class.java, true).forEach {
            states.init(ValueState(it))
        }
        javaClass.listAnnotations(MetaStateDef::class.java, true).forEach {
            states.init(MetaState(it))
        }
    }

    @Throws(ControlException::class)
    override fun init() {
        logger.info("Initializing device '{}'...", name)
        states.update(INITIALIZED_STATE, true)
        stateListenerJob = context.launch {
            val flow = states.changes()
            flow.collect {
                onStateChange(it.first, it.second)
            }
        }
    }

    @Throws(ControlException::class)
    override fun shutdown() {
        logger.info("Shutting down device '{}'...", name)
        forEachConnection(Connection::class.java) { c ->
            try {
                c.close()
            } catch (e: Exception) {
                logger.error("Failed to close connection", e)
            }
        }
        states.update(INITIALIZED_STATE, false)
        stateListenerJob?.cancel()
        executor.shutdown()
    }


    override val name: String
        get() = meta.getString("name", type)

    protected fun runOnDeviceThread(runnable: () -> Unit): Future<*> {
        return executor.submit(runnable)
    }

    protected fun <T> callOnDeviceThread(callable: () -> T): Future<T> {
        return executor.submit(callable)
    }

    protected fun scheduleOnDeviceThread(delay: Duration, runnable: () -> Unit): ScheduledFuture<*> {
        return executor.schedule(runnable, delay.toMillis(), TimeUnit.MILLISECONDS)
    }

    protected fun repeatOnDeviceThread(
        interval: Duration,
        delay: Duration = Duration.ZERO,
        runnable: () -> Unit
    ): ScheduledFuture<*> {
        return executor.scheduleWithFixedDelay(runnable, delay.toMillis(), interval.toMillis(), TimeUnit.MILLISECONDS)
    }


    /**
     * Override to apply custom internal reaction of state change
     */
    protected open fun onStateChange(stateName: String, value: Any) {
        forEachConnection(Roles.DEVICE_LISTENER_ROLE, DeviceListener::class.java) {
            it.notifyStateChanged(this, stateName, value)
        }
    }

    override val type: String
        get() = meta.getString("type", "unknown")

    protected fun updateState(stateName: String, value: Any?) {
        states.update(stateName, value)
    }
}


val Device.initialized: Boolean
    get() {
        return if (this is AbstractDevice) {
            this.initialized
        } else {
            this.states
                .filter { it.name == INITIALIZED_STATE }
                .filterIsInstance(ValueState::class.java).firstOrNull()?.value?.boolean ?: false
        }
    }

fun Device.notifyError(message: String, error: Throwable? = null) {
    logger.error(message, error)
    forEachConnection(DeviceListener::class.java) {
        it.evaluateDeviceException(this, message, error)
    }
}

fun Device.dispatchEvent(event: Event) {
    forEachConnection(EventHandler::class.java) { it -> it.pushEvent(event) }
}