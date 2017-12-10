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
import hep.dataforge.control.devices.StateDef
import hep.dataforge.control.devices.StateDefs
import hep.dataforge.control.ports.PortFactory
import hep.dataforge.control.ports.PortTimeoutException
import hep.dataforge.description.ValueDef
import hep.dataforge.exceptions.ControlException
import hep.dataforge.exceptions.PortException
import hep.dataforge.meta.Meta
import hep.dataforge.utils.DateTimeUtils
import hep.dataforge.values.Value
import hep.dataforge.values.ValueType.*
import hep.dataforge.control.devices.booleanState
import hep.dataforge.control.devices.doubleState
import hep.dataforge.control.devices.timeState
import org.slf4j.LoggerFactory
import java.text.DecimalFormat
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.concurrent.Future
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * @author Polina
 */
@ValueDef(name = "timeout", type = arrayOf(NUMBER), def = "400", info = "A timeout for port response")
@StateDefs(
        StateDef(value = ValueDef(name = "current", type = arrayOf(NUMBER), def = "0", info = "Current current")),
        StateDef(value = ValueDef(name = "voltage", type = arrayOf(NUMBER), def = "0", info = "Current voltage")),
        StateDef(value = ValueDef(name = "targetCurrent", type = arrayOf(NUMBER), def = "0", info = "Target current"), writable = true),
        StateDef(value = ValueDef(name = "targetVoltage", type = arrayOf(NUMBER), def = "5.0", info = "Target voltage"), writable = true),
        StateDef(value = ValueDef(name = "output", type = arrayOf(BOOLEAN), def = "false", info = "Weather output on or off"), writable = true),
        StateDef(value = ValueDef(name = "lastUpdate", type = arrayOf(TIME), def = "0", info = "Time of the last update"), writable = true),
        StateDef(value = ValueDef(name = "updating", type = arrayOf(BOOLEAN), def = "false", info = "Shows if current ramping in progress"), writable = true),
        StateDef(value = ValueDef(name = "monitoring", type = arrayOf(BOOLEAN), def = "false", info = "Shows if monitoring task is running"), writable = true),
        StateDef(value = ValueDef(name = "speed", type = arrayOf(NUMBER), info = "Current change speed in Amper per minute"), writable = true)
)
open class LambdaMagnet(context: Context, meta: Meta, private val controller: LambdaPortController) : AbstractDevice(context, meta) {

    private var closePortOnShutDown = false

    private val name: String = meta.getString("name", "LAMBDA")
    /**
     * @return the address
     */
    val address: Int = meta.getInt("address", 1)!!
    private val scheduler = ScheduledThreadPoolExecutor(1)

    var listener: MagnetStateListener? = null
    //    private volatile double current = 0;
    private val timeout: Duration = meta.optString("timeout").map<Duration> { Duration.parse(it) }.orElse(Duration.ofMillis(200))
    private var monitorTask: Future<*>? = null
    private var updateTask: Future<*>? = null

    var lastUpdate by timeState()
        private set

    val current by doubleState()
    val voltage by doubleState()
    var targetCurrent by doubleState()
    var targetVoltage by doubleState()
    var output by booleanState()
    var monitoring by booleanState()


    /**
     * current change speed in Amper per minute
     *
     * @param speed
     */
    var speed = MAX_SPEED

    /**
     * A setup for single magnet controller
     *
     * @param context
     * @param meta
     * @throws ControlException
     */
    @Throws(ControlException::class)
    constructor(context: Context, meta: Meta) : this(context, meta, LambdaPortController(context, PortFactory.getPort(meta.getString("port")))) {
        closePortOnShutDown = true
    }


    override fun computeState(stateName: String): Any {
       return when (stateName) {
            "current" -> controller.talk(address, timeout) { s2d(getParameter("MC")) }
            "voltage" -> controller.talk(address, timeout) { s2d(getParameter("MV")) }
            "targetCurrent" -> controller.talk(address, timeout) { s2d(getParameter("PC")) }
            "targetVoltage" -> controller.talk(address, timeout) { s2d(getParameter("PV")) }
            "output" -> controller.talk(address, timeout) { talk("OUT?") == "OK" }
            "monitoring" -> monitorTask != null
            "updating" -> updateTask != null
            else -> getLogicalState(stateName)
        }
    }

    override fun requestStateChange(stateName: String, value: Value) {
        when (stateName) {
            "targetCurrent" -> {
                if (setParameter("PC", value.doubleValue())) {
                    lastUpdate = DateTimeUtils.now()
                } else {
                    reportError("Can't set the target current", null)
                }
            }
            "targetVoltage" -> {
                if (!setParameter("PV", value.doubleValue())) {
                    reportError("Can't set the target voltage", null)
                }
            }
            "updating" -> if (value.booleanValue()) {
                startUpdateTask()
            } else {
                stopUpdateTask()
            }
            "monitoring" -> if (value.booleanValue()) {
                startMonitorTask()
            } else {
                stopMonitorTask()
            }
            "output" -> setOutputMode(value.booleanValue())
            else -> setLogicalState(stateName, value);
        }
    }

//    protected open fun setCurrent(current: Double) {
//
//        if (!setParameter("PC", current)) {
//            reportError("Can't set the current", null)
//        } else {
//            lastUpdate = DateTimeUtils.now()
//        }
//    }
    /**
    //     * Gets status of magnet for current moment
    //     *
    //     * @return status of magnet
    //     */
//    private val status: MagnetStatus
//        @Throws(PortException::class)
//        get() {
//            return controller.talk(address, timeout) {
//                val out: Boolean = "ON" == talk("OUT?")
//
//                val measuredCurrent = s2d(getParameter("MC"))
//                updateLogicalState("current", measuredCurrent)
//                val setCurrent = s2d(getParameter("PC"))
//                val measuredVoltage = s2d(getParameter("MV"))
//                val setVoltage = s2d(getParameter("PV"))
//
//                MagnetStatus(out, measuredCurrent, setCurrent, measuredVoltage, setVoltage).also {
//                    listener?.acceptStatus(getName(), it)
//                }
//            }
//        }
//


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

