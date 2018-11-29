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
import hep.dataforge.control.devices.Device
import hep.dataforge.control.devices.DeviceFactory
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaUtils
import inr.numass.control.NumassControlApplication
import javafx.stage.Stage

class EGunApplication: NumassControlApplication<EGun>() {
    override val deviceFactory: DeviceFactory = object :DeviceFactory{
        override val type: String = "numass.lambda"

        override fun build(context: Context, meta: Meta): Device {
            return EGun(context, meta)
        }
    }

    override fun setupStage(stage: Stage, device: EGun) {
        stage.title = "Numass gun control"
    }

    override fun getDeviceMeta(config: Meta): Meta {
        return MetaUtils.findNode(config,"device"){it.getString("type") == "numass.gun"}.orElseThrow{RuntimeException("Gun configuration not found")}
    }
}