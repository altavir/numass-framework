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

import hep.dataforge.context.Context
import hep.dataforge.control.RoleDef
import hep.dataforge.control.RoleDefs
import hep.dataforge.control.collectors.RegularPointCollector
import hep.dataforge.control.connections.Roles
import hep.dataforge.control.devices.Device
import hep.dataforge.control.devices.PortSensor
import hep.dataforge.control.devices.Sensor
import hep.dataforge.control.devices.StateDef
import hep.dataforge.control.measurements.AbstractMeasurement
import hep.dataforge.control.measurements.Measurement
import hep.dataforge.control.ports.GenericPortController
import hep.dataforge.control.ports.Port
import hep.dataforge.description.ValueDef
import hep.dataforge.exceptions.ControlException
import hep.dataforge.exceptions.MeasurementException
import hep.dataforge.exceptions.StorageException
import hep.dataforge.meta.Meta
import hep.dataforge.storage.api.TableLoader
import hep.dataforge.storage.commons.LoaderFactory
import hep.dataforge.storage.commons.StorageConnection
import hep.dataforge.tables.TableFormat
import hep.dataforge.tables.TableFormatBuilder
import hep.dataforge.utils.DateTimeUtils
import hep.dataforge.values.Values
import inr.numass.control.DeviceView
import inr.numass.control.StorageHelper
import java.time.Duration
import java.util.*
import java.util.function.BiConsumer


/**
 * A device controller for Dubna PKT 8 cryogenic thermometry device
 *
 * @author Alexander Nozik
 */
@RoleDefs(
        RoleDef(name = Roles.STORAGE_ROLE),
        RoleDef(name = Roles.VIEW_ROLE)
)
@ValueDef(name = "port", def = "virtual", info = "The name of the port for this PKT8")
@StateDef(ValueDef(name = "storing"))
@DeviceView(PKT8Display::class)
class PKT8Device(context: Context, meta: Meta) : PortSensor<PKT8Result>(context, meta) {
    /**
     * The key is the letter (a,b,c,d...) as in measurements
     */
    val channels = LinkedHashMap<String, PKT8Channel>()
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

    val sps: String
        get() = getState(SPS).stringValue()

    val pga: String
        get() = getState(PGA).stringValue()

    val abuf: String
        get() = getState(ABUF).stringValue()

    private val duration = Duration.parse(getMeta().getString("averagingDuration", "PT30S"))

    private fun buildLoader(connection: StorageConnection): TableLoader {
        val storage = connection.storage
        val suffix = DateTimeUtils.fileSuffix()

        try {
            return LoaderFactory.buildPointLoder(storage, "cryotemp_" + suffix, "", "timestamp", tableFormat)
        } catch (e: StorageException) {
            throw RuntimeException("Failed to builder loader from storage", e)
        }

    }

    @Throws(ControlException::class)
    override fun init() {

        //read channel configuration
        if (getMeta().hasMeta("channel")) {
            for (node in getMeta().getMetaList("channel")) {
                val designation = node.getString("designation", "default")
                this.channels.put(designation, createChannel(node))
            }
        } else {
            //set default channel configuration
            for (designation in CHANNEL_DESIGNATIONS) {
                this.channels.put(designation, createChannel(designation))
            }
            logger.warn("No channels defined in configuration")
        }

        super.init()

        //update parameters from meta
        meta.optValue("pga").ifPresent {
            logger.info("Setting dynamic range to " + it.intValue())
            val response = sendAndWait("g" + it.intValue()).trim { it <= ' ' }
            if (response.contains("=")) {
                updateState(PGA, Integer.parseInt(response.substring(4)))
            } else {
                logger.error("Setting pga failed with message: " + response)
            }
        }


        setSPS(getMeta().getInt("sps", 0))
        setBUF(getMeta().getInt("abuf", 100))

        // setting up the collector
        storageHelper = StorageHelper(this) { connection: StorageConnection -> this.buildLoader(connection) }
    }

    @Throws(ControlException::class)
    override fun shutdown() {
        measurement?.stop(true)
        storageHelper?.close()
        super.shutdown()
    }

    @Throws(ControlException::class)
    override fun buildPort(portName: String): Port {
        //setup connection
        val handler: Port = if ("virtual" == portName) {
            logger.info("Starting {} using virtual debug port", name)
            PKT8VirtualPort("PKT8", getMeta().getMetaOrEmpty("debug"))
        } else {
            super.buildPort(portName)
        }
        handler.setDelimiter("\n")

        return handler
    }

