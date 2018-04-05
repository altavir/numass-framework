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
import hep.dataforge.kodex.useEachMeta
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
    override val name: String = meta.getString("name", "virtual::numass.lambda")

    init {
        meta.useEachMeta("magnet") {
            val num = it.getInt("address", 1)
            val resistance = it.getDouble("resistance", 1.0)
            magnets[num] = VirtualMagnetStatus(resistance)
        }
    }

    override fun toString(): String = name

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
            receive("FAIL".toByteArray())//TODO какая команда правильная?
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
                planResponse(java.lang.Double.toString(currentMagnet().voltage), latency)
                return
            }
            "MV?" -> {
                planResponse(java.lang.Double.toString(currentMagnet().voltage), latency)
                return
            }
            else -> LoggerFactory.getLogger(javaClass).warn("Unknown command {}", comand)
        }
    }

    private fun currentMagnet(): VirtualMagnetStatus {
        if (currentAddress < 0) {
            throw RuntimeException()
        }
        return magnets[currentAddress]!!
    }

    private inner class VirtualMagnetStatus(val resistance: Double,
                                            var on: Boolean = true,
                                            var out: Boolean = false,
                                            var current: Double = 0.0) {

        val voltage get() = current * resistance
    }

    companion object {
        private val latency = Duration.ofMillis(50)
    }
}
