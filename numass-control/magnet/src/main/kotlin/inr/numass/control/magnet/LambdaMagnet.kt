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
import hep.dataforge.meta.Meta
import hep.dataforge.states.StateDef
import hep.dataforge.states.StateDefs
import hep.dataforge.states.valueState
import hep.dataforge.utils.DateTimeUtils
import hep.dataforge.values.ValueType.*
import kotlinx.coroutines.experimental.runBlocking
import org.slf4j.LoggerFactory
import java.text.DecimalFormat
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.Future
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * @author Polina
 */
@ValueDef(name = "timeout", type = [NUMBER], def = "400", info = "A timeout for port response")
@StateDefs(
        StateDef(value = ValueDef(name = "current", type = arrayOf(NUMBER), def = "0", info = "Current current")),
        StateDef(value = ValueDef(name = "voltage", type = arrayOf(NUMBER), def = "0", info = "Current voltage")),
        StateDef(value = ValueDef(name = "outCurrent", type = arrayOf(NUMBER), def = "0", info = "Target current"), writable = true),
        StateDef(value = ValueDef(name = "outVoltage", type = arrayOf(NUMBER), def = "5.0", info = "Target voltage"), writable = true),
        StateDef(value = ValueDef(name = "output", type = arrayOf(BOOLEAN), def = "false", info = "Weather output on or off"), writable = true),
        StateDef(value = ValueDef(name = "lastUpdate", type = arrayOf(TIME), def = "0", info = "Time of the last update"), writable = true),
        StateDef(value = ValueDef(name = "updating", type = arrayOf(BOOLEAN), def = "false", info = "Shows if current ramping in progress"), writable = true),
        StateDef(value = ValueDef(name = "monitoring", type = arrayOf(BOOLEAN), def = "false", info = "Shows if monitoring task is running"), writable = true),
        StateDef(value = ValueDef(name = "speed", type = arrayOf(NUMBER), info = "Current change speed in Amper per minute"), writable = true)
)
open class LambdaMagnet(context: Context, meta: Meta, private val controller: LambdaPortController) : AbstractDevice(context, meta) {

    private var closePortOnShutDown = false

    /**
     * @return the address
     */
    val address: Int = meta.getInt("address", 1)

    override val name: String = meta.getString("name", "LAMBDA_$address")
    private val scheduler = ScheduledThreadPoolExecutor(1)

    //var listener: MagnetStateListener? = null
    //    private volatile double current = 0;
    private val timeout: Duration = meta.optString("timeout").map<Duration> { Duration.parse(it) }.orElse(Duration.ofMillis(200))
    private var monitorTask: Future<*>? = null
    private var updateTask: Future<*>? = null

    var lastUpdate by valueState("lastUpdate", getter = {0}).timeDelegate
        private set

    // read-only values of current output
    val current = valueState("current", getter = { controller.talk(address, timeout) { s2d(getParameter("MC")) } })
//    val current by current.double

    val voltage = valueState("voltage", getter = { controller.talk(address, timeout) { s2d(getParameter("MV")) } })
//    val voltage by voltage.double

    var target = valueState("target")

    //output values of current and voltage
    private var outCurrent by valueState("outCurrent", getter = { controller.talk(address, timeout) { s2d(getParameter("PC")) } }) { _, value ->
        if (setParameter("PC", value.doubleValue())) {
            lastUpdate = DateTimeUtils.now()
        } else {
            notifyError("Can't set the target current")
        }
        return@valueState value
    }.doubleDelegate

    private val outVoltage = valueState("outVoltage", getter = { controller.talk(address, timeout) { s2d(getParameter("PV")) } }) { _, value ->
        if (!setParameter("PV", value.doubleValue())) {
            notifyError("Can't set the target voltage")
        }
        return@valueState value
    }

    val output = valueState("output", getter = { controller.talk(address, timeout) { talk("OUT?") == "OK" } }) { _, value ->
        setOutputMode(value.booleanValue())
    }

    var monitoring =valueState("monitoring", getter = { monitorTask != null }) { _, value ->
        if (value.booleanValue()) {
            startMonitorTask()
        } else {
            stopMonitorTask()
        }
        return@valueState value
    }

    var updating = valueState("updating", getter = { updateTask != null }) { _, value ->
        if (value.booleanValue()) {
            startUpdateTask()
        } else {
            stopUpdateTask()
        }
        return@valueState value
    }


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
    constructor(context: Context, meta: Meta) : this(context, meta, LambdaPortController(context, PortFactory.build(meta.getString("port")))) {
        closePortOnShutDown = true
    }

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

//    private fun reportError(errorMessage: String, error: Throwable?) {
//        listener?.error(name, errorMessage, error) ?: LoggerFactory.getLogger(javaClass).error(errorMessage, error)
//
//    }

    @Throws(PortException::class)
    private fun talk(request: String): String {
        return try {
            controller.send(request + "\r")
            controller.waitFor(timeout).trim()
        } catch (tex: Port.PortTimeoutException) {
            //Single retry on timeout
            LoggerFactory.getLogger(javaClass).warn("A timeout exception for request '$request'. Making another attempt.")
            controller.send(request + "\r")
            controller.waitFor(timeout).trim()
        }
    }

//    private fun update(key: String, value: String) {
//        when (key) {
//            "OUT" -> updateState("output", value == "ON")
//            "MC" -> updateState("current", s2d(value))
//            "PC" -> updateState("outCurrent", s2d(value))
//            "MV" -> updateState("voltage", s2d(value))
//            "PV" -> updateState("outVoltage", s2d(value))
//        }
//    }

    @Throws(PortException::class)
    private fun getParameter(name: String): String = talk("$name?")

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
                updateState("current",measuredI)
                if (Math.abs(measuredI - targetI) > CURRENT_PRECISION) {
                    val nextI = nextI(measuredI, targetI)
                    outCurrent = nextI
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
        if (!setParameter("OUT", outState)) {
            notifyError("Can't set output mode")
        } else {
            updateState("output", out)
        }
    }

    private fun nextI(measuredI: Double, targetI: Double): Double {
//        assert(measuredI != target)

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

