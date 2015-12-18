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
package inr.numass.data;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.Scanner;

/**
 *
 * @author Darksnake
 */
public class MonitorPoint {

//    private static SimpleDateFormat format = new SimpleDateFormat("yyyy:MM:dd:HH:mm:ss");

    private final double monitorError;
    private final double monitorValue;
    private final LocalDateTime time;

    public MonitorPoint(LocalDateTime time, double monitorValue, double monitorError) {
        this.time = time;
        this.monitorValue = monitorValue;
        this.monitorError = monitorError;
    }

    public MonitorPoint(String str) throws ParseException {
        Scanner sc = new Scanner(str);
        String datestr = sc.next();
        //using ISO-8601
        time = LocalDateTime.parse(datestr);
        monitorValue = sc.nextDouble();
        if (sc.hasNextDouble()) {
            monitorError = sc.nextDouble();
        } else {
            monitorError = 0;
        }
    }

    /**
     * @return the monitorError
     */
    public double getMonitorError() {
        return monitorError;
    }

    /**
     * @return the monitorValue
     */
    public double getMonitorValue() {
        return monitorValue;
    }

    /**
     * @return the time
     */
    public LocalDateTime getTime() {
        return time;
    }

}
