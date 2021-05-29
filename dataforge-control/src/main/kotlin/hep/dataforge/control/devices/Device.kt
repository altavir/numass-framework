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

import hep.dataforge.Named
import hep.dataforge.connections.AutoConnectible
import hep.dataforge.connections.Connection.EVENT_HANDLER_ROLE
import hep.dataforge.connections.Connection.LOGGER_ROLE
import hep.dataforge.connections.RoleDef
import hep.dataforge.connections.RoleDefs
import hep.dataforge.context.ContextAware
import hep.dataforge.control.connections.Roles.DEVICE_LISTENER_ROLE
import hep.dataforge.control.connections.Roles.VIEW_ROLE
import hep.dataforge.events.EventHandler
import hep.dataforge.exceptions.ControlException
import hep.dataforge.meta.Metoid
import hep.dataforge.states.Stateful
import org.slf4j.Logger


/**
 * The Device is general abstract representation of any physical or virtual
 * apparatus that can interface with data acquisition and control system.
 *
 *
 * The device has following important features:
 *
 *
 *  *
 * **States:** each device has a number of states that could be
 * accessed by `getState` method. States could be either stored as some
 * internal variables or calculated on demand. States calculation is
 * synchronous!
 *
 *  *
 * **Listeners:** some external class which listens device state
 * changes and events. By default listeners are represented by weak references
 * so they could be finalized any time if not used.
 *  *
 * **Connections:** any external device connectors which are used
 * by device. The difference between listener and connection is that device is
 * obligated to notify all registered listeners about all changes, but
 * connection is used by device at its own discretion. Also usually only one
 * connection is used for each single purpose.
 *
 *
 *
 * @author Alexander Nozik
 */
@RoleDefs(
        RoleDef(name = DEVICE_LISTENER_ROLE, objectType = DeviceListener::class, info = "A device listener"),
        RoleDef(name = LOGGER_ROLE, objectType = Logger::class, unique = true, info = "The logger for this device"),
        RoleDef(name = EVENT_HANDLER_ROLE, objectType = EventHandler::class, info = "The listener for device events"),
        RoleDef(name = VIEW_ROLE)
)
interface Device : AutoConnectible, Metoid, ContextAware, Named, Stateful {

    /**
     * Device type
     *
     * @return
     */
    val type: String

    @JvmDefault
    override val logger: Logger
        get() = optConnection(LOGGER_ROLE, Logger::class.java).orElse(context.logger)

    /**
     * Initialize device and check if it is working but do not start any
     * measurements or issue commands. Init method could be called only once per
     * MeasurementDevice object. On second call it throws exception or does
     * nothing.
     *
     * @throws ControlException
     */
    @Throws(ControlException::class)
    fun init()

    /**
     * Release all resources locked during init. No further work with device is
     * possible after shutdown. The init method called after shutdown can cause
     * exceptions or incorrect work.
     *
     * @throws ControlException
     */
    @Throws(ControlException::class)
    fun shutdown()


    companion object {
        const val INITIALIZED_STATE = "initialized"
    }
}
