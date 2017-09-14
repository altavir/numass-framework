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

import hep.dataforge.context.Global;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static java.time.temporal.ChronoUnit.SECONDS;

/**
 * Заплатка для задания поправки на масс-спектрометр
 *
 * @author Darksnake
 */
public class MonitorCorrector {

    private final double average;
    private final List<MonitorPoint> list;

    public MonitorCorrector(String path) throws ParseException, IOException {
        this(Global.instance().io().getFile(path));
    }

    public MonitorCorrector(Path monitorFile) throws ParseException, IOException {
        list = new ArrayList<>();
        
        BufferedReader reader = new BufferedReader(Files.newBufferedReader(monitorFile));
//        Scanner sc = new Scanner(monitorFile);

        double sum = 0;
        String str = reader.readLine();
        while ((str!=null)&&(!str.isEmpty())) {
            MonitorPoint point = new MonitorPoint(str);
            str = reader.readLine();
            list.add(point);
            sum += point.getMonitorValue();
        }
        average = sum / list.size();
    }

    /**
     * возвращает ближайшую по времени точку
     *
     * @param time
     * @return
     */
    public MonitorPoint findNearestMonitorPoint(LocalDateTime time) {
        MonitorPoint nearest = this.list.get(0);
        for (MonitorPoint point : this.list) {
            if (Math.abs(point.getTime().until(time, SECONDS)) 
                    < Math.abs(nearest.getTime().until(time, SECONDS))) {
                nearest = point;
            }
        }
        return nearest;

    }

    public double getCorrection(LocalDateTime start, double length) {
        LocalDateTime finish = start.plusSeconds((long) length);

        return (findNearestMonitorPoint(start).getMonitorValue() + findNearestMonitorPoint(finish).getMonitorValue()) / 2 / average;
    }

    public double getCorrectionError(LocalDateTime start, double length) {
        LocalDateTime finish = start.plusSeconds((long) length);

        return (findNearestMonitorPoint(start).getMonitorError() + findNearestMonitorPoint(finish).getMonitorError()) / 2 / average;
    }

}
