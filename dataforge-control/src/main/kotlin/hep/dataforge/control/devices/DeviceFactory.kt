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

package hep.dataforge.control.devices

import hep.dataforge.utils.ContextMetaFactory

/**
 * Created by darksnake on 06-May-17.
 */
interface DeviceFactory : ContextMetaFactory<Device> {
    /**
     * The type of the device factory. One factory can supply multiple device classes depending on configuration.
     *
     * @return
     */
    val type: String
}