    private fun setBUF(buf: Int) {
        logger.info("Setting avaraging buffer size to " + buf)
        var response: String
        try {
            response = sendAndWait("b" + buf).trim { it <= ' ' }
        } catch (ex: Exception) {
            response = ex.message ?: ""
        }

        if (response.contains("=")) {
            updateState(ABUF, Integer.parseInt(response.substring(14)))
            //            getLogger().info("successfully set buffer size to {}", this.abuf);
        } else {
            logger.error("Setting averaging buffer failed with message: " + response)
        }
    }

    @Throws(ControlException::class)
    fun changeParameters(sps: Int, abuf: Int) {
        stopMeasurement(false)
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

    private fun setSPS(sps: Int) {
        logger.info("Setting sampling rate to " + spsToStr(sps))
        val response: String = try {
            sendAndWait("v" + sps).trim { it <= ' ' }
        } catch (ex: Exception) {
            ex.message ?: ""
        }

        if (response.contains("=")) {
            updateState(SPS, Integer.parseInt(response.substring(4)))
        } else {
            logger.error("Setting sps failed with message: " + response)
        }
    }

    @Throws(MeasurementException::class)
    override fun createMeasurement(): Measurement<PKT8Result> {
        return if (this.measurement != null) {
            this.measurement
        } else {
            try {
                PKT8Measurement(connection)
            } catch (e: ControlException) {
                throw MeasurementException(e)
            }

        }
    }

    @Throws(MeasurementException::class)
    override fun startMeasurement(): Measurement<PKT8Result> {
        //clearing PKT queue
        try {
            send("p")
            sendAndWait("p")
        } catch (e: ControlException) {
            logger.error("Failed to clear PKT8 port")
            //   throw new MeasurementException(e);
        }

        return super.startMeasurement()
    }


    inner class PKT8Measurement(private val controller: GenericPortController) : AbstractMeasurement<PKT8Result>() {

        override fun getDevice(): Device = this@PKT8Device

        private var collector: RegularPointCollector = RegularPointCollector(duration, channels.values.map { it.name }) { dp: Values ->
            logger.debug("Point measurement complete. Pushing...")
            storageHelper?.push(dp)
        }

        var errorListener: BiConsumer<String, Throwable>? = null
        var stopListener: GenericPortController.PhraseListener? = null;
        var valueListener: GenericPortController.PhraseListener? = null;

        override fun start() {
            if (isStarted) {
                logger.warn("Trying to start measurement which is already started")
            }

            try {
                logger.info("Starting measurement")
                //add weak error listener
                errorListener = controller.onError(this::onError)

                //add weak stop listener
                stopListener = controller.onPhrase("[Ss]topped\\s*") {
                    afterPause()
                    updateState(Sensor.MEASURING_STATE, false)
                }

                //add weak measurement listener
                valueListener = controller.onPhrase("[a-f].*") {
                    val trimmed = it.trim()
                    val designation = trimmed.substring(0, 1)
                    val rawValue = java.lang.Double.parseDouble(trimmed.substring(1)) / 100

                    val channel = this@PKT8Device.channels[designation]

                    if (channel != null) {
                        result(channel.evaluate(rawValue))
                        collector.put(channel.name, channel.getTemperature(rawValue))
                    } else {
                        result(PKT8Result(designation, rawValue, -1.0))
                    }
                }

                //send start signal
                controller.send("s")

                afterStart()
            } catch (ex: ControlException) {
                onError("Failed to start measurement", ex)
            }

        }

        @Throws(MeasurementException::class)
        override fun stop(force: Boolean): Boolean {
            if (isFinished) {
                logger.warn("Trying to stop measurement which is already stopped")
                return true
            } else {

                try {
                    logger.info("Stopping measurement")
                    val response = sendAndWait("p").trim()
                    // Должно быть именно с большой буквы!!!
                    return "Stopped" == response || "stopped" == response
                } catch (ex: Exception) {
                    onError("Failed to stop measurement", ex)
                    return false
                } finally {
                    afterStop()
                    errorListener?.let { controller.removeErrorListener(it) }
                    stopListener?.let { controller.removePhraseListener(it) }
                    valueListener?.let { controller.removePhraseListener(it) }
                    collector.stop()
                    logger.debug("Collector stopped")
                }
            }
        }
    }

    companion object {
        val PKT8_DEVICE_TYPE = "numass.pkt8"

        val PGA = "pga"
        val SPS = "sps"
        val ABUF = "abuf"
        private val CHANNEL_DESIGNATIONS = arrayOf("a", "b", "c", "d", "e", "f", "g", "h")
    }

}


data class PKT8Result(val channel: String, val rawValue: Double, val temperature: Double) {

    val rawString: String = String.format("%.2f", rawValue)

    val temperatureString: String = String.format("%.2f", temperature)
}