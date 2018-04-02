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

import hep.dataforge.connections.NamedValueListener
import hep.dataforge.connections.RoleDef
import hep.dataforge.connections.RoleDefs
import hep.dataforge.context.Context
import hep.dataforge.control.collectors.RegularPointCollector
import hep.dataforge.control.connections.Roles
import hep.dataforge.control.devices.PortSensor
import hep.dataforge.control.ports.GenericPortController
import hep.dataforge.control.ports.Port
import hep.dataforge.control.ports.PortFactory
import hep.dataforge.description.ValueDef
import hep.dataforge.exceptions.ControlException
import hep.dataforge.exceptions.MeasurementException
import hep.dataforge.exceptions.PortException
import hep.dataforge.meta.Meta
import hep.dataforge.states.StateDef
import hep.dataforge.states.StateDefs
import hep.dataforge.states.valueState
import hep.dataforge.storage.commons.StorageConnection
import hep.dataforge.tables.TableFormatBuilder
import hep.dataforge.tables.ValuesListener
import hep.dataforge.values.ValueType
import inr.numass.control.DeviceView
import inr.numass.control.NumassStorageConnection
import java.time.Duration
import java.util.*

/**
 * @author Alexander Nozik
 */
@RoleDefs(
        RoleDef(name = Roles.STORAGE_ROLE, objectType = StorageConnection::class),
        RoleDef(name = Roles.VIEW_ROLE)
)
@StateDefs(
        StateDef(value = ValueDef(name = "controlled", info = "Connection with the device itself"), writable = true),
        StateDef(value = ValueDef(name = "storing", info = "Define if this device is currently writes to storage"), writable = true),
        StateDef(value = ValueDef(name = "filament", info = "The number of filament in use"), writable = true),
        StateDef(value = ValueDef(name = "filamentOn", info = "Mass-spectrometer filament on"), writable = true),
        StateDef(ValueDef(name = "filamentStatus", info = "Filament status")),
        StateDef(ValueDef(name = "peakJump.zero", type = [ValueType.NUMBER], info = "Peak jump zero reading"))
)
@DeviceView(MspDisplay::class)
class MspDevice(context: Context, meta: Meta) : PortSensor(context, meta) {

//    private var measurementDelegate: Consumer<MspResponse>? = null

    val selected: Boolean by valueState("selected").boolean

    var controlled: Boolean by valueState("controlled") { _, value ->
        control(value.booleanValue())
    }.boolean

    var filament by valueState("filament") { old, value ->
        selectFilament(value.intValue())
    }.int

    var filamentOn: Boolean by valueState("filamentOn") { _, value ->
        setFilamentOn(value.booleanValue())
    }.boolean

    var peakJumpZero: Double by valueState("peakJump.zero").double

    private val averagingDuration: Duration = Duration.parse(meta.getString("averagingDuration", "PT30S"))

    private var storageHelper: NumassStorageConnection? = null

    private val collector = RegularPointCollector(averagingDuration) { res ->
        notifyResult(res)
        forEachConnection(ValuesListener::class.java) {
            it.accept(res)
        }
    }

    override fun connect(meta: Meta): GenericPortController {
        val portName = meta.getString("name")
        logger.info("Connecting to port {}", portName)
        val port: Port = PortFactory.build(meta)
        return GenericPortController(context, port, "\r\r").also {
            it.weakOnPhrase({ it.startsWith("FilamentStatus") }, this) {
                val response = MspResponse(it)
                val status = response[0, 2]
                updateState("filamentOn", status == "ON")
                updateState("filamentStatus", status)
            }
            logger.info("Connected to MKS mass-spectrometer on {}", it.port)
        }
    }


    @Throws(ControlException::class)
    override fun shutdown() {
        super.stopMeasurement()
        if (connected) {
            setFilamentOn(false)
            connect(false)
        }
        super.shutdown()
    }

//    //TODO make actual request
//    override fun computeState(stateName: String): Any = when (stateName) {
//        "controlled" -> false
//        "filament" -> 1
//        "filamentOn" -> false//Always return false on first request
//        "filamentStatus" -> "UNKNOWN"
//        else -> super.computeState(stateName)
//    }

    override fun getType(): String = MSP_DEVICE_TYPE

