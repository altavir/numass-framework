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
package inr.numass.control.magnet;

import hep.dataforge.control.ports.PortHandler;
import java.util.Locale;

/**
 *
 * @author Alexander Nozik
 */
public class TestController {

    /**
     * @param args the command line arguments
     * @throws java.lang.Exception
     */
    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.US);// чтобы отделение десятичных знаков было точкой
//        ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
//        rootLogger.setLevel(Level.INFO);

        PortHandler handler;
        MagnetController firstController;
        MagnetController secondController;

//        String comName = "COM12";
//        handler = new ComPortHandler(comName);
        handler = new VirtualLambdaPort("COM12", 1, 2, 3, 4);

        firstController = new MagnetController(handler, 1);
//        secondController = new MagnetController(handler, 4);
        secondController = new SafeMagnetController("TEST", handler, 4, (int address, double current) -> current < 1.0);

        MagnetStateListener listener = new MagnetStateListener() {

            @Override
            public void acceptStatus(String name,  MagnetStatus state) {
                System.out.printf("%s (%s): Im = %f, Um = %f, Is = %f, Us = %f;%n",
                        name,
                        state.isOutputOn(),
                        state.getMeasuredCurrent(),
                        state.getMeasuredVoltage(),
                        state.getSetCurrent(),
                        state.getSetVoltage()
                );
            }

            @Override
            public void acceptNextI(String name,  double nextI) {
                System.out.printf("%s: nextI = %f;%n", name, nextI);
            }

            @Override
            public void acceptMeasuredI(String name,  double measuredI) {
                System.out.printf("%s: measuredI = %f;%n", name, measuredI);
            }
        };

        firstController.setListener(listener);
        secondController.setListener(listener);

        try {
            firstController.startMonitorTask(2000);
            secondController.startMonitorTask(2000);
            secondController.setOutputMode(true);
            secondController.startUpdateTask(2.0, 1000);
            System.in.read();
            firstController.stopMonitorTask();
            secondController.stopMonitorTask();
            secondController.stopUpdateTask();
            secondController.setOutputMode(false);
        } finally {
//            handler.close();
        }
    }

}
