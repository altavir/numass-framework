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

import hep.dataforge.connections.Connection

/**
 * A listener that listens to device state change initialization and shut down
 *
 * @author Alexander Nozik
 */
interface DeviceListener: Connection {

    /**
     * Notify that state of device is changed.
     *
     * @param device
     * @param name   the name of the state
     * @param state
     */
    fun notifyStateChanged(device: Device, name: String, state: Any)

    /**
     *
     * @param device
     * @param message
     * @param exception
     */
    fun evaluateDeviceException(device: Device, message: String, exception: Throwable?) {

    }
}
