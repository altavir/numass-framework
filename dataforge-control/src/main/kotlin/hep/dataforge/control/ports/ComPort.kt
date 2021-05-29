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
package hep.dataforge.control.ports

import hep.dataforge.exceptions.PortException
import hep.dataforge.meta.Meta
import hep.dataforge.meta.buildMeta
import jssc.SerialPort
import jssc.SerialPort.*
import jssc.SerialPortEventListener
import jssc.SerialPortException
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * @author Alexander Nozik
 */
class ComPort(val address: String, val config: Meta) : Port() {

    override val name: String = if (address.startsWith("com::")) {
        address
    } else {
        "com::$address"
    }

    //    private static final int CHAR_SIZE = 1;
    //    private static final int MAX_SIZE = 50;
    private val port: SerialPort by lazy {
        SerialPort(name)
    }

    private val serialPortListener = SerialPortEventListener { event ->
        if (event.isRXCHAR) {
            val chars = event.eventValue
            try {
                val bytes = port.readBytes(chars)
                receive(bytes)
            } catch (ex: IOException) {
                throw RuntimeException(ex)
            } catch (ex: SerialPortException) {
                throw RuntimeException(ex)
            }
        }
    }

    override val isOpen: Boolean
        get() = port.isOpened


    override fun toString(): String {
        return name
    }

    @Throws(PortException::class)
    override fun open() {
        try {
            if (!port.isOpened) {
                port.apply {
                    openPort()
                    val baudRate = config.getInt("baudRate", BAUDRATE_9600)
                    val dataBits = config.getInt("dataBits", DATABITS_8)
                    val stopBits = config.getInt("stopBits", STOPBITS_1)
                    val parity = config.getInt("parity", PARITY_NONE)
                    setParams(baudRate, dataBits, stopBits, parity)
                    addEventListener(serialPortListener)
                }
            }
        } catch (ex: SerialPortException) {
            throw PortException("Can't open the port", ex)
        }

    }

    @Throws(PortException::class)
    fun clearPort() {
        try {
            port.purgePort(PURGE_RXCLEAR or PURGE_TXCLEAR)
        } catch (ex: SerialPortException) {
            throw PortException(ex)
        }

    }

    @Throws(Exception::class)
    override fun close() {
        port.let {
            it.removeEventListener()
            if (it.isOpened) {
                it.closePort()
            }
        }
        super.close()
    }

    @Throws(PortException::class)
    public override fun send(message: ByteArray) {
        if (!isOpen) {
            open()
        }
        launch {
            try {
                logger.debug("SEND: $message")
                port.writeBytes(message)
            } catch (ex: SerialPortException) {
                throw RuntimeException(ex)
            }
        }
    }

    override fun toMeta(): Meta = buildMeta {
        "type" to "com"
        "name" to this@ComPort.name
        "address" to address
        update(config)
    }

    companion object {

        /**
         * Construct ComPort with default parameters:
         *
         *
         * Baud rate: 9600
         *
         *
         * Data bits: 8
         *
         *
         * Stop bits: 1
         *
         *
         * Parity: non
         *
         * @param portName
         */
        @JvmOverloads
        fun create(portName: String, baudRate: Int = BAUDRATE_9600, dataBits: Int = DATABITS_8, stopBits: Int = STOPBITS_1, parity: Int = PARITY_NONE): ComPort {
            return ComPort(portName, buildMeta {
                setValue("type", "com")
                putValue("name", portName)
                putValue("baudRate", baudRate)
                putValue("dataBits", dataBits)
                putValue("stopBits", stopBits)
                putValue("parity", parity)
            })
        }
    }
}

