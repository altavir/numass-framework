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

import hep.dataforge.context.Context
import hep.dataforge.control.devices.AbstractDevice
import hep.dataforge.control.ports.Port
import hep.dataforge.control.ports.PortFactory
import hep.dataforge.description.ValueDef
import hep.dataforge.exceptions.ControlException
import hep.dataforge.exceptions.PortException
import hep.dataforge.kodex.buildMeta
import hep.dataforge.meta.Meta
import hep.dataforge.states.StateDef
import hep.dataforge.states.StateDefs
import hep.dataforge.states.valueState
import hep.dataforge.utils.DateTimeUtils
import hep.dataforge.values.ValueType.*
import inr.numass.control.DeviceView
import inr.numass.control.magnet.fx.MagnetDisplay
import kotlinx.coroutines.experimental.runBlocking
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.Future
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * @author Polina
 */

@StateDefs(
        StateDef(value = ValueDef(name = "current", type = arrayOf(NUMBER), def = "0", info = "Current current")),
        StateDef(value = ValueDef(name = "voltage", type = arrayOf(NUMBER), def = "0", info = "Current voltage")),
        StateDef(value = ValueDef(name = "outCurrent", type = arrayOf(NUMBER), def = "0", info = "Target current"), writable = true),
        StateDef(value = ValueDef(name = "outVoltage", type = arrayOf(NUMBER), def = "5.0", info = "Target voltage"), writable = true),
        StateDef(value = ValueDef(name = "output", type = arrayOf(BOOLEAN), def = "false", info = "Weather output on or off"), writable = true),
        StateDef(value = ValueDef(name = "lastUpdate", type = arrayOf(TIME), def = "0", info = "Time of the last update"), writable = true),
        StateDef(value = ValueDef(name = "updating", type = arrayOf(BOOLEAN), def = "false", info = "Shows if current ramping in progress"), writable = true),
        StateDef(value = ValueDef(name = "monitoring", type = arrayOf(BOOLEAN), def = "false", info = "Shows if monitoring task is running"), writable = true),
        StateDef(value = ValueDef(name = "speed", type = arrayOf(NUMBER), info = "Current change speed in Ampere per minute"), writable = true),
        StateDef(ValueDef(name = "status", type = [STRING], def = "INIT", enumeration = LambdaMagnet.MagnetStatus::class, info = "Current state of magnet operation"))
)
@DeviceView(MagnetDisplay::class)
class LambdaMagnet(private val controller: LambdaPortController, meta: Meta) : AbstractDevice(controller.context, meta) {

    private var closePortOnShutDown = false

    /**
     * @return the address
     */
    val address: Int = meta.getInt("address", 1)

    override val name: String = meta.getString("name", "LAMBDA_$address")
    private val scheduler = ScheduledThreadPoolExecutor(1)

    //var listener: MagnetStateListener? = null
    //    private volatile double current = 0;

    private var monitorTask: Future<*>? = null
    private var updateTask: Future<*>? = null

    var lastUpdate by valueState("lastUpdate", getter = { 0 }).timeDelegate
        private set

    // read-only values of current output
    val current = valueState("current", getter = { s2d(controller.getParameter(address, "MC")) })

    val voltage = valueState("voltage", getter = { s2d(controller.getParameter(address, "MV")) })

    var target = valueState("target")

    //output values of current and voltage
    private var outCurrent by valueState("outCurrent", getter = { s2d(controller.getParameter(address, "PC")) }) { _, value ->
        if (controller.setParameter(address, "PC", value.doubleValue())) {
            lastUpdate = DateTimeUtils.now()
        } else {
            notifyError("Can't set the target current")
        }
        return@valueState value
    }.doubleDelegate

    private val outVoltage = valueState("outVoltage", getter = { s2d(controller.getParameter(address, "PV")) }) { _, value ->
        if (!controller.setParameter(address, "PV", value.doubleValue())) {
            notifyError("Can't set the target voltage")
        }
        return@valueState value
    }

    val output = valueState("output", getter = { controller.talk(address, "OUT?") == "OK" }) { _, value ->
        setOutputMode(value.booleanValue())
        if (!value.booleanValue()) {
            status = MagnetStatus.OFF
        }
    }

    var monitoring = valueState("monitoring", getter = { monitorTask != null }) { _, value ->
        if (value.booleanValue()) {
            startMonitorTask()
        } else {
            stopMonitorTask()
        }
        return@valueState value
    }

    /**
     *
     */
    var updating = valueState("updating", getter = { updateTask != null }) { _, value ->
        if (value.booleanValue()) {
            startUpdateTask()
        } else {
            stopUpdateTask()
        }
        return@valueState value
    }


    /**
     * current change speed in Ampere per minute
     *
     * @param speed
     */
    var speed by valueState("speed").doubleDelegate


    var status by valueState("status").enumDelegate<MagnetStatus>()
        private set

    /**
     * The binding limit for magnet current
     */
    var bound: (Double) -> Boolean = { it < meta.getDouble("maxCurrent") }

    /**
     * A setup for single magnet controller
     *
     * @param context
     * @param meta
     * @throws ControlException
     */
    @Throws(ControlException::class)
    constructor(context: Context, meta: Meta) : this(LambdaPortController(context, PortFactory.build(meta.getString("port"))), meta) {
        closePortOnShutDown = true
    }

