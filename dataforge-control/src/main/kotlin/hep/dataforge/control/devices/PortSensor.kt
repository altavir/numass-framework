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

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.control.devices

import hep.dataforge.context.Context
import hep.dataforge.control.devices.PortSensor.Companion.CONNECTED_STATE
import hep.dataforge.control.devices.PortSensor.Companion.DEBUG_STATE
import hep.dataforge.control.ports.GenericPortController
import hep.dataforge.control.ports.PortFactory
import hep.dataforge.description.NodeDef
import hep.dataforge.description.ValueDef
import hep.dataforge.description.ValueDefs
import hep.dataforge.events.EventBuilder
import hep.dataforge.exceptions.ControlException
import hep.dataforge.meta.Meta
import hep.dataforge.nullable
import hep.dataforge.states.*
import hep.dataforge.useValue
import hep.dataforge.values.ValueType.BOOLEAN
import hep.dataforge.values.ValueType.NUMBER
import java.time.Duration

/**
 * A Sensor that uses a Port to obtain data
 *
 * @param <T>
 * @author darksnake
 */
@StateDefs(
    StateDef(
        value = ValueDef(
            key = CONNECTED_STATE,
            type = [BOOLEAN],
            def = "false",
            info = "The connection state for this device"
        ), writable = true
    ),
    //StateDef(value = ValueDef(name = PORT_STATE, info = "The name of the port to which this device is connected")),
    StateDef(
        value = ValueDef(
            key = DEBUG_STATE,
            type = [BOOLEAN],
            def = "false",
            info = "If true, then all received phrases would be shown in the log"
        ), writable = true
    )
)
@MetaStateDef(
    value = NodeDef(
        key = "port",
        descriptor = "method::hep.dataforge.control.ports.PortFactory.build",
        info = "Information about port"
    ), writable = true
)
@ValueDefs(
    ValueDef(key = "timeout", type = arrayOf(NUMBER), def = "400", info = "A timeout for port response in milliseconds")
)
abstract class PortSensor(context: Context, meta: Meta) : Sensor(context, meta) {

    private var _connection: GenericPortController? = null
    protected val connection: GenericPortController
        get() = _connection ?: throw RuntimeException("Not connected")

    val connected = valueState(CONNECTED_STATE, getter = { connection.port.isOpen }) { old, value ->
        if (old != value) {
            logger.info("State 'connect' changed to $value")
            connect(value.boolean)
        }
        update(value)
    }

    var debug by valueState(DEBUG_STATE) { old, value ->
        if (old != value) {
            logger.info("Turning debug mode to $value")
            setDebugMode(value.boolean)
        }
        update(value)
    }.booleanDelegate

    var port by metaState(PORT_STATE, getter = { connection.port.toMeta() }) { old, value ->
        if (old != value) {
            setupConnection(value)
        }
        update(value)
    }.delegate

    private val defaultTimeout: Duration = Duration.ofMillis(meta.getInt("timeout", 400).toLong())

    init {
//        meta.useMeta(PORT_STATE) {
//            port = it
//        }
        meta.useValue(DEBUG_STATE) {
            updateState(DEBUG_STATE, it.boolean)
        }
    }

    private fun setDebugMode(debugMode: Boolean) {
        //Add debug listener
        if (debugMode) {
            connection.apply {
                onAnyPhrase("$name[debug]") { phrase -> logger.debug("Device {} received phrase: \n{}", name, phrase) }
                onError("$name[debug]") { message, error ->
                    logger.error(
                        "Device {} exception: \n{}",
                        name,
                        message,
                        error
                    )
                }
            }
        } else {
            connection.apply {
                removePhraseListener("$name[debug]")
                removeErrorListener("$name[debug]")
            }
        }
        updateState(DEBUG_STATE, debugMode)
    }

    private fun connect(connected: Boolean) {
        if (connected) {
            try {
                if (_connection == null) {
                    logger.debug("Setting up connection using device meta")
                    val initialPort = meta.optMeta(PORT_STATE).nullable
                        ?: meta.optString(PORT_STATE).nullable?.let { PortFactory.nameToMeta(it) }
                        ?: Meta.empty()
                    setupConnection(initialPort)
                }
                connection.open()
                this.connected.update(true)
            } catch (ex: Exception) {
                notifyError("Failed to open connection", ex)
                this.connected.update(false)
            }
        } else {
            _connection?.close()
            _connection = null
            this.connected.update(false)
        }
    }

    protected open fun buildConnection(meta: Meta): GenericPortController {
        val port = PortFactory.build(meta)
        return GenericPortController(context, port)
    }

    private fun setupConnection(portMeta: Meta) {
        _connection?.close()
        this._connection = buildConnection(portMeta)
        setDebugMode(debug)
        updateState(PORT_STATE, portMeta)
    }

    @Throws(ControlException::class)
    override fun shutdown() {
        super.shutdown()
        connected.set(false)
    }

    protected fun sendAndWait(request: String, timeout: Duration = defaultTimeout): String {
        return connection.sendAndWait(request, timeout) { true }
    }

    protected fun sendAndWait(
        request: String,
        timeout: Duration = defaultTimeout,
        predicate: (String) -> Boolean
    ): String {
        return connection.sendAndWait(request, timeout, predicate)
    }

    protected fun send(message: String) {
        connection.send(message)
        dispatchEvent(
            EventBuilder
                .make(name)
                .setMetaValue("request", message)
                .build()
        )
    }

    companion object {
        const val CONNECTED_STATE = "connected"
        const val PORT_STATE = "port"
        const val DEBUG_STATE = "debug"
    }
}