    /**
     * Startup MSP: get available sensors, select sensor and control.
     *
     * @param on
     * @return
     * @throws hep.dataforge.exceptions.ControlException
     */
    private fun control(on: Boolean): Boolean {
        val sensorName: String
        if (on) {
            //ensure device is connected
            connected = true
            var response = commandAndWait("Sensors")
            if (response.isOK) {
                sensorName = response[2, 1]
            } else {
                notifyError(response.errorDescription, null)
                return false
            }
            //PENDING определеить в конфиге номер прибора

            response = commandAndWait("Select", sensorName)
            if (response.isOK) {
                updateState("selected", true)
            } else {
                notifyError(response.errorDescription, null)
                return false
            }

            response = commandAndWait("Control", "inr.numass.msp", "1.1")
            if (response.isOK) {

                updateState("controlled", true)
            } else {
                notifyError(response.errorDescription, null)
                return false
            }
            //                connected = true;
            updateState(PortSensor.CONNECTED_STATE, true)
            return true
        } else {
            return !commandAndWait("Release").isOK
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
        val response = connection.waitFor(TIMEOUT) { str: String -> str.trim { it <= ' ' }.startsWith(commandName) }
        return MspResponse(response)
    }

    @Throws(PortException::class)
    private fun selectFilament(filament: Int) {
        val response = commandAndWait("FilamentSelect", filament)
        if (response.isOK) {
            updateState("filament", response[1, 1])
        } else {
            logger.error("Failed to set filament with error: {}", response.errorDescription)
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

        val errorDescription: String
            get() {
                return if (isOK) {
                    throw RuntimeException("Not a error")
                } else {
                    get(2, 1)
                }
            }

        operator fun get(lineNo: Int, columnNo: Int): String = data[lineNo][columnNo]
    }

//    @Throws(MeasurementException::class)
//    override fun createMeasurement(): PeakJumpMeasurement {
//        val measurementMeta = meta.getMeta("peakJump")
//        val s = measurementMeta.getString("type", "peakJump")
//        if (s == "peakJump") {
//            val measurement = PeakJumpMeasurement(measurementMeta)
//            this.measurementDelegate = measurement
//            return measurement
//        } else {
//            throw MeasurementException("Unknown measurement type")
//        }
//    }

    override fun stopMeasurement() {
        super.stopMeasurement()
        execute {
            stopPeakJump()
        }
    }

    override fun startMeasurement(oldMeta: Meta?, newMeta: Meta) {
        if (oldMeta != null) {
            stopMeasurement()
        }
        if (newMeta.getString("type", "peakJump") == "peakJump") {
            execute {
                startPeakJump(newMeta)
            }
        } else {
            throw MeasurementException("Unknown measurement type")
        }
    }

    private fun startPeakJump(meta: Meta) {
        notifyMeasurementState(MeasurementState.IN_PROGRESS)
        val measurementName = "peakJump"
        val filterMode = meta.getString("filterMode", "PeakAverage")
        val accuracy = meta.getInt("accuracy", 5)
        //PENDING вставить остальные параметры?
        sendAndWait("MeasurementRemoveAll")

//        val peakMap: MutableMap<Int, String> = LinkedHashMap()

        val builder = TableFormatBuilder().addTime("timestamp")

        if (commandAndWait("AddPeakJump", measurementName, filterMode, accuracy, 0, 0, 0).isOK) {
//            peakMap.clear()
            for (peak in meta.getMetaList("peak")) {
//                peakMap[peak.getInt("mass")] = peak.getString("name", peak.getString("mass"))
                if (!commandAndWait("MeasurementAddMass", peak.getString("mass")).isOK) {
                    throw ControlException("Can't add mass to measurement measurement for msp")
                }
                builder.addNumber(peak.getString("name", peak.getString("mass")))
            }
        } else {
            throw ControlException("Can't create measurement for msp")
        }

        storageHelper = NumassStorageConnection("msp") { builder.build() }
        connect(storageHelper)

        connection.onAnyPhrase(this) {
            val response = MspResponse(it)
            when (response.commandName) {
                "MassReading" -> {
                    val mass = java.lang.Double.parseDouble(response[0, 1])
                    val value = java.lang.Double.parseDouble(response[0, 2]) / 100.0
                    val massName = Integer.toString(Math.floor(mass + 0.5).toInt())
                    collector.put(massName, value)
                    forEachConnection(Roles.VIEW_ROLE, NamedValueListener::class.java) { listener -> listener.pushValue(massName, value) }
                }
                "ZeroReading" -> {
                    updateState("peakJump.zero", java.lang.Double.parseDouble(response[0, 2]) / 100.0)
                }
                "StartingScan" -> {
                    val numScans = Integer.parseInt(response[0, 3])

                    if (numScans == 0) {
                        try {
                            command("ScanResume", 10)
                            //FIXME обработать ошибку связи
                        } catch (ex: PortException) {
                            notifyError("Failed to resume scan", ex)
                        }

                    }
                }
            }
        }

        if (!filamentOn) {
            notifyError("Can't start measurement. Filament is not turned on.")
        }
        if (!commandAndWait("ScanAdd", measurementName).isOK) {
            notifyError("Failed to add scan")
        }

        if (!commandAndWait("ScanStart", 2).isOK) {
            notifyError("Failed to start scan")
        }
    }

    private fun stopPeakJump() {
        collector.stop()
        val stop = commandAndWait("ScanStop").isOK
        //Reset loaders in connections
        storageHelper?.let { disconnect(it) }
        notifyMeasurementState(MeasurementState.STOPPED)
    }

    companion object {
        const val MSP_DEVICE_TYPE = "numass.msp"

        private val TIMEOUT = Duration.ofMillis(200)
    }
}
