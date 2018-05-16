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
package inr.numass.control.cryotemp

import hep.dataforge.connections.RoleDef
import hep.dataforge.connections.RoleDefs
import hep.dataforge.context.Context
import hep.dataforge.control.collectors.RegularPointCollector
import hep.dataforge.control.connections.Roles
import hep.dataforge.control.devices.PortSensor
import hep.dataforge.control.devices.Sensor
import hep.dataforge.control.ports.GenericPortController
import hep.dataforge.control.ports.Port
import hep.dataforge.control.ports.PortFactory
import hep.dataforge.description.ValueDef
import hep.dataforge.exceptions.ControlException
import hep.dataforge.exceptions.StorageException
import hep.dataforge.meta.Meta
import hep.dataforge.states.StateDef
import hep.dataforge.states.valueState
import hep.dataforge.storage.api.TableLoader
import hep.dataforge.storage.commons.LoaderFactory
import hep.dataforge.storage.commons.StorageConnection
import hep.dataforge.tables.TableFormat
import hep.dataforge.tables.TableFormatBuilder
import hep.dataforge.utils.DateTimeUtils
import inr.numass.control.DeviceView
import inr.numass.control.StorageHelper
import java.time.Duration
import java.time.Instant
import java.util.*


/**
 * A device controller for Dubna PKT 8 cryogenic thermometry device
 *
 * @author Alexander Nozik
 */
@RoleDefs(
        RoleDef(name = Roles.STORAGE_ROLE),
        RoleDef(name = Roles.VIEW_ROLE)
)
@ValueDef(key = "port", def = "virtual", info = "The name of the port for this PKT8")
@StateDef(value = ValueDef(key = "storing", info = "Define if this device is currently writes to storage"), writable = true)
@DeviceView(PKT8Display::class)
class PKT8Device(context: Context, meta: Meta) : PortSensor(context, meta) {
    /**
     * The key is the letter (a,b,c,d...) as in measurements
     */
    val channels = LinkedHashMap<String, PKT8Channel>()

    val storing = valueState("storing")
    private var storageHelper: StorageHelper? = null

    /**
     * Cached values
     */
    //private var format: TableFormat? = null


    // Building data format
    private val tableFormat: TableFormat by lazy {
        val tableFormatBuilder = TableFormatBuilder()
                .addTime("timestamp")

        for (channel in this.channels.values) {
            tableFormatBuilder.addNumber(channel.name)
        }
        tableFormatBuilder.build()
    }

    val sps: String by valueState(SPS).stringDelegate

    val pga: String by valueState(PGA).stringDelegate

    val abuf: String by valueState(ABUF).stringDelegate

    private val duration = Duration.parse(meta.getString("averagingDuration", "PT30S"))

    private fun buildLoader(connection: StorageConnection): TableLoader {
        val storage = connection.storage
        val suffix = DateTimeUtils.fileSuffix()

        try {
            return LoaderFactory.buildPointLoader(storage, "cryotemp_$suffix", "", "timestamp", tableFormat)
        } catch (e: StorageException) {
            throw RuntimeException("Failed to builder loader from storage", e)
        }

    }

    @Throws(ControlException::class)
    override fun init() {

        //read channel configuration
        if (meta.hasMeta("channel")) {
            for (node in meta.getMetaList("channel")) {
                val designation = node.getString("designation", "default")
                this.channels[designation] = createChannel(node)
            }
        } else {
            //set default channel configuration
            for (designation in CHANNEL_DESIGNATIONS) {
                this.channels[designation] = createChannel(designation)
            }
            logger.warn("No channels defined in configuration")
        }

        super.init()

        //update parameters from meta
        meta.optValue("pga").ifPresent {
            logger.info("Setting dynamic range to " + it.int)
            val response = sendAndWait("g" + it.int).trim { it <= ' ' }
            if (response.contains("=")) {
                updateState(PGA, Integer.parseInt(response.substring(4)))
            } else {
                logger.error("Setting pga failed with message: $response")
            }
        }


        setSPS(meta.getInt("sps", 0))
        setBUF(meta.getInt("abuf", 100))

        // setting up the collector
        storageHelper = StorageHelper(this) { connection: StorageConnection -> this.buildLoader(connection) }
    }

    @Throws(ControlException::class)
    override fun shutdown() {
        storageHelper?.close()
        super.shutdown()
    }

    override fun buildConnection(meta: Meta): GenericPortController {
        val portName = meta.getString("name", "virtual")

        val port: Port = if (portName == "virtual") {
            logger.info("Starting {} using virtual debug port", name)
            PKT8VirtualPort("PKT8", meta)
        } else {
            logger.info("Connecting to port {}", portName)
            PortFactory.build(meta)
        }
        return GenericPortController(context, port, "\n")
    }

    private fun setBUF(buf: Int) {
        logger.info("Setting averaging buffer size to $buf")
        var response: String
        try {
            response = sendAndWait("b$buf").trim { it <= ' ' }
        } catch (ex: Exception) {
            response = ex.message ?: ""
        }

        if (response.contains("=")) {
            updateState(ABUF, Integer.parseInt(response.substring(14)))
            //            getLogger().info("successfully set buffer size to {}", this.abuf);
        } else {
            logger.error("Setting averaging buffer failed with message: $response")
        }
    }

