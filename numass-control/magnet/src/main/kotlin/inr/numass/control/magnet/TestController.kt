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

import hep.dataforge.control.ports.Port
import java.util.*

/**
 *
 * @author Alexander Nozik
 */
object TestController {

    /**
     * @param args the command line arguments
     * @throws java.lang.Exception
     */
    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {
        Locale.setDefault(Locale.US)// чтобы отделение десятичных знаков было точкой
        //        ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        //        rootLogger.setLevel(Level.INFO);

        val handler: Port
        val firstController: LambdaMagnet
        val secondController: LambdaMagnet

        //        String comName = "COM12";
        //        handler = new ComPort(comName);
        handler = VirtualLambdaPort("COM12", 1, 2, 3, 4)

        firstController = LambdaMagnet(handler, 1)
        //        secondController = new LambdaMagnet(handler, 4);
        secondController = SafeLambdaMagnet("TEST", handler, 4, { address: Int, current: Double -> current < 1.0 })

        val listener = object : MagnetStateListener {

            override fun acceptStatus(name: String, state: MagnetStatus) {
                System.out.printf("%s (%s): Im = %f, Um = %f, Is = %f, Us = %f;%n",
                        name,
                        state.isOutputOn,
                        state.measuredCurrent,
                        state.measuredVoltage,
                        state.setCurrent,
                        state.setVoltage
                )
            }

            override fun acceptNextI(name: String, nextI: Double) {
                System.out.printf("%s: nextI = %f;%n", name, nextI)
            }

            override fun acceptMeasuredI(name: String, measuredI: Double) {
                System.out.printf("%s: measuredI = %f;%n", name, measuredI)
            }
        }

        firstController.listener = listener
        secondController.listener = listener

        try {
            firstController.startMonitorTask(2000)
            secondController.startMonitorTask(2000)
            secondController.setOutputMode(true)
            secondController.startUpdateTask(2.0, 1000)
            System.`in`.read()
            firstController.stopMonitorTask()
            secondController.stopMonitorTask()
            secondController.stopUpdateTask()
            secondController.setOutputMode(false)
        } finally {
            //            handler.close();
        }
    }

}
