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
package hep.dataforge.control.ports

import hep.dataforge.description.ValueDef
import hep.dataforge.description.ValueDefs
import hep.dataforge.exceptions.ControlException
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder
import hep.dataforge.set
import hep.dataforge.utils.MetaFactory
import java.util.*

/**
 *
 * @author [Alexander Nozik](mailto:altavir@gmail.com)
 */
object PortFactory : MetaFactory<Port> {

    private val portMap = HashMap<Meta, Port>()


    @ValueDefs(
            ValueDef(key = "type", def = "tcp", info = "The type of the port"),
            ValueDef(key = "address", required = true, info = "The specific designation of this port according to type"),
            ValueDef(key = "type", def = "tcp", info = "The type of the port")
    )
    override fun build(meta: Meta): Port {
        val protocol = meta.getString("type", "tcp")
        val port = when (protocol) {
            "com" -> {
                if (meta.hasValue("address")) {
                    ComPort(meta.getString("address"), meta)
                } else {
                    throw IllegalArgumentException("Not enough information to create a port")
                }
            }
            "tcp" -> {
                if (meta.hasValue("ip") && meta.hasValue("port")) {
                    TcpPort(meta.getString("ip"), meta.getInt("port"), meta)
                } else {
                    throw IllegalArgumentException("Not enough information to create a port")
                }
            }
            "virtual" -> buildVirtualPort(meta)
            else -> throw ControlException("Unknown protocol")
        }
        return portMap.getOrPut(port.toMeta()) { port }
    }

    private fun buildVirtualPort(meta: Meta): Port {
        val className = meta.getString("class")
        val theClass = Class.forName(className)
        return theClass.getDeclaredConstructor(Meta::class.java).newInstance(meta) as Port
    }

    /**
     * Create new port or reuse existing one if it is already created
     * @param portName
     * @return
     * @throws ControlException
     */
    fun build(portName: String): Port {
        return build(nameToMeta(portName))
    }

    fun nameToMeta(portName: String): Meta {
        val builder = MetaBuilder("port")
                .setValue("name", portName)

        val type = portName.substringBefore("::", "com")
        val address = portName.substringAfter("::")

        builder["type"] = type
        builder["address"] = address

        if (type == "tcp") {
            builder["ip"] = address.substringBefore(":")
            builder["port"] = address.substringAfter(":").toInt()
        }

        return builder.build();
    }
}
