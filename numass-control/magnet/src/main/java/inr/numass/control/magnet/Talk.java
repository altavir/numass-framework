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

import ch.qos.logback.classic.Level;
import hep.dataforge.control.ports.PortFactory;
import hep.dataforge.control.ports.PortHandler;
import hep.dataforge.exceptions.PortException;
import hep.dataforge.utils.DateTimeUtils;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

/**
 * @author Alexander Nozik
 */
public class Talk {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.US);
        ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(Level.INFO);

        String portName = "/dev/ttyr00";

        if (args.length > 0) {
            portName = args[0];
        }
        PortHandler handler;
        handler = PortFactory.getPort(portName);
        handler.setPhraseCondition((String str) -> str.endsWith("\r"));

//        MagnetController controller = new MagnetController(handler, 1);
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        System.out.printf("INPUT > ");
        String nextString = reader.readLine();

        while (!"exit".equals(nextString)) {
            try {
                Instant start = DateTimeUtils.now();
                String answer = handler.sendAndWait(nextString + "\r", 1000);
                //String answer = controller.request(nextString);                
                System.out.printf("ANSWER (latency = %s): %s;%n", Duration.between(start, DateTimeUtils.now()), answer.trim());
            } catch (PortException ex) {
                ex.printStackTrace();
            }
            System.out.printf("INPUT > ");
            nextString = reader.readLine();
        }

        handler.close();

    }

}