    @Throws(ControlException::class)
    fun changeParameters(sps: Int, abuf: Int) {
        stopMeasurement()
        //setting sps
        setSPS(sps)
        //setting buffer
        setBUF(abuf)
    }

    /**
     * '0' : 2,5 SPS '1' : 5 SPS '2' : 10 SPS '3' : 25 SPS '4' : 50 SPS '5' :
     * 100 SPS '6' : 500 SPS '7' : 1 kSPS '8' : 3,75 kSPS
     *
     * @param sps
     * @return
     */
    private fun spsToStr(sps: Int): String {
        return when (sps) {
            0 -> "2.5 SPS"
            1 -> "5 SPS"
            2 -> "10 SPS"
            3 -> "25 SPS"
            4 -> "50 SPS"
            5 -> "100 SPS"
            6 -> "500 SPS"
            7 -> "1 kSPS"
            8 -> "3.75 kSPS"
            else -> "unknown value"
        }
    }

    /**
     * '0' : ± 5 В '1' : ± 2,5 В '2' : ± 1,25 В '3' : ± 0,625 В '4' : ± 312.5 мВ
     * '5' : ± 156,25 мВ '6' : ± 78,125 мВ
     *
     * @param pga
     * @return
     */
    private fun pgaToStr(pga: Int): String {
        return when (pga) {
            0 -> "± 5 V"
            1 -> "± 2,5 V"
            2 -> "± 1,25 V"
            3 -> "± 0,625 V"
            4 -> "± 312.5 mV"
            5 -> "± 156.25 mV"
            6 -> "± 78.125 mV"
            else -> "unknown value"
        }
    }

    private val collector = RegularPointCollector(duration) {
        notifyResult(it)
        storageHelper?.push(it)
    }

    private fun notifyChannelResult(designation: String, rawValue: Double) {
        updateState("raw.$designation", rawValue)

        val channel = channels[designation]

        val temperature = channel?.let {
            val temp = it.getTemperature(rawValue)
            updateState("temp.$designation", temp)
            collector.put(it.name, temp)
            temp
        }
        forEachConnection(PKT8ValueListener::class.java) {
            it.report(PKT8Reading(channel?.name ?: designation, rawValue, temperature))
        }
    }

    override fun startMeasurement(oldMeta: Meta?, newMeta: Meta) {
        if (oldMeta != null) {
            stopMeasurement()
        }

//        if (!oldMeta.isEmpty) {
//            logger.warn("Trying to start measurement which is already started")
//        }


        logger.info("Starting measurement")

        connection.also {
            it.onPhrase("[Ss]topped\\s*", this) {
                notifyMeasurementState(Sensor.MeasurementState.STOPPED)
            }

            //add weak measurement listener
            it.onPhrase("[a-f].*", this) {
                val trimmed = it.trim()
                val designation = trimmed.substring(0, 1)
                val rawValue = java.lang.Double.parseDouble(trimmed.substring(1)) / 100

                notifyChannelResult(designation, rawValue)
            }

            //send start signal
            it.send("s")
            notifyMeasurementState(Sensor.MeasurementState.IN_PROGRESS)
        }

    }

    override fun stopMeasurement() {
        try {
            logger.info("Stopping measurement")
            val response = sendAndWait("p").trim()
            // Должно быть именно с большой буквы!!!
            if ("Stopped" == response || "stopped" == response) {
                notifyMeasurementState(Sensor.MeasurementState.STOPPED)
            }
        } catch (ex: Exception) {
            notifyError("Failed to stop measurement", ex)
        } finally {
            connection.removeErrorListener(this)
            connection.removePhraseListener(this)
            collector.stop()
            logger.debug("Collector stopped")
        }
    }

    private fun setSPS(sps: Int) {
        logger.info("Setting sampling rate to " + spsToStr(sps))
        val response: String = try {
            sendAndWait("v$sps").trim { it <= ' ' }
        } catch (ex: Exception) {
            ex.message ?: ""
        }

        if (response.contains("=")) {
            updateState(SPS, Integer.parseInt(response.substring(4)))
        } else {
            logger.error("Setting sps failed with message: $response")
        }
    }

    companion object {
        const val PKT8_DEVICE_TYPE = "numass.pkt8"

        const val PGA = "pga"
        const val SPS = "sps"
        const val ABUF = "abuf"
        private val CHANNEL_DESIGNATIONS = arrayOf("a", "b", "c", "d", "e", "f", "g", "h")
    }
}

data class PKT8Reading(val channel: String, val rawValue: Double, val temperature: Double?) {

    val rawString: String = String.format("%.2f", rawValue)

    val temperatureString: String = String.format("%.2f", temperature)
}

interface PKT8ValueListener {
    fun report(reading: PKT8Reading, time: Instant = Instant.now())
}