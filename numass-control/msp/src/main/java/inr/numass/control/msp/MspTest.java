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
package inr.numass.control.msp;

import hep.dataforge.exceptions.PortException;
import java.io.IOException;

/**
 *
 * @author darksnake
 */
public class MspTest {

    /**
     * @param args the command line arguments
     * @throws hep.dataforge.exceptions.PortException
     */
    public static void main(String[] args) throws PortException, IOException, InterruptedException {
//        Locale.setDefault(Locale.US);// чтобы отделение десятичных знаков было точкой
//        ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
//        rootLogger.setLevel(Level.INFO);
//
//        MspListener listener = new MspListener() {
//
//            @Override
//            public void acceptMeasurement(Map<Double, Double> measurement) {
//                final StringBuilder mesString = new StringBuilder("[");
//                measurement.forEach((Double key, Double value) -> mesString.append(String.format("%g:%g,", key, value)));
//                mesString.deleteCharAt(mesString.length() - 1);
//                mesString.append("]");
//                System.out.println("MEASUREMENT: " + mesString);
//            }
//
//            @Override
//            public void acceptMessage(String message) {
//                System.out.println("RECIEVE: " + message);
//            }
//
//            @Override
//            public void acceptRequest(String message) {
//                System.out.println("SEND: " + message);
//            }
//
//            @Override
//            public void error(String errorMessage, Throwable error) {
//                System.out.println("ERROR: " + errorMessage);
//                if (error != null) {
//                    error.printStackTrace();
//                }
//            }
//        };
//
//        MspDevice controller = new MspDevice("127.0.0.1", 10014, listener);
//        try {
//            controller.init();
//            String name = controller.createMeasurement("default", 2, 4, 18, 28);
//            controller.setFileamentOn(true);
//
//            controller.startMeasurement(name);
//            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
//            reader.readLine();
//        } finally {
//            controller.stop();
//            System.exit(0);
//        }
    }

}
