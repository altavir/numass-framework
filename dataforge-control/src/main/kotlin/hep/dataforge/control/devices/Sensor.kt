/*
 * Copyright  2017 Alexander Nozik.
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

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.control.devices

import hep.dataforge.context.Context
import hep.dataforge.control.devices.Sensor.Companion.MEASUREMENT_MESSAGE_STATE
import hep.dataforge.control.devices.Sensor.Companion.MEASUREMENT_META_STATE
import hep.dataforge.control.devices.Sensor.Companion.MEASUREMENT_PROGRESS_STATE
import hep.dataforge.control.devices.Sensor.Companion.MEASUREMENT_RESULT_STATE
import hep.dataforge.control.devices.Sensor.Companion.MEASUREMENT_STATUS_STATE
import hep.dataforge.control.devices.Sensor.Companion.MEASURING_STATE
import hep.dataforge.description.NodeDef
import hep.dataforge.description.ValueDef
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaMorph
import hep.dataforge.meta.buildMeta
import hep.dataforge.states.*
import hep.dataforge.values.ValueType
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import java.time.Duration
import java.time.Instant

/**
 * A device with which could perform of one-time or regular measurements. Only one measurement is allowed at a time
 *
 * @author Alexander Nozik
 */
@ValueDef(
    key = "resultBuffer",
    type = [ValueType.NUMBER],
    def = "100",
    info = "The size of the buffer for results of measurements"
)
@StateDefs(
    StateDef(
        value = ValueDef(
            key = MEASURING_STATE,
            type = [ValueType.BOOLEAN],
            info = "Shows if this sensor is actively measuring"
        ), writable = true
    ),
    StateDef(
        ValueDef(
            key = MEASUREMENT_STATUS_STATE,
            enumeration = Sensor.MeasurementState::class,
            info = "Shows if this sensor is actively measuring"
        )
    ),
    StateDef(ValueDef(key = MEASUREMENT_MESSAGE_STATE, info = "Current message")),
    StateDef(ValueDef(key = MEASUREMENT_PROGRESS_STATE, type = [ValueType.NUMBER], info = "Current progress"))
)
@MetaStateDefs(
    MetaStateDef(
        value = NodeDef(key = MEASUREMENT_META_STATE, info = "Configuration of current measurement."),
        writable = true
    ),
    MetaStateDef(NodeDef(key = MEASUREMENT_RESULT_STATE, info = "The result of the last measurement in Meta form"))
)
abstract class Sensor(context: Context, meta: Meta) : AbstractDevice(context, meta) {

    private val coroutineContext = executor.asCoroutineDispatcher()

    protected var job: Job? = null

    val resultState = metaState(MEASUREMENT_RESULT_STATE)

    /**
     * The result of last measurement
     */
    val result: Meta by resultState.delegate

    /**
     * The error from last measurement
     */
    val error: Meta by metaState(MEASUREMENT_ERROR_STATE).delegate

    /**
     * Current measurement configuration
     */
    var measurement by metaState(MEASUREMENT_META_STATE) { old: Meta?, value: Meta ->
        startMeasurement(old, value)
        update(value)
    }.delegate

    /**
     * true if measurement in process
     */
    val measuring = valueState(MEASURING_STATE) { value ->
        if (value.boolean) {
            startMeasurement(null, measurement)
        } else {
            stopMeasurement()
        }
        update(value)
    }

    /**
     * Current state of the measurement
     */
    val measurementState by valueState(MEASUREMENT_STATUS_STATE).enumDelegate<MeasurementState>()

    var message by valueState(MEASUREMENT_MESSAGE_STATE).stringDelegate

    var progress by valueState(MEASUREMENT_PROGRESS_STATE).doubleDelegate

    override fun shutdown() {
        stopMeasurement()
        super.shutdown()
    }

    /**
     * Start measurement with current configuration if it is not in progress
     */
    fun measure() {
        if (!measuring.booleanValue) {
            measuring.set(true)
        }
    }

