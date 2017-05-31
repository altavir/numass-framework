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
package inr.numass.control.cryotemp

import hep.dataforge.control.connections.Roles
import hep.dataforge.control.devices.DeviceFactory
import hep.dataforge.meta.Meta
import inr.numass.control.DeviceViewConnection
import inr.numass.control.NumassControlApplication
import javafx.stage.Stage

/**
 * @author darksnake
 */
class PKT8App : NumassControlApplication<PKT8Device>() {
    override fun buildView(device: PKT8Device): DeviceViewConnection<PKT8Device> {
        return PKT8ViewConnection().apply {
            device.connect(this, Roles.VIEW_ROLE)
        }
    }

    override val deviceFactory: DeviceFactory = PKT8DeviceFactory()

    override fun setupStage(stage: Stage, device: PKT8Device) {
        stage.title = "Numass temperature view " + device.name
        stage.minHeight = 400.0
        stage.minWidth = 400.0
    }

    override fun acceptDevice(meta: Meta): Boolean {
        return meta.getString("type") == "PKT8"
    }


}
