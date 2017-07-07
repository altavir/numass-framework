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
package inr.numass.data.legacy;

import hep.dataforge.description.ValueDef;
import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaBuilder;
import hep.dataforge.names.NamedMetaHolder;
import inr.numass.data.api.NumassEvent;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Contains the whole data but requires a lot of memory
 *
 * @author Darksnake
 */
@ValueDef(name = "info", info = "file text header")
public class RawNMFile extends NamedMetaHolder {

//    public static String TYPE = ":data:numassdatafile";

    private final List<RawNMPoint> points = new ArrayList<>();


    public RawNMFile(String fileName) {
        super(fileName);
    }

    public RawNMFile(String name, Meta meta) {
        super(name, meta);
    }

    public RawNMFile(String name, String header) {
        super(name, new MetaBuilder("meta").setValue("info", header));
    }

    public String getHead() {
        return meta().getString("info", "");
    }

    @Deprecated
    public void generatePAW(OutputStream stream) {
        PrintWriter writer = new PrintWriter(new BufferedOutputStream(stream));
        long counter = 0;
        for (RawNMPoint point : this.getData()) {
            double U = point.getUread();
            for (NumassEvent event : point.getEvents()) {
                counter++;
                writer.printf("%d\t%f\t%d\t%.1f\t%.2f%n", counter, event.getTime(), event.getChanel(), point.getLength(), U);
            }

        }
        writer.flush();
    }

    /**
     * merge of all point with given Uset
     *
     * @param U
     * @return
     */
    public RawNMPoint getByUset(double U) {
        RawNMPoint res = null;

        for (RawNMPoint point : points) {
            if (point.getUset() == U) {
                if (res == null) {
                    res = point;
                } else {
                    res = res.merge(point);
                }
            }
        }
        return res;
    }


    /**
     * merge of all point with given Uread
     *
     * @param U
     * @return
     */
    public RawNMPoint getByUread(double U) {
        RawNMPoint res = null;

        for (RawNMPoint point : points) {
            if (point.getUread() == U) {
                if (res == null) {
                    res = point;
                } else {
                    res = res.merge(point);
                }
            }
        }
        return res;
    }

    /**
     * @return the data
     */
    public List<RawNMPoint> getData() {
        return points;
    }

//    public void putEvent(double U, short chanel, double time) {
//        for (RawNMPoint point : this.getData()) {
//            if (U == point.getUread()) {
//                point.putEvent(new NumassEvent(chanel, time));
//                return;
//            }
//        }
//        RawNMPoint newpoint = new RawNMPoint();
//        newpoint.putEvent(new NumassEvent(chanel, time));
//        this.putPoint(newpoint);
//    }

    public void putPoint(RawNMPoint point) {
        points.add(point);
    }

    public int size() {
        return this.points.size();
    }

}
