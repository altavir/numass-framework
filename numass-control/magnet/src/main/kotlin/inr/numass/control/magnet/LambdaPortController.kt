package inr.numass.control.magnet

import hep.dataforge.control.ports.Port

class LambdaPortController(private val port: Port) : Port.PortController {

    private var address: Int = -1;

    init {
        port.holdBy(this)
    }

    override fun acceptPhrase(message: String?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun portError(errorMessage: String?, error: Throwable?) {
        super.portError(errorMessage, error)
    }
}