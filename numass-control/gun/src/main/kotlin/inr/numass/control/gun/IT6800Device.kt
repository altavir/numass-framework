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

package inr.numass.control.gun

import hep.dataforge.context.Context
import hep.dataforge.control.devices.AbstractDevice
import hep.dataforge.control.ports.PortHelper
import hep.dataforge.meta.Meta
import hep.dataforge.states.valueState
import tornadofx.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.experimental.and


class IT6800Device(context: Context, meta: Meta) : AbstractDevice(context, meta) {
    private val portHelper = PortHelper(this)

    val connected get() = portHelper.connected

    val address: Byte = meta.getValue("address", 0).number.toByte()

    val remoteState = valueState("remote",
            setter = { value -> sendBoolean(Command.REMOTE.code, value.boolean) }
    )

    val outputState = valueState("output",
            setter = { value -> sendBoolean(Command.OUTPUT.code, value.boolean) }
    )

    val voltageState = valueState("voltage",
            setter = { value -> sendInt(Command.VOLTAGE.code, (value.double * 1000).toInt()) }
    )

    val currentState = valueState("current",
            setter = { value -> sendShort(Command.CURRENT.code, (value.double * 1000).toInt().toShort()) }
    )

    fun connect() {
        connected.set(true)
        portHelper.connection.onAnyPhrase(this) {
            val buffer = ByteBuffer.wrap(it.toByteArray(Charsets.US_ASCII))
            buffer.order(ByteOrder.LITTLE_ENDIAN)

            if (buffer.get(1) != address) {
                //skip
                return@onAnyPhrase
            }

            val code = buffer.get(2)
            when (code) {
                Command.REMOTE.code -> {
                    val value = buffer[3] > 0
                    remoteState.update(value)
                }
                Command.OUTPUT.code -> {
                    val value = buffer[3] > 0
                    outputState.update(value)
                }
                Command.VOLTAGE.code -> {
                    val value = buffer.getInt(3)
                    voltageState.update(value.toDouble() / 1000)
                }
                Command.CURRENT.code -> {
                    val value = buffer.getShort(3)
                    currentState.update(value.toDouble() / 1000)
                }
                Command.READ.code -> {
                    val current = buffer.getShort(3)
                    currentState.update(current.toDouble() / 1000)
                    val value = buffer.getInt(5)
                    voltageState.update(value.toDouble() / 1000)
                    val state = buffer.get(9)
                    outputState.update(state and 1 > 0)
                    remoteState.update(state.toInt() ushr 7 and 1 >0)
                }
            }
        }
    }

    private fun request(command: Byte, data: ByteBuffer): String {
        if (data.limit() != 21) error("Wrong size of data array")
        val buffer = ByteBuffer.allocate(26)
        buffer.put(0, START)
        buffer.put(1, address)
        buffer.put(2, command)
        buffer.position(3)
        buffer.put(data)
        val checksum = (START + address + command + data.array().sum()).rem(256).toByte()
        buffer.put(25, checksum)
        return String(buffer.array(), Charsets.US_ASCII)
    }

    private fun sendBoolean(command: Byte, value: Boolean) {
        val data = ByteBuffer.allocate(21)
        data.put(0, if (value) 1 else 0)
        portHelper.send(request(command, data))
    }

    private fun sendShort(command: Byte, value: Short) {
        val data = ByteBuffer.allocate(21)
        data.order(ByteOrder.LITTLE_ENDIAN)
        data.putShort(0, value)
        portHelper.send(request(command, data))
    }

    private fun sendInt(command: Byte, value: Int) {
        val data = ByteBuffer.allocate(21)
        data.order(ByteOrder.LITTLE_ENDIAN)
        data.putInt(0, value)
        portHelper.send(request(command, data))
    }

    override fun shutdown() {
        portHelper.shutdown()
        super.shutdown()
    }

    enum class Command(val code: Byte) {
        REMOTE(0x20),
        OUTPUT(0x21),
        VOLTAGE(0x23),
        CURRENT(0x24),
        READ(0x26),
        INFO(0x31)
    }

    companion object {
        private const val START = (170).toByte() // AA
    }
}