    /**
     * Notify measurement state changed
     */
    protected fun notifyMeasurementState(state: MeasurementState) {
        updateState(MEASUREMENT_STATUS_STATE, state.name)
        when (state) {
            MeasurementState.NOT_STARTED -> updateState(MEASURING_STATE, false)
            MeasurementState.STOPPED -> updateState(MEASURING_STATE, false)
            MeasurementState.IN_PROGRESS -> updateState(MEASURING_STATE, true)
            MeasurementState.WAITING -> updateState(MEASURING_STATE, true)
        }
    }

    /**
     * Set active measurement using given meta
     * @param oldMeta Meta of previous active measurement. If null no measurement was set
     * @param newMeta Meta of new measurement. If null, then clear measurement
     * @return actual meta for new measurement
     */
    protected abstract fun startMeasurement(oldMeta: Meta?, newMeta: Meta)

    /**
     * stop measurement with given meta
     */
    protected open fun stopMeasurement() {
        synchronized(this) {
            job?.cancel()
            notifyMeasurementState(MeasurementState.STOPPED)
        }
    }

    protected fun measurement(action: suspend () -> Unit) {
        job = context.launch {
            notifyMeasurementState(MeasurementState.IN_PROGRESS)
            action.invoke()
            notifyMeasurementState(MeasurementState.STOPPED)
        }
    }

    protected fun scheduleMeasurement(interval: Duration, action: suspend () -> Unit) {
        job = context.launch {
            delay(interval)
            notifyMeasurementState(MeasurementState.IN_PROGRESS)
            action.invoke()
            notifyMeasurementState(MeasurementState.STOPPED)
        }
    }

    @InternalCoroutinesApi
    protected fun regularMeasurement(interval: Duration, action: suspend () -> Unit) {
        job = context.launch {
            while (true) {
                notifyMeasurementState(MeasurementState.IN_PROGRESS)
                action.invoke()
                notifyMeasurementState(MeasurementState.WAITING)
                delay(interval)
            }
        }.apply {
            invokeOnCompletion(onCancelling = true, invokeImmediately = true) {
                notifyMeasurementState(MeasurementState.STOPPED)
            }
        }
    }

    protected fun notifyResult(value: Any, timestamp: Instant = Instant.now()) {
        val result = buildMeta("result") {
            RESULT_TIMESTAMP to timestamp
            when (value) {
                is Meta -> setNode(RESULT_VALUE, value)
                is MetaMorph -> setNode(RESULT_VALUE, value.toMeta())
                else -> RESULT_VALUE to value
            }
        }
        updateState(MEASUREMENT_RESULT_STATE, result)
        forEachConnection(SensorListener::class.java){
            it.reading(this,value)
        }
    }

    protected fun notifyError(value: Any, timestamp: Instant = Instant.now()) {
        val result = buildMeta("error") {
            RESULT_TIMESTAMP to timestamp
            if (value is Meta) {
                setNode(RESULT_VALUE, value)
            } else {
                RESULT_VALUE to value
            }
        }
        updateState(MEASUREMENT_ERROR_STATE, result)
    }

    enum class MeasurementState {
        NOT_STARTED, // initial state, not started
        IN_PROGRESS, // in progress
        WAITING, // waiting on scheduler
        STOPPED // stopped
    }

    companion object {
        const val MEASURING_STATE = "measurement.active"
        const val MEASUREMENT_STATUS_STATE = "measurement.state"
        const val MEASUREMENT_META_STATE = "measurement.meta"
        const val MEASUREMENT_RESULT_STATE = "measurement.result"
        const val MEASUREMENT_ERROR_STATE = "measurement.error"
        const val MEASUREMENT_MESSAGE_STATE = "measurement.message"
        const val MEASUREMENT_PROGRESS_STATE = "measurement.progress"

        const val RESULT_SUCCESS = "success"
        const val RESULT_TIMESTAMP = "timestamp"
        const val RESULT_VALUE = "value"

    }
}

interface SensorListener{
    fun reading(sensor: Sensor, any:Any)
}
