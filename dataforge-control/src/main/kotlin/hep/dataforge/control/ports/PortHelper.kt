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

package hep.dataforge.control.ports

import hep.dataforge.context.Context
import hep.dataforge.context.ContextAware
import hep.dataforge.control.devices.Device
import hep.dataforge.control.devices.dispatchEvent
import hep.dataforge.description.ValueDef
import hep.dataforge.events.EventBuilder
import hep.dataforge.meta.Meta
import hep.dataforge.nullable
import hep.dataforge.states.*
import hep.dataforge.useValue
import hep.dataforge.values.ValueType
import org.slf4j.Logger
import java.time.Duration

@StateDef(value = ValueDef(key = "connected",type = [ValueType.BOOLEAN], def = "false"), writable = true)
class PortHelper(
        val device: Device,
        val builder: ((Context, Meta) -> GenericPortController) = { context, meta -> GenericPortController(context, PortFactory.build(meta)) }
) : Stateful, ContextAware {
    override val logger: Logger
        get() = device.logger

    override val states: StateHolder
        get() = device.states

    override val context: Context
        get() = device.context


    private val Device.portMeta: Meta
        get() = meta.optMeta(PORT_STATE).nullable
                ?: device.meta.optValue(PORT_STATE).map {
                    PortFactory.nameToMeta(it.string)
                }.orElse(Meta.empty())

    var connection: GenericPortController = builder(context, device.portMeta)
        private set

    val connectedState = valueState(CONNECTED_STATE, getter = { connection.port.isOpen }) { old, value ->
        if (old != value) {
            logger.info("State 'connect' changed to $value")
            if (value.boolean) {
                connection.open()
            } else {
                connection.close()
            }
            //connect(value.boolean)
        }
        update(value)
    }

    var connected by connectedState.booleanDelegate

    var debug by valueState(DEBUG_STATE) { old, value ->
        if (old != value) {
            logger.info("Turning debug mode to $value")
            setDebugMode(value.boolean)
        }
        update(value)
    }.booleanDelegate

    var port by metaState(PORT_STATE, getter = { connection.port.toMeta() }) { old, value ->
        if (old != value) {
            setDebugMode(false)
            connectedState.update(false)
            connection.close()
            connection = builder(context, value)
            connection.open()
            setDebugMode(debug)
        }
        update(value)
    }.delegate

    private val defaultTimeout: Duration = Duration.ofMillis(device.meta.getInt("port.timeout", 400).toLong())

    val name get() = device.name

    init {
        states.update(PORT_STATE, connection.port.toMeta())
        device.meta.useValue(DEBUG_STATE) {
            debug = it.boolean
        }
    }

    private fun setDebugMode(debugMode: Boolean) {
        //Add debug listener
        if (debugMode) {
            connection.apply {
                onAnyPhrase("$name[debug]") { phrase -> logger.debug("Device {} received phrase: \n{}", name, phrase) }
                onError("$name[debug]") { message, error -> logger.error("Device {} exception: \n{}", name, message, error) }
            }
        } else {
            connection.apply {
                removePhraseListener("$name[debug]")
                removeErrorListener("$name[debug]")
            }
        }
        states.update(DEBUG_STATE, debugMode)
    }

    fun shutdown() {
        connectedState.set(false)
    }

    fun sendAndWait(request: String, timeout: Duration = defaultTimeout): String {
        return connection.sendAndWait(request, timeout) { true }
    }

    fun sendAndWait(request: String, timeout: Duration = defaultTimeout, predicate: (String) -> Boolean): String {
        return connection.sendAndWait(request, timeout, predicate)
    }

    fun send(message: String) {
        connection.send(message)
        device.dispatchEvent(
                EventBuilder
                        .make(device.name)
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