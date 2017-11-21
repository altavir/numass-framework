package inr.numass.control.magnet

import hep.dataforge.context.Context
import hep.dataforge.control.ports.GenericPortController
import hep.dataforge.control.ports.Port
import java.time.Duration

class LambdaPortController(context: Context, port: Port) : GenericPortController(context, port) {
    private var currentAddress: Int = -1;

    private fun setAddress(address: Int, timeout: Duration) {
        val response = sendAndWait("ADR $address\r", timeout) { true }.trim()
        if (response == "OK") {
            currentAddress = address
        } else {
            throw RuntimeException("Failed to set address to LAMBDA device on $port")
        }
    }

    /**
     * perform series of synchronous actions ensuring that all of them have the same address
     */
    fun <R> talk(address: Int, timeout: Duration, action: (GenericPortController) -> R): R {
        synchronized(this) {
            setAddress(address, timeout)
            return action(this)
        }
    }

//    override fun getContext(): Context = _context
//
//    private var address: Int = -1;
//
//    private val channel = ConflatedChannel<String>();
//
//    private val listeners = ReferenceRegistry<LambdaPortListener>()
//
//    fun open() {
//        try {
//            port.holdBy(this)
//            if (!port.isOpen) {
//                port.open()
//            }
//        } catch (e: PortException) {
//            throw RuntimeException("Can't hold the port $port LAMBDA port controller", e)
//        }
//    }
//
//
//    private suspend fun setAddress(address: Int, timeout: Duration) {
//        synchronized(this) {
//            port.send(this, "ADR $address\r")
//            val res = channel.receive()
//            if (res == "OK") {
//                this.address = address
//            }
//        }
//    }
//
//    private suspend fun sendMessage(message: String): String {
//
//    }
//
//    suspend fun fireSequence(address: Int, timeout: Duration, vararg messages: String) {
//        setAddress(address, timeout);
//        for (message in messages) {
//            sendMessage(message);
//        }
//    }
//
//    override fun close() {
//        port.releaseBy(this)
//    }
//
//    override fun acceptPhrase(message: String) {
//        async {
//            channel.send(message);
//        }
//        listeners.forEach {
//            if (it.address == address) {
//                context.parallelExecutor().submit { it.action(message) }
//            }
//        }
//    }
//
//    override fun acceptError(errorMessage: String?, error: Throwable?) {
//        listeners.forEach {
//            if (it.address == address) {
//                context.parallelExecutor().submit { it.onError(errorMessage, error) }
//            }
//        }
//    }
//
//    class LambdaPortListener(val address: Int, val action: (String) -> Unit, val onError: (String?, Throwable?) -> Unit = { _, _ -> })
}