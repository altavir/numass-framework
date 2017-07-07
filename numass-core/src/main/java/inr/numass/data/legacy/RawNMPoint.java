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

import inr.numass.data.api.NumassEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Хранит информацию о спектре точки, но не об отдельных событиях.
 *
 * @author Darksnake
 */
public class RawNMPoint {

    public static final int MAX_EVENTS_PER_POINT = 260000;
    public static int MAX_CHANEL = 4095;

    private Instant startTime;
    private final List<NumassEvent> events;
    private double length;
    private double uread;

    private double uset;

    public RawNMPoint(double U, List<NumassEvent> events, double t) {
        this.uset = U;
        this.uread = U;
        this.events = events;
        this.length = t;
    }

    public RawNMPoint(double Uset, double Uread, List<NumassEvent> events, double t) {
        this.uset = Uset;
        this.uread = Uread;
        this.events = events;
        this.length = t;
    }

    public RawNMPoint(double uset, double uread, List<NumassEvent> events, double t, Instant absouteTime) {
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

//    @Override
//    public RawNMPoint clone() {
//        ArrayList<NumassEvent> newevents = new ArrayList<>();
//        newevents.addAll(this.getEvents());
//        return new RawNMPoint(getUset(), getUread(), newevents, getLength());
//    }

    public Instant getStartTime() {
        return startTime;
    }

    public double getCr() {
        return getEventsCount() / getLength();
    }

    public double getCrError() {
        return Math.sqrt(getEventsCount()) / getLength();
    }

    /**
     * @return the events
     */
    public List<NumassEvent> getEvents() {
        return events;
    }

    public long getEventsCount() {
        return events.size();
    }

    /**
     * Measurement time
     *
     * @return the tset
     */
    public double getLength() {
        if (Double.isNaN(length)) {
            throw new Error();
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
        List<NumassEvent> events = new ArrayList<>(this.events);
        for (NumassEvent newEvent : point.getEvents()) {
            events.add(new NumassEvent(newEvent.getChanel(), newEvent.getTime() + this.getLength()));
        }
        double length = this.length + point.length;
        double uread = (this.uread + point.uread) / 2;
        return new RawNMPoint(this.uset, uread, events, length, this.startTime);
    }

//    void putEvent(NumassEvent event) {
//        events.add(event);
//    }

    public RawNMPoint selectChanels(int from, int to) {
        assert to > from;

        List<NumassEvent> res = new ArrayList<>();
        for (NumassEvent event : this.getEvents()) {
            if ((event.getChanel() >= from) && (event.getChanel() <= to)) {
                res.add(event);
            }
        }
        return new RawNMPoint(getUset(), getUread(), res, getLength());
    }

//    void setStartTime(Instant absouteTime) {
//        this.startTime = absouteTime;
//    }
//
//    /**
//     * @param tset the tset to set
//     */
//    void setLength(double tset) {
//        this.length = tset;
//    }

//    /**
//     * @param Uread the Uread to set
//     */
//    void setUread(double Uread) {
//        assert Uread >= 0;
//        this.uread = Uread;
//    }
//
//    /**
//     * @param Uset the Uset to set
//     */
//    void setUset(double Uset) {
//        this.uset = Uset;
//    }

}