    constructor(context: Context, port: Port, address: Int) : this(LambdaPortController(context, port), buildMeta { "address" to address })

    @Throws(ControlException::class)
    override fun init() {
        super.init()
        controller.open()
    }

    @Throws(ControlException::class)
    override fun shutdown() {
        super.shutdown()
        try {
            if (closePortOnShutDown) {
                controller.close()
                controller.port.close()
            }
        } catch (ex: Exception) {
            throw ControlException("Failed to close the port", ex)
        }

    }

    /**
     * Extract number from LAMBDA response
     *
     * @param str
     * @return
     */
    private fun s2d(str: String): Double = java.lang.Double.valueOf(str)

    /**
     * Cancel current update task
     */
    fun stopUpdateTask() {
        updateTask?.cancel(false)
    }

    /**
     * Start recursive updates of current with given delays between updates. If
     * delay is 0 then updates are made immediately.
     *
     * @param targetI
     * @param delay
     */

    private fun startUpdateTask(delay: Int = DEFAULT_DELAY) {
        assert(delay > 0)
        stopUpdateTask()
        val call = {
            try {
                val measuredI = current.doubleValue
                val targetI = target.doubleValue
                updateState("current", measuredI)
                if (Math.abs(measuredI - targetI) > CURRENT_PRECISION) {
                    val nextI = nextI(measuredI, targetI)
                    if (bound(nextI)) {
                        outCurrent = nextI
                        status = MagnetStatus.OK
                    } else {
                        status = MagnetStatus.BOUND
                    }
                } else {
                    stopUpdateTask()
                }

            } catch (ex: PortException) {
                notifyError("Error in update task", ex)
                stopUpdateTask()
            }
        }

        updateTask = scheduler.scheduleWithFixedDelay(call, 0, delay.toLong(), TimeUnit.MILLISECONDS)
        updateState("updating", true)
    }

    @Throws(PortException::class)
    private fun setOutputMode(out: Boolean) {
        val outState: Int = if (out) 1 else 0
        if (!controller.setParameter(address, "OUT", outState)) {
            notifyError("Can't set output mode")
        } else {
            updateState("output", out)
        }
    }

    private fun nextI(measuredI: Double, targetI: Double): Double {

        var step = if (lastUpdate == Instant.EPOCH) {
            MIN_UP_STEP_SIZE
        } else {
            //Choose optimal speed but do not exceed maximum speed
            Math.min(MAX_STEP_SIZE, lastUpdate.until(DateTimeUtils.now(), ChronoUnit.MILLIS).toDouble() / 60000.0 * speed)
        }

        val res = if (targetI > measuredI) {
            step = Math.max(MIN_UP_STEP_SIZE, step)
            Math.min(targetI, measuredI + step)
        } else {
            step = Math.max(MIN_DOWN_STEP_SIZE, step)
            Math.max(targetI, measuredI - step)
        }

        // не вводится ток меньше 0.5
        return if (res < 0.5 && targetI > CURRENT_PRECISION) {
            0.5
        } else if (res < 0.5 && targetI < CURRENT_PRECISION) {
            0.0
        } else {
            res
        }
    }

    /**
     * Cancel current monitoring task
     */
    private fun stopMonitorTask() {
        monitorTask?.let {
            it.cancel(true)
            monitorTask = null
        }
    }

    /**
     * Start monitoring task which checks for magnet status and then waits for
     * fixed time.
     *
     * @param delay an interval between scans in milliseconds
     */
    private fun startMonitorTask(delay: Int = DEFAULT_MONITOR_DELAY) {
        assert(delay >= 1000)
        stopMonitorTask()


        val call = Runnable {
            try {
                runBlocking {
                    states["voltage"]?.read()
                    states["current"]?.read()
                }
            } catch (ex: PortException) {
                notifyError("Port connection exception during status measurement", ex)
                stopMonitorTask()
            }
        }

        monitorTask = scheduler.scheduleWithFixedDelay(call, 0, delay.toLong(), TimeUnit.MILLISECONDS)

    }

    /**
     * Add symmetric non-blocking conditions to ensure currents in two magnets have difference within given value.
     * @param controller
     * @param difference
     */
    fun bindTo(controller: LambdaMagnet, difference: Double) {
        this.bound = { i -> Math.abs(controller.current.doubleValue - i) <= difference }
        controller.bound = { i -> Math.abs(this.current.doubleValue - i) <= difference }
    }

    enum class MagnetStatus {
        INIT, // no information
        OFF, // Magnet output is off
        OK, // Magnet ouput is on
        ERROR, // Some error
        BOUND // Magnet in bound mode
    }

    companion object {

        const val CURRENT_PRECISION = 0.05
        const val DEFAULT_DELAY = 1
        const val DEFAULT_MONITOR_DELAY = 2000
        const val MAX_STEP_SIZE = 0.2
        const val MIN_UP_STEP_SIZE = 0.005
        const val MIN_DOWN_STEP_SIZE = 0.05
        const val MAX_SPEED = 5.0 // 5 A per minute

    }
}

