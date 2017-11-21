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
package inr.numass.control.magnet

import hep.dataforge.control.ports.ComPort
import jssc.SerialPortException

/**
 *
 * @author Alexander Nozik
 */
object SetCurrent {

    /**
     * @param args the command line arguments
     */
    @Throws(SerialPortException::class)
    @JvmStatic
    fun main(args: Array<String>) {
        if (args.size < 3) {
            throw IllegalArgumentException("Wrong number of parameters")
        }
        val comName = args[0]
        val lambdaaddress = Integer.valueOf(args[1])!!
        val current = java.lang.Double.valueOf(args[2])

        val handler = ComPort(comName)

        val controller = LambdaMagnet(handler, lambdaaddress)

        controller.startUpdateTask(current, 500)
    }

}
