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
package inr.numass.control.msp

import hep.dataforge.context.Context
import hep.dataforge.control.NamedValueListener
import hep.dataforge.control.RoleDef
import hep.dataforge.control.RoleDefs
import hep.dataforge.control.collectors.RegularPointCollector
import hep.dataforge.control.connections.Roles
import hep.dataforge.control.devices.Device
import hep.dataforge.control.devices.PortSensor
import hep.dataforge.control.devices.StateDef
import hep.dataforge.control.devices.StateDefs
import hep.dataforge.control.measurements.AbstractMeasurement
import hep.dataforge.control.ports.Port
import hep.dataforge.description.ValueDef
import hep.dataforge.exceptions.ControlException
import hep.dataforge.exceptions.MeasurementException
import hep.dataforge.exceptions.PortException
import hep.dataforge.meta.Meta
import hep.dataforge.storage.api.TableLoader
import hep.dataforge.storage.commons.LoaderFactory
import hep.dataforge.storage.commons.StorageConnection
import hep.dataforge.tables.TableFormatBuilder
import hep.dataforge.utils.DateTimeUtils
import hep.dataforge.values.Value
import hep.dataforge.values.Values
import inr.numass.control.DeviceView
import inr.numass.control.StorageHelper
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.function.Consumer

/**
 * @author Alexander Nozik
 */
@RoleDefs(
        RoleDef(name = Roles.STORAGE_ROLE, objectType = StorageConnection::class),
        RoleDef(name = Roles.VIEW_ROLE)
)
@StateDefs(
        StateDef(value = ValueDef(name = PortSensor.CONNECTED_STATE, info = "Connection with the device itself"), writable = true),
        StateDef(value = ValueDef(name = "storing", info = "Define if this device is currently writes to storage"), writable = true),
        StateDef(value = ValueDef(name = "filament", info = "The number of filament in use"), writable = true),
        StateDef(value = ValueDef(name = "filamentOn", info = "Mass-spectrometer filament on"), writable = true),
        StateDef(ValueDef(name = "filamentStatus", info = "Filament status"))
)
@DeviceView(MspDisplay::class)
class MspDevice(context: Context, meta: Meta) : PortSensor<Values>(context, meta) {

    private var measurementDelegate: Consumer<MspResponse>? = null

    val isSelected: Boolean
        get() = getState("selected").booleanValue()

    val isControlled: Boolean
        get() = getState("controlled").booleanValue()

    val isFilamentOn: Boolean
        get() = getState("filamentOn").booleanValue()

    private val averagingDuration: Duration = Duration.parse(meta().getString("averagingDuration", "PT30S"))

    @Throws(ControlException::class)
    override fun init() {
        super.init()
        connection.weakOnError(this::notifyError)
        onResponse("FilamentStatus"){
            val status = it[0, 2]
            updateState("filamentOn", status == "ON")
            updateState("filamentStatus", status)
        }
        logger.info("Connected to MKS mass-spectrometer on {}", connection.port);
    }

    /**
     * Add reaction on specific response
     */
    private fun onResponse(command: String, action: (MspResponse) -> Unit) {
        connection.weakOnPhrase({it.startsWith(command)}){
            action(MspResponse(it))
        }
    }

    /*

        override fun acceptPhrase(message: String) {
        dispatchEvent(
                EventBuilder
                        .make("msp")
                        .setMetaValue("response", message.trim { it <= ' ' }).build()
        )
        val response = MspResponse(message)

        when (response.commandName) {
        // all possible async messages
            "FilamentStatus" -> {
                val status = response[0, 2]
                updateState("filamentOn", status == "ON")
                updateState("filamentStatus", status)
            }
        }
        if (measurementDelegate != null) {
            measurementDelegate!!.accept(response)
        }
    }

    override fun portError(errorMessage: String?, error: Throwable?) {
        notifyError(errorMessage, error)
    }

     */

    override fun buildPort(portName: String?): Port = super.buildPort(portName).apply { setDelimiter("\r\r") }

    @Throws(ControlException::class)
    override fun shutdown() {
        super.stopMeasurement(true)
        if (isConnected) {
            setFilamentOn(false)
            setConnected(false)
        }
        super.shutdown()
    }

    @Throws(MeasurementException::class)
    override fun createMeasurement(): PeakJumpMeasurement {
        val measurementMeta = meta().getMeta("peakJump")
        val s = measurementMeta.getString("type", "peakJump")
        if (s == "peakJump") {
            val measurement = PeakJumpMeasurement(measurementMeta)
            this.measurementDelegate = measurement
            return measurement
        } else {
            throw MeasurementException("Unknown measurement type")
        }
    }

