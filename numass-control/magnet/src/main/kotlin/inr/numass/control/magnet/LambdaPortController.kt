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
}