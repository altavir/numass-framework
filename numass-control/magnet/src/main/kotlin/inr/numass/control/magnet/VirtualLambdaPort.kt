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
package inr.numass.control.magnet

import hep.dataforge.control.ports.VirtualPort
import hep.dataforge.exceptions.PortException
import hep.dataforge.meta.Meta
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*

/**
 *
 * @author Alexander Nozik
 */
class VirtualLambdaPort(meta: Meta) : VirtualPort(meta) {

    @Volatile private var currentAddress = -1
    private val magnets = HashMap<Int, VirtualMagnetStatus>()
    private val virtualPortName: String = meta.getString("name", "virtual::numass.lambda")

//    constructor(portName: String, magnets: Map<Int, Double>) {
//        this.virtualPortName = portName
//        magnets.forEach { key, value -> this.magnets.put(key, VirtualMagnetStatus(value)) }
//    }
//
//    constructor(portName: String, vararg magnets: Int) {
//        this.virtualPortName = portName
//        for (magnet in magnets) {
//            this.magnets.put(magnet, VirtualMagnetStatus(0.01))
//        }
//    }

    init {
        meta.useEachMeta("magnet") {
            val num = it.getInt("address", 1)
            val resistance = it.getDouble("resistance", 1.0)
            magnets.put(num, VirtualMagnetStatus(resistance))
        }
    }

    override fun toString(): String = virtualPortName

    override fun evaluateRequest(request: String) {
        val command: String
        var value = ""
        val split = request.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (split.size == 1) {
            command = request
        } else {
            command = split[0]
            value = split[1]
        }
        try {
            evaluateRequest(command.trim { it <= ' ' }, value.trim { it <= ' ' })
        } catch (ex: RuntimeException) {

            receivePhrase("FAIL")//TODO какая команда правильная?
            LoggerFactory.getLogger(javaClass).error("Request evaluation failure", ex)
        }

    }

    private fun sendOK() {
        planResponse("OK", latency)
    }

    private fun evaluateRequest(comand: String, value: String) {
        when (comand) {
            "ADR" -> {
                val address = Integer.parseInt(value)
                if (magnets.containsKey(address)) {
                    currentAddress = address
                    sendOK()
                }
                return
            }
            "ADR?" -> {
                planResponse(Integer.toString(currentAddress), latency)
                return
            }
            "OUT" -> {
                val state = Integer.parseInt(value)
                currentMagnet().out = state == 1
                sendOK()
                return
            }
            "OUT?" -> {
                val out = currentMagnet().out
                if (out) {
                    planResponse("ON", latency)
                } else {
                    planResponse("OFF", latency)
                }
                return
            }
            "PC" -> {
                var current = java.lang.Double.parseDouble(value)
                if (current < 0.5) {
                    current = 0.0
                }
                currentMagnet().current = current
                sendOK()
                return
            }
            "PC?" -> {
                planResponse(java.lang.Double.toString(currentMagnet().current), latency)
                return
            }
            "MC?" -> {
                planResponse(java.lang.Double.toString(currentMagnet().current), latency)
                return
            }
            "PV?" -> {
                planResponse(java.lang.Double.toString(currentMagnet().getVoltage()), latency)
                return
            }
            "MV?" -> {
                planResponse(java.lang.Double.toString(currentMagnet().getVoltage()), latency)
                return
            }
            else -> LoggerFactory.getLogger(javaClass).warn("Unknown comand {}", comand)
        }
    }

    private fun currentMagnet(): VirtualMagnetStatus {
        if (currentAddress < 0) {
            throw RuntimeException()
        }
        return magnets[currentAddress]!!
    }

    @Throws(Exception::class)
    override fun close() {

    }

    @Throws(PortException::class)
    override fun open() {

    }

    override fun isOpen(): Boolean = true

    private inner class VirtualMagnetStatus(val resistance: Double,
                                            var on: Boolean = true,
                                            var out: Boolean = false,
                                            var current: Double = 0.0) {

        fun getVoltage() = current * resistance
    }

    companion object {

        private val latency = Duration.ofMillis(50)
    }
}