    @Throws(ControlException::class)
    override fun computeState(stateName: String): Any = when (stateName) {
        "connected" -> false
        "filament" -> 1
        "filamentOn" -> false//Always return false on first request
        "filamentStatus" -> "UNKNOWN"
        "storing" -> false
        else -> super.computeState(stateName)
    }

    override fun getType(): String = MSP_DEVICE_TYPE

    @Throws(ControlException::class)
    override fun requestStateChange(stateName: String, value: Value) {
        when (stateName) {
            PortSensor.CONNECTED_STATE -> setConnected(value.booleanValue())
            "filament" -> selectFilament(value.intValue())
            "filamentOn" -> setFilamentOn(value.booleanValue())
            else -> super.requestStateChange(stateName, value)
        }
    }

    /**
     * Startup MSP: get available sensors, select sensor and control.
     *
     * @param connected
     * @return
     * @throws hep.dataforge.exceptions.ControlException
     */
    @Throws(ControlException::class)
    private fun setConnected(connected: Boolean): Boolean {
        val sensorName: String
        if (isConnected != connected) {
            if (connected) {
                connection.open()
                var response = commandAndWait("Sensors")
                if (response.isOK) {
                    sensorName = response[2, 1]
                } else {
                    notifyError(response.errorDescription(), null)
                    return false
                }
                //PENDING определеить в конфиге номер прибора

                response = commandAndWait("Select", sensorName)
                if (response.isOK) {
                    updateState("selected", true)
                    //                    selected = true;
                } else {
                    notifyError(response.errorDescription(), null)
                    return false
                }

                response = commandAndWait("Control", "inr.numass.msp", "1.0")
                if (response.isOK) {
                    //                    controlled = true;
                    //                    invalidateState("controlled");
                    updateState("controlled", true)
                } else {
                    notifyError(response.errorDescription(), null)
                    return false
                }
                //                connected = true;
                updateState(PortSensor.CONNECTED_STATE, true)
                return true
            } else {
                connection.close()
                return !commandAndWait("Release").isOK
            }

        } else {
            return false
        }
    }

    /**
     * Send request to the msp
     *
     * @param command
     * @param parameters
     * @throws PortException
     */
    @Throws(PortException::class)
    private fun command(command: String, vararg parameters: Any) {
        send(buildCommand(command, *parameters))
    }

    /**
     * A helper method to builder msp command string
     *
     * @param command
     * @param parameters
     * @return
     */
    private fun buildCommand(command: String, vararg parameters: Any): String {
        val builder = StringBuilder(command)
        for (par in parameters) {
            builder.append(String.format(" \"%s\"", par.toString()))
        }
        builder.append("\n")
        return builder.toString()
    }

    /**
     * Send specific command and wait for its results (the result must begin
     * with command name)
     *
     * @param commandName
     * @param parameters
     * @return
     * @throws PortException
     */
    @Throws(PortException::class)
    private fun commandAndWait(commandName: String, vararg parameters: Any): MspResponse {
        send(buildCommand(commandName, *parameters))
        val response = connection.waitFor(timeout) { str: String -> str.trim { it <= ' ' }.startsWith(commandName) }
        return MspResponse(response)
    }

    @Throws(PortException::class)
    private fun selectFilament(filament: Int) {
        val response = commandAndWait("FilamentSelect", filament)
        if (response.isOK) {
            updateState("filament", response[1, 1])
        } else {
            logger.error("Failed to set filament with error: {}", response.errorDescription())
        }
    }

    /**
     * Turn filament on or off
     *
     * @param filamentOn
     * @return
     * @throws hep.dataforge.exceptions.PortException
     */
    @Throws(PortException::class)
    private fun setFilamentOn(filamentOn: Boolean): Boolean {
        return if (filamentOn) {
            commandAndWait("FilamentControl", "On").isOK
        } else {
            commandAndWait("FilamentControl", "Off").isOK
        }
    }
//
//    /**
//     * Evaluate general async messages
//     *
//     * @param response
//     */
//    private fun evaluateResponse(response: MspResponse) {
//
//    }

    /**
     * The MKS response as two-dimensional array of strings
     */
    class MspResponse(response: String) {

        private val data = ArrayList<List<String>>()

        val commandName: String
            get() = this[0, 0]

        val isOK: Boolean
            get() = "OK" == this[0, 1]

