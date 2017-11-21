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

import ch.qos.logback.classic.Level
import hep.dataforge.control.ports.Port
import org.slf4j.LoggerFactory
import java.util.*

/**
 *
 * @author Alexander Nozik
 */
object TestSynch {

    private val firstCurrent = 0.0

    /**
     * @param args the command line arguments
     * @throws java.lang.Exception
     */
    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {
        Locale.setDefault(Locale.US)// чтобы отделение десятичных знаков было точкой
        val rootLogger = LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME) as ch.qos.logback.classic.Logger
        rootLogger.level = Level.INFO

        val handler: Port
        val firstController: LambdaMagnet
        val secondController: LambdaMagnet

        //        String comName = "COM12";
        //        handler = new ComPort(comName);
        handler = VirtualLambdaPort("COM12", 1, 2, 3, 4)

        firstController = LambdaMagnet(handler, 1)
        //        secondController = new LambdaMagnet(handler, 2);
        secondController = SafeLambdaMagnet("TEST", handler, 2,
                object : SafeLambdaMagnet.SafeMagnetCondition {

                    //                    @Override
                    //                    public boolean isBlocking() {
                    //                        return false;
                    //                    }
                    override fun onFail() {
                        java.awt.Toolkit.getDefaultToolkit().beep()

                    }

                    override fun isSafe(address: Int, current: Double): Boolean {
                        return Math.abs(current - firstCurrent) <= 0.2
                    }
                })

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
            firstController.setOutputMode(true)
            firstController.startUpdateTask(1.0, 10)
            secondController.startUpdateTask(2.0, 10)
            System.`in`.read()
            firstController.stopMonitorTask()
            secondController.stopMonitorTask()
            secondController.stopUpdateTask()
            firstController.stopUpdateTask()
            secondController.setOutputMode(false)
            firstController.setOutputMode(false)
            System.exit(0)
        } finally {
            //            handler.close();
        }
    }

}
