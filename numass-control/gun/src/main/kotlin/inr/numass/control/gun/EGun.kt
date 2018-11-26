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
import hep.dataforge.context.ContextAware
import hep.dataforge.control.devices.AbstractDevice
import hep.dataforge.control.devices.Device
import hep.dataforge.control.devices.DeviceHub
import hep.dataforge.meta.Meta
import hep.dataforge.meta.Metoid
import hep.dataforge.names.Name
import hep.dataforge.optional
import java.util.*

class EGun(context: Context, meta: Meta) : AbstractDevice(context, meta), DeviceHub, ContextAware, Metoid {
    val sources: List<IT6800Device> by lazy {
        meta.getMetaList("source").map {IT6800Device(context, it) }
    }

    override val deviceNames: List<Name> by lazy{ sources.map { Name.of(it.name) }}

    override fun optDevice(name: Name): Optional<Device> {
        return sources.find { it.name == name.toString() }.optional
    }
}