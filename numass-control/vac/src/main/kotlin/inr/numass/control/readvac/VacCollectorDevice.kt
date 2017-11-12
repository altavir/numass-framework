/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.control.readvac

import hep.dataforge.context.Context
import hep.dataforge.control.Connection
import hep.dataforge.control.RoleDef
import hep.dataforge.control.collectors.RegularPointCollector
import hep.dataforge.control.connections.Roles
import hep.dataforge.control.connections.StorageConnection
import hep.dataforge.control.devices.Device
import hep.dataforge.control.devices.DeviceHub
import hep.dataforge.control.devices.PortSensor.CONNECTED_STATE
import hep.dataforge.control.devices.Sensor
import hep.dataforge.control.devices.StateDef
import hep.dataforge.control.measurements.AbstractMeasurement
import hep.dataforge.control.measurements.Measurement
import hep.dataforge.description.ValueDef
import hep.dataforge.exceptions.ControlException
import hep.dataforge.meta.Meta
import hep.dataforge.names.Name
import hep.dataforge.storage.api.TableLoader
import hep.dataforge.storage.commons.LoaderFactory
import hep.dataforge.tables.TableFormatBuilder
import hep.dataforge.tables.ValueMap
import hep.dataforge.utils.DateTimeUtils
import hep.dataforge.values.Value
import hep.dataforge.values.ValueType
import hep.dataforge.values.Values
import inr.numass.control.DeviceView
import inr.numass.control.StorageHelper
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.stream.Stream

/**
 * @author [Alexander Nozik](mailto:altavir@gmail.com)
 */
@RoleDef(name = Roles.STORAGE_ROLE, objectType = StorageConnection::class, info = "Storage for acquired points")
@StateDef(value = ValueDef(name = "storing", info = "Define if this device is currently writes to storage"), writable = true)
@DeviceView(VacCollectorDisplay::class)
class VacCollectorDevice(context: Context, meta: Meta, val sensors: Collection<Sensor<Double>>) : Sensor<Values>(context, meta), DeviceHub {

    private val helper = StorageHelper(this, this::buildLoader)

    private val averagingDuration: Duration
        get() = Duration.parse(meta().getString("averagingDuration", "PT30S"))


    override fun optDevice(name: Name): Optional<Device> {
        return Optional.ofNullable(sensors.find { it.name == name.toUnescaped() })
    }

    override fun deviceNames(): Stream<Name> {
        return sensors.stream().map { Name.ofSingle(it.name) }
    }


    override fun init() {
        super.init()
        for (s in sensors) {
            s.init()
        }
    }

    override fun createMeasurement(): Measurement<Values> {
        //TODO use meta
        return VacuumMeasurement()
    }

    override fun getType(): String {
        return "Numass vacuum"
    }


    @Throws(ControlException::class)
    override fun shutdown() {
        super.shutdown()
        helper.close()
        for (sensor in sensors) {
            sensor.shutdown()
        }
    }

    private fun buildLoader(connection: StorageConnection): TableLoader {
        val format = TableFormatBuilder().setType("timestamp", ValueType.TIME)
        sensors.forEach { s -> format.setType(s.name, ValueType.NUMBER) }

        val suffix = DateTimeUtils.fileSuffix()

        return LoaderFactory.buildPointLoder(connection.storage, "vactms_" + suffix, "", "timestamp", format.build())
    }

    override fun connectAll(connection: Connection, vararg roles: String) {
        connect(connection, *roles)
        this.sensors.forEach { it.connect(connection, *roles) }
    }

    override fun connectAll(context: Context, meta: Meta) {
        this.connectionHelper.connect(context, meta)
        this.sensors.forEach { it.connectionHelper.connect(context, meta) }
    }

    private inner class VacuumMeasurement : AbstractMeasurement<Values>() {

        private val collector = RegularPointCollector(averagingDuration) { this.result(it) }
        private var executor: ScheduledExecutorService? = null
        private var currentTask: ScheduledFuture<*>? = null

        override fun getDevice(): Device {
            return this@VacCollectorDevice
        }


        override fun start() {
            executor = Executors.newSingleThreadScheduledExecutor { r: Runnable -> Thread(r, "VacuumMeasurement thread") }
            val delay = meta().getInt("delay", 5)!! * 1000
            currentTask = executor!!.scheduleWithFixedDelay({
                sensors.forEach { sensor ->
                    try {
                        val value: Any?
                        value = if (sensor.optBooleanState(CONNECTED_STATE).orElse(false)) {
                            sensor.read()
                        } else {
                            null
                        }
                        collector.put(sensor.name, value)
                    } catch (ex: Exception) {
                        collector.put(sensor.name, Value.NULL)
                    }
                }
            }, 0, delay.toLong(), TimeUnit.MILLISECONDS)
        }


        @Synchronized override fun result(result: Values, time: Instant) {
            super.result(result, time)
            helper.push(result)
        }

        private fun terminator(): Values {
            val p = ValueMap.Builder()
            p.putValue("timestamp", DateTimeUtils.now())
            deviceNames().forEach { n -> p.putValue(n.toUnescaped(), null) }
            return p.build()
        }

        override fun stop(force: Boolean): Boolean {
            val isRunning = currentTask != null
            if (isRunning) {
                logger.debug("Stopping vacuum collector measurement. Writing terminator point")
                result(terminator())
                currentTask!!.cancel(force)
                executor!!.shutdown()
                currentTask = null
                afterStop()
            }
            return isRunning
        }
    }
}
