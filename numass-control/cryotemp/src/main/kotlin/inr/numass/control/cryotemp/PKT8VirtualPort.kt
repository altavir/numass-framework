/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.control.cryotemp

import hep.dataforge.control.ports.VirtualPort
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaUtils
import hep.dataforge.meta.Metoid
import hep.dataforge.values.asValue
import java.time.Duration
import java.util.*
import java.util.function.Supplier


/**
 * @author Alexander Nozik
 */
class PKT8VirtualPort(private val portName: String, meta: Meta) : VirtualPort(meta), Metoid {

    private val generator = Random()

    override val name: String = portName

    @Synchronized override fun evaluateRequest(request: String) {
        when (request) {
            "s" -> {
                val letters = arrayOf("a", "b", "c", "d", "e", "f", "g", "h")
                for (letter in letters) {
                    val channelMeta = MetaUtils.findNodeByValue(meta, "channel", "letter", letter.asValue()).orElse(Meta.empty())

                    val average: Double
                    val sigma: Double
                    if (channelMeta != null) {
                        average = channelMeta.getDouble("av", 1200.0)
                        sigma = channelMeta.getDouble("sigma", 50.0)
                    } else {
                        average = 1200.0
                        sigma = 50.0
                    }

                    this.planRegularResponse(
                            Supplier {
                                val res = average + generator.nextGaussian() * sigma
                                //TODO convert double value to formatted string
                                String.format("%s000%d", letter, (res * 100).toInt())
                            },
                            Duration.ZERO, Duration.ofMillis(500), letter, "measurement"
                    )
                }
                return
            }
            "p" -> {
                cancelByTag("measurement")
                planResponse("Stopped", Duration.ofMillis(50))
            }
        }
    }

    @Throws(Exception::class)
    override fun close() {
        cancelByTag("measurement")
        super.close()
    }

}
