/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.control.readvac

import hep.dataforge.context.Context
import hep.dataforge.control.devices.PortSensor
import hep.dataforge.control.devices.intState
import hep.dataforge.control.ports.GenericPortController
import hep.dataforge.control.ports.Port
import hep.dataforge.control.ports.PortFactory
import hep.dataforge.description.ValueDef
import hep.dataforge.meta.Meta
import hep.dataforge.states.StateDef
import hep.dataforge.values.ValueType.NUMBER
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.util.regex.Pattern

/**
 * @author Alexander Nozik
 */
@StateDef(value = ValueDef(name = "address", type = [NUMBER], def = "1", info = "A modbus address"), writable = true)
class MeradatVacDevice(context: Context, meta: Meta) : PortSensor(context, meta) {

    var address by intState("address")

    override fun connect(meta: Meta): GenericPortController {
        val port: Port = PortFactory.build(meta)
        logger.info("Connecting to port {}", port.name)

        return GenericPortController(context, port) { it.endsWith("\r\n") }
    }

    override fun getType(): String {
        return meta.getString("type", "numass.vac.vit")
    }

    override fun setMeasurement(oldMeta: Meta?, newMeta: Meta) {
        startMeasurement{
            doMeasure()
        }
    }


    private fun doMeasure(): Meta {
        val requestBase: String = String.format(":%02d", address)
        val dataStr = requestBase.substring(1) + REQUEST
        val query = requestBase + REQUEST + calculateLRC(dataStr) + "\r\n" // ":010300000002FA\r\n";
        val response: Pattern = Pattern.compile(requestBase + "0304(\\w{4})(\\w{4})..\r\n")

        val answer = sendAndWait(query) { phrase -> phrase.startsWith(requestBase) }

        if (answer.isEmpty()) {
            updateLogicalState(PortSensor.CONNECTED_STATE, false)
            return produceError("No signal")
        } else {
            val match = response.matcher(answer)

            return if (match.matches()) {
                val base = Integer.parseInt(match.group(1), 16).toDouble() / 10.0
                var exp = Integer.parseInt(match.group(2), 16)
                if (exp > 32766) {
                    exp -= 65536
                }
                var res = BigDecimal.valueOf(base * Math.pow(10.0, exp.toDouble()))
                res = res.setScale(4, RoundingMode.CEILING)
                updateLogicalState(PortSensor.CONNECTED_STATE, true)
                produceResult(res)
            } else {
                updateLogicalState(PortSensor.CONNECTED_STATE, false)
                produceError("Wrong answer: $answer")
            }
        }
    }


    companion object {
        private const val REQUEST = "0300000002"

        fun calculateLRC(inputString: String): String {
            /*
         * String is Hex String, need to convert in ASCII.
         */
            val bytes = BigInteger(inputString, 16).toByteArray()
            val checksum = bytes.sumBy { it.toInt() }
            var value = Integer.toHexString(-checksum)
            value = value.substring(value.length - 2).toUpperCase()
            if (value.length < 2) {
                value = "0$value"
            }

            return value
        }
    }
}