        init {
            val rx = "[^\"\\s]+|\"(\\\\.|[^\\\\\"])*\""
            val scanner = Scanner(response.trim { it <= ' ' })

            while (scanner.hasNextLine()) {
                val line = ArrayList<String>()
                var next: String? = scanner.findWithinHorizon(rx, 0)
                while (next != null) {
                    line.add(next)
                    next = scanner.findInLine(rx)
                }
                data.add(line)
            }
        }

        fun errorCode(): Int {
            return if (isOK) {
                -1
            } else {
                Integer.parseInt(get(1, 1))
            }
        }

        fun errorDescription(): String? {
            return if (isOK) {
                null
            } else {
                get(2, 1)
            }
        }

        operator fun get(lineNo: Int, columnNo: Int): String = data[lineNo][columnNo]
    }

    inner class PeakJumpMeasurement(private val meta: Meta) : AbstractMeasurement<Values>(), Consumer<MspResponse> {

        private val collector = RegularPointCollector(averagingDuration, Consumer { this.result(it) })
        private val helper = StorageHelper(this@MspDevice) { connection: StorageConnection -> this.makeLoader(connection) }
        private var peakMap: MutableMap<Int, String> = LinkedHashMap()
        private var zero = 0.0

        private fun makeLoader(connection: StorageConnection): TableLoader {
            val storage = connection.storage

            val builder = TableFormatBuilder().addTime("timestamp")
            this.peakMap.values.forEach { builder.addNumber(it) }

            val format = builder.build()

            val suffix = DateTimeUtils.fileSuffix()
            return LoaderFactory
                    .buildPointLoder(storage, "msp_" + suffix, "", "timestamp", format)

        }

        override fun getDevice(): Device = this@MspDevice

        override fun start() {
            try {
                val measurementName = "peakJump"
                val filterMode = meta.getString("filterMode", "PeakAverage")
                val accuracy = meta.getInt("accuracy", 5)!!
                //PENDING вставить остальные параметры?
                sendAndWait("MeasurementRemoveAll")
                if (commandAndWait("AddPeakJump", measurementName, filterMode, accuracy, 0, 0, 0).isOK) {
                    peakMap.clear()
                    for (peak in meta.getMetaList("peak")) {
                        peakMap.put(peak.getInt("mass"), peak.getString("name", peak.getString("mass")))
                        if (!commandAndWait("MeasurementAddMass", peak.getString("mass")).isOK) {
                            throw ControlException("Can't add mass to measurement measurement for msp")
                        }
                    }
                } else {
                    throw ControlException("Can't create measurement for msp")
                }

                if (!isFilamentOn) {
                    this.error("Can't start measurement. Filament is not turned on.", null)
                }
                if (!commandAndWait("ScanAdd", measurementName).isOK) {
                    this.error("Failed to add scan", null)
                }

                if (!commandAndWait("ScanStart", 2).isOK) {
                    this.error("Failed to start scan", null)
                }
            } catch (ex: ControlException) {
                error(ex)
            }

            afterStart()
        }

        @Throws(MeasurementException::class)
        override fun stop(force: Boolean): Boolean {
            try {
                collector.stop()
                val stop = commandAndWait("ScanStop").isOK
                afterStop()
                helper.close()
                return stop
            } catch (ex: PortException) {
                throw MeasurementException(ex)
            }

        }

        @Synchronized override fun result(result: Values, time: Instant) {
            super.result(result, time)
            helper.push(result)
        }

        internal fun error(errorMessage: String?, error: Throwable?) {
            if (error == null) {
                error(MeasurementException(errorMessage))
            } else {
                error(error)
            }
        }

        override fun accept(response: MspResponse) {
            //Evaluating measurement information
            when (response.commandName) {
                "MassReading" -> {
                    val mass = java.lang.Double.parseDouble(response[0, 1])
                    val value = java.lang.Double.parseDouble(response[0, 2]) / 100.0
                    val massName = Integer.toString(Math.floor(mass + 0.5).toInt())
                    collector.put(massName, value)
                    forEachConnection(Roles.VIEW_ROLE, NamedValueListener::class.java) { listener -> listener.pushValue(massName, value) }
                }
                "ZeroReading" -> zero = java.lang.Double.parseDouble(response[0, 2]) / 100.0
                "StartingScan" -> {
                    val numScans = Integer.parseInt(response[0, 3])

                    if (numScans == 0) {
                        try {
                            command("ScanResume", 10)
                            //FIXME обработать ошибку связи
                        } catch (ex: PortException) {
                            error(null, ex)
                        }

                    }
                }
            }
        }
    }

    companion object {
        val MSP_DEVICE_TYPE = "numass.msp"

        private val TIMEOUT = Duration.ofMillis(200)
    }
}
