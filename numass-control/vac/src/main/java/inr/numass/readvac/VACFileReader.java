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
package inr.numass.readvac;

import hep.dataforge.data.DataPoint;
import hep.dataforge.data.MapPoint;
import hep.dataforge.io.LineIterator;
import java.io.File;
import java.io.FileNotFoundException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import hep.dataforge.data.PointParser;

/**
 *
 * @author Darksnake
 */
public class VACFileReader implements Iterator<DataPoint> {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");//14.04.2014 21:30:10

    public static VACFileReader fromDirectory(String dir) throws FileNotFoundException {
        File directory = new File(dir);
        String[] list = directory.list((File dir1, String name) -> name.startsWith("VacTMS") && name.endsWith(".txt"));
        if(list.length == 0){
            throw new FileNotFoundException("Data files not found in the given directory");
        }
        Arrays.sort(list);
        return new VACFileReader(new File(directory,list[list.length-1]));
    }

    public static VACFileReader fromFile(String file) throws FileNotFoundException {
        return new VACFileReader(new File(file));
    }

    private final LineIterator iterator;
    private final PointParser parser;

    private VACFileReader(File vacFile) throws FileNotFoundException {
        this.iterator = new LineIterator(vacFile);
        iterator.next();
        parser = new LikhovidVACParser();
    }

    public VACFileReader(File vacFile, PointParser parser) throws FileNotFoundException {
        this.iterator = new LineIterator(vacFile);
        iterator.next();
        this.parser = parser;
    }

    public DataPoint get(Instant time) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public DataPoint getLast() {
        DataPoint point = null;
        while (hasNext()) {
            point = next();
        }
        return point;
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public DataPoint next() {
        if (iterator.hasNext()) {
            return parser.parse(iterator.next());
        } else {
            return null;
        }
    }

    public List<DataPoint> updateFrom(Instant from) {
        List<DataPoint> res = new ArrayList<>();
        while (iterator.hasNext()) {
            DataPoint point = next();
            if (point != null && point.getValue("timestamp").timeValue().isAfter(from)) {
                res.add(point);
            }
        }
        return res;
    }

    public List<DataPoint> updateFrom() {
        List<DataPoint> res = new ArrayList<>();
        while (iterator.hasNext()) {
            DataPoint point = next();
            if (point != null) {
                res.add(point);
            }
        }
        return res;
    }

    private static class LikhovidVACParser implements PointParser {
        static final Pattern pattern = Pattern.compile("(\\S* \\S*)\\s*(\\S*);\\s*(\\S*)\\s*(\\S*)\\s*(\\S*)");
        @Override
        public DataPoint parse(String str) {
            Matcher matcher = pattern.matcher(str);
            if(!matcher.matches()){
                return null;
            }
            
            LocalDateTime dt = LocalDateTime.parse(matcher.group(1), formatter);
            Instant time = dt.toInstant(ZoneOffset.ofHours(0));
            String p1 = matcher.group(2);
            String p2 = matcher.group(3);            
            String p3 = matcher.group(4);            
            String px = matcher.group(5);            
            

            return new MapPoint(VACManager.names, new Object[]{time, p1, p2, p3, px});

        }
    }

}
