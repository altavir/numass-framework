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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Хранит информацию о спектре точки, но не об отдельных событиях.
 * 
 * @author Darksnake
 */
public class RawNMPoint implements Cloneable {

    public static final int MAX_EVENTS_PER_POINT = 260000;
    public static int MAX_CHANEL = 4095;
    
    private Instant startTime;
    private final List<NMEvent> events;
    private double length;
    private double uread;

    private double uset;

    public RawNMPoint(double U, List<NMEvent> events, double t) {
        this.uset = U;
        this.uread = U;
        this.events = events;
        this.length = t;
    }

    public RawNMPoint(double Uset, double Uread, List<NMEvent> events, double t) {
        this.uset = Uset;
        this.uread = Uread;
        this.events = events;
        this.length = t;
    }

    public RawNMPoint(double uset, double uread, List<NMEvent> events, double t, Instant absouteTime) {
        this.uset = uset;
        this.uread = uread;
        this.length = t;
        this.startTime = absouteTime;
        this.events = events;
    }

    RawNMPoint() {
        events = new ArrayList<>();
        uset = 0;
        uread = 0;
        length = Double.NaN;
    }

    @Override
    public RawNMPoint clone() {
        ArrayList<NMEvent> newevents = new ArrayList<>();
        for (NMEvent event : this.getEvents()) {
            newevents.add(event.clone());
        }
        return new RawNMPoint(getUset(), getUread(), newevents, getLength());
    }

    public Instant getStartTime() {
        return startTime;
    }

    public double getCR() {
        return getEventsCount() / getLength();
    }

    public double getCRError() {
        return Math.sqrt(getEventsCount()) / getLength();
    }

    /**
     * @return the events
     */
    public List<NMEvent> getEvents() {
        return events;
    }

    public long getEventsCount() {
        return events.size();
    }

    /**
     * Measurement time
     * @return the tset
     */
    public double getLength() {
        if (Double.isNaN(length)) {
            throw new Error();
        }
        if(events.size()>MAX_EVENTS_PER_POINT){
            return events.get(events.size()-1).getTime()-events.get(0).getTime();
        }
        return length;
    }

    /**
     * @return the Uread
     */
    public double getUread() {
        if (uread <= 0) {
            return getUset();
        } else {
            return uread;
        }
    }

    /**
     * @return the Uset
     */
    public double getUset() {
        if (uset < 0) {
            throw new IllegalStateException();
        }
        return uset;
    }

    public RawNMPoint merge(RawNMPoint point) {
        RawNMPoint res = this.clone();
        for (NMEvent newEvent : point.getEvents()) {
            res.putEvent(new NMEvent(newEvent.getChanel(), newEvent.getTime() + this.getLength()));
        }
        res.length += point.getLength();
        res.uread = (this.uread + point.uread) / 2;
        return res;
    }

    void putEvent(NMEvent event) {
        events.add(event);
    }

    public RawNMPoint selectChanels(int from, int to) {
        assert to > from;

        List<NMEvent> res = new ArrayList<>();
        for (NMEvent event : this.getEvents()) {
            if ((event.getChanel() >= from) && (event.getChanel() <= to)) {
                res.add(event);
            }
        }
        return new RawNMPoint(getUset(), getUread(), res, getLength());
    }

    void setStartTime(Instant absouteTime) {
        this.startTime = absouteTime;
    }

    /**
     * @param tset the tset to set
     */
    void setLength(double tset) {
        this.length = tset;
    }

    /**
     * @param Uread the Uread to set
     */
    void setUread(double Uread) {
        assert Uread >= 0;
        this.uread = Uread;
    }

    /**
     * @param Uset the Uset to set
     */
    void setUset(double Uset) {
        this.uset = Uset;
    }

}
