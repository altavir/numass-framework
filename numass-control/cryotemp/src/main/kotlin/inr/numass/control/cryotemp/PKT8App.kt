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

import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaUtils
import inr.numass.control.NumassControlApplication
import javafx.stage.Stage

/**
 * @author darksnake
 */
class PKT8App : NumassControlApplication<PKT8Device>() {

    override val deviceFactory = PKT8DeviceFactory()

    override fun setupStage(stage: Stage, device: PKT8Device) {
        stage.title = "Numass temperature view " + device.name
        stage.minHeight = 400.0
        stage.minWidth = 400.0
    }

    override fun getDeviceMeta(config: Meta): Meta {
        return MetaUtils.findNode(config,"device"){it.getString("name") == "numass.temp"}.orElseThrow{RuntimeException("Temperature measurement configuration not found")}
    }
}