    private fun reportError(errorMessage: String, error: Throwable?) {
        listener?.error(getName(), errorMessage, error) ?: LoggerFactory.getLogger(javaClass).error(errorMessage, error)

    }

    @Throws(PortException::class)
    private fun talk(request: String): String {
        try {
            controller.send(request + "\r")
            return controller.waitFor(timeout).trim()
        } catch (tex: PortTimeoutException) {
            //Single retry on timeout
            LoggerFactory.getLogger(javaClass).warn("A timeout exception for request '$request'. Making another attempt.")
            controller.send(request + "\r")
            return controller.waitFor(timeout).trim()
        }
    }

    private fun update(key: String, value: String) {
        when (key) {
            "OUT" -> updateLogicalState("output", value == "ON")
            "MC" -> updateLogicalState("current", s2d(value))
            "PC" -> updateLogicalState("targetCurrent", s2d(value))
            "MV" -> updateLogicalState("voltage", s2d(value))
            "PV" -> updateLogicalState("targetVoltage", s2d(value))
        }
    }

    @Throws(PortException::class)
    private fun getParameter(name: String): String = talk(name + "?")

    @Throws(PortException::class)
    private fun setParameter(key: String, state: String): Boolean = "OK" == talk("$key $state")

    @Throws(PortException::class)
    private fun setParameter(key: String, state: Int): Boolean = setParameter(key, state.toString())

    @Throws(PortException::class)
    private fun setParameter(key: String, state: Double): Boolean = setParameter(key, d2s(state))

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
        updateTask?.let {
            it.cancel(false)
            lastUpdate = null
            listener?.updateTaskStateChanged(getName(), false)
        }
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
                val measuredI = current ?: 0.0
                val targetI = targetCurrent ?: 0.0
                listener?.acceptMeasuredI(getName(), measuredI)
                if (Math.abs(measuredI - targetI) > CURRENT_PRECISION) {
                    val nextI = nextI(measuredI, targetI)
                    listener?.acceptNextI(getName(), nextI)
                    targetCurrent = nextI
                } else {
                    stopUpdateTask()
                }

            } catch (ex: PortException) {
                reportError("Error in update task", ex)
                stopUpdateTask()
            }
        }

        updateTask = scheduler.scheduleWithFixedDelay(call, 0, delay.toLong(), TimeUnit.MILLISECONDS)
        listener?.updateTaskStateChanged(getName(), true)
        updateLogicalState("updating", Value.of(true))
    }

    @Throws(PortException::class)
    private fun setOutputMode(out: Boolean) {
        val outState: Int = if (out) 1 else 0
        if (!setParameter("OUT", outState)) {
            listener?.error(getName(), "Can't set output mode", null)
        } else {
            requestStateChange("output", Value.of(out))
            listener?.outputModeChanged(getName(), out)
        }
    }

    private fun nextI(measuredI: Double, targetI: Double): Double {
//        assert(measuredI != targetI)

        var step = if (lastUpdate == null) {
            MIN_UP_STEP_SIZE
        } else {
            //Choose optimal speed but do not exceed maximum speed
            Math.min(MAX_STEP_SIZE,
                    lastUpdate!!.until(DateTimeUtils.now(), ChronoUnit.MILLIS).toDouble() / 60000.0 * speed)
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
            listener?.monitorTaskStateChanged(getName(), false)
            monitorTask = null
        }
        updateLogicalState("output", Value.of(false))
    }

    override fun getName(): String {
        return if (this.name.isEmpty()) {
            "LAMBDA " + address
        } else {
            this.name
        }
    }

    /**
     * Start monitoring task which checks for magnet status and then waits for
     * fixed time.
     *
     * @param delay an interval between scans in milliseconds
     */
    @JvmOverloads
    fun startMonitorTask(delay: Int = DEFAULT_MONITOR_DELAY) {
        assert(delay >= 1000)
        stopMonitorTask()

        val call = Runnable {
            try {
                status
            } catch (ex: PortException) {
                reportError("Port connection exception during status measurement", ex)
                stopMonitorTask()
            }
        }

        monitorTask = scheduler.scheduleWithFixedDelay(call, 0, delay.toLong(), TimeUnit.MILLISECONDS)
        listener?.monitorTaskStateChanged(getName(), true)

    }

//    fun request(message: String): String? {
//        try {
//            if (!setADR()) {
//                throw RuntimeException("F")
//            }
//            return talk(message)
//        } catch (ex: PortException) {
//            reportError("Can not send message to the port", ex)
//            return null
//        }
//
//    }

    companion object {

        private val LAMBDA_FORMAT = DecimalFormat("###.##")
        const val CURRENT_PRECISION = 0.05
        const val DEFAULT_DELAY = 1
        const val DEFAULT_MONITOR_DELAY = 2000
        const val MAX_STEP_SIZE = 0.2
        const val MIN_UP_STEP_SIZE = 0.005
        const val MIN_DOWN_STEP_SIZE = 0.05
        const val MAX_SPEED = 5.0 // 5 A per minute

        /**
         * Method converts double to LAMBDA string
         *
         * @param d double that should be converted to string
         * @return string
         */
        private fun d2s(d: Double): String = LAMBDA_FORMAT.format(d)
    }

}
