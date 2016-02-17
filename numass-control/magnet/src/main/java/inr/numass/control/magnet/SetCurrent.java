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

import hep.dataforge.control.ports.ComPortHandler;
import hep.dataforge.control.ports.PortHandler;
import jssc.SerialPortException;

/**
 *
 * @author Alexander Nozik
 */
public class SetCurrent {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws SerialPortException {
        if (args.length < 3) {
            throw new IllegalArgumentException("Wrong number of parameters");
        }
        String comName = args[0];
        int lambdaaddress = Integer.valueOf(args[1]);
        double current = Double.valueOf(args[2]);

        PortHandler handler = new ComPortHandler(comName);

        MagnetController controller = new MagnetController(handler, lambdaaddress);

        controller.startUpdateTask(current, 500);
    }

}
