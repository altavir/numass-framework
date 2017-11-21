/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.control.readvac

import hep.dataforge.context.Context
import hep.dataforge.control.devices.Device
import hep.dataforge.control.devices.PortSensor
import hep.dataforge.control.measurements.Measurement
import hep.dataforge.control.measurements.SimpleMeasurement
import hep.dataforge.control.ports.Port
import hep.dataforge.description.ValueDef
import hep.dataforge.exceptions.ControlException
import hep.dataforge.meta.Meta
import hep.dataforge.values.ValueType.NUMBER
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.util.regex.Pattern

/**
 * @author Alexander Nozik
 */
@ValueDef(name = "address", type = arrayOf(NUMBER), def = "1", info = "A modbus address")
class MeradatVacDevice(context: Context, meta: Meta) : PortSensor<Double>(context, meta) {

    @Throws(ControlException::class)
    override fun buildPort(portName: String): Port {
        val newHandler = super.buildPort(portName)
        newHandler.setDelimiter("\r\n")
        return newHandler
    }

    override fun createMeasurement(): Measurement<Double> = MeradatMeasurement()

    override fun getType(): String {
        return getMeta().getString("type", "Vit vacuumeter")
    }


    private inner class MeradatMeasurement : SimpleMeasurement<Double>() {

        private val query: String // ":010300000002FA\r\n";
        private val response: Pattern
        private val base: String

        init {
            base = String.format(":%02d", getMeta().getInt("address", 1))
            val dataStr = base.substring(1) + REQUEST
            query = base + REQUEST + calculateLRC(dataStr) + "\r\n"
            response = Pattern.compile(base + "0304(\\w{4})(\\w{4})..\r\n")
        }

        @Synchronized
        @Throws(Exception::class)
        override fun doMeasure(): Double? {

            val answer = sendAndWait(query) { phrase -> phrase.startsWith(base) }

            if (answer.isEmpty()) {
                this.updateMessage("No signal")
                updateState(PortSensor.CONNECTED_STATE, false)
                return null
            } else {
                val match = response.matcher(answer)

                if (match.matches()) {
                    val base = Integer.parseInt(match.group(1), 16).toDouble() / 10.0
                    var exp = Integer.parseInt(match.group(2), 16)
                    if (exp > 32766) {
                        exp -= 65536
                    }
                    var res = BigDecimal.valueOf(base * Math.pow(10.0, exp.toDouble()))
                    res = res.setScale(4, RoundingMode.CEILING)
                    this.updateMessage("OK")
                    updateState(PortSensor.CONNECTED_STATE, true)
                    return res.toDouble()
                } else {
                    this.updateMessage("Wrong answer: " + answer)
                    updateState(PortSensor.CONNECTED_STATE, false)
                    return null
                }
            }
        }

        override fun getDevice(): Device = this@MeradatVacDevice
    }

    companion object {
        private val REQUEST = "0300000002"

        fun calculateLRC(inputString: String): String {
            /*
         * String is Hex String, need to convert in ASCII.
         */
            val bytes = BigInteger(inputString, 16).toByteArray()
            val checksum = bytes.sumBy { it.toInt() }
            var value = Integer.toHexString(-checksum)
            value = value.substring(value.length - 2).toUpperCase()
            if (value.length < 2) {
                value = "0" + value
            }

            return value
        }
    }
}
