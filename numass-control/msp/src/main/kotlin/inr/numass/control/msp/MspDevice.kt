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
import hep.dataforge.kodex.useMeta
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
import inr.numass.control.msp.MspDevice.Companion.SELECTED_STATE
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
        StateDef(ValueDef(name = SELECTED_STATE)),
        StateDef(value = ValueDef(name = "storing", info = "Define if this device is currently writes to storage"), writable = true),
        StateDef(value = ValueDef(name = "filament", info = "The number of filament in use"), writable = true),
        StateDef(value = ValueDef(name = "filamentOn", info = "Mass-spectrometer filament on"), writable = true),
        StateDef(ValueDef(name = "filamentStatus", info = "Filament status")),
        StateDef(ValueDef(name = "peakJump.zero", type = [ValueType.NUMBER], info = "Peak jump zero reading"))
)
@DeviceView(MspDisplay::class)
class MspDevice(context: Context, meta: Meta) : PortSensor(context, meta) {

//    private var measurementDelegate: Consumer<MspResponse>? = null

    //val selected: Boolean by valueState("selected").booleanDelegate

    val controlled = valueState(CONTROLLED_STATE) { value ->
        runOnDeviceThread {
            val res = control(value.boolean)
            updateState(CONTROLLED_STATE, res)
        }
    }

    val filament = valueState("filament") { value ->
        selectFilament(value.int)
    }

    val filamentOn = valueState("filamentOn") { value ->
        setFilamentOn(value.boolean)
    }

    var peakJumpZero: Double by valueState("peakJump.zero").doubleDelegate

    private val averagingDuration: Duration = Duration.parse(meta.getString("averagingDuration", "PT30S"))

    private var storageHelper: NumassStorageConnection? = null

    private val collector = RegularPointCollector(averagingDuration) { res ->
        notifyResult(res)
        forEachConnection(ValuesListener::class.java) {
            it.accept(res)
        }
    }

    override fun buildConnection(meta: Meta): GenericPortController {
        logger.info("Connecting to port {}", meta)
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

    override fun init() {
        super.init()
        meta.useMeta("peakJump"){
            updateState(MEASUREMENT_META_STATE, it)
        }
    }

    @Throws(ControlException::class)
    override fun shutdown() {
        if (controlled.booleanValue) {
            setFilamentOn(false)
        }
        controlled.set(false)
        super.shutdown()
    }

    override val type: String
        get() = MSP_DEVICE_TYPE

    /**
     * Startup MSP: get available sensors, select sensor and control.
     *
     * @param on
     * @return
     * @throws hep.dataforge.exceptions.ControlException
     */
    private fun control(on: Boolean): Boolean {
        if (on != this.controlled.booleanValue) {
            val sensorName: String
            if (on) {
                logger.info("Starting initialization sequence")
                //ensure device is connected
                connected.setAndWait(true)
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
                    controlled.update(true)
                } else {
                    notifyError(response.errorDescription, null)
                    return false
                }
                //                connected = true;
                updateState(PortSensor.CONNECTED_STATE, true)
                return true
            } else {
                logger.info("Releasing device")
                return !commandAndWait("Release").isOK
            }
        } else {
            return on
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
        val command = buildCommand(commandName, *parameters)
        if (debug) {
            logger.info("SEND: $command")
        }
        val response = connection.sendAndWait(command, TIMEOUT) { str: String -> str.trim { it <= ' ' }.startsWith(commandName) }
        if (debug) {
            logger.info("RECEIVE:\n$response")
        }
        return MspResponse(response)
    }

    @Throws(PortException::class)
    private fun selectFilament(filament: Int) {
        runOnDeviceThread {
            val response = commandAndWait("FilamentSelect", filament)
            if (response.isOK) {
                this.filament.update(response[1, 1])
            } else {
                logger.error("Failed to set filament with error: {}", response.errorDescription)
            }
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

    override fun stopMeasurement() {
        runOnDeviceThread {
            stopPeakJump()
        }
        super.stopMeasurement()
    }

    override fun startMeasurement(oldMeta: Meta?, newMeta: Meta) {
        if (oldMeta != null) {
            stopMeasurement()
        }
        if (newMeta.getString("type", "peakJump") == "peakJump") {
            runOnDeviceThread {
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
        sendAndWait("MeasurementRemoveAll", Duration.ofMillis(200))

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

        if (!filamentOn.booleanValue) {
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

    companion object {
        const val MSP_DEVICE_TYPE = "numass.msp"
        const val CONTROLLED_STATE = "controlled"
        const val SELECTED_STATE = "selected"

        private val TIMEOUT = Duration.ofMillis(200)
    }
}
