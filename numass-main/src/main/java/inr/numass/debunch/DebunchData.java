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
package inr.numass.debunch;

import inr.numass.data.NMEvent;
import inr.numass.data.RawNMPoint;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Хранит сортированный набор событий с возможностью вырезать куски и склеивать
 * концы
 *
 * @author Darksnake
 */
class DebunchData {

    /**
     * Удаляет из листа события в определенном диапазоне времени. При этом общее
     * время не изменяется, поэтому скорость счета меняется. Возвращает
     * количество удаленных событий.
     *
     * @param from
     * @param to
     * @return
     */
    private static List<NMEvent> removeFrame(List<NMEvent> events, Frame frame) {
        List<NMEvent> res = new ArrayList<>();
        for (NMEvent event : events) {
            if (event.getTime() >= frame.getEnd()) {
                res.add(new NMEvent(event.getChanel(), event.getTime() - frame.length()));
            } else if (event.getTime() <= frame.getBegin()) {
                res.add(event);
            }
        }
        return res;
    }

    private final List<Frame> bunches = new ArrayList<>();
    private final List<NMEvent> events;
    private final double length;

    public DebunchData(RawNMPoint point) {
        events = point.getEvents();
        Collections.sort(events, new EventComparator());
        length = point.getLength();
    }

    public Frame countInFrame(double start, double length, int lowerChanel, int upperChanel) {
        double end;

        if (start + length < this.getLength()) {
            end = start + length;
        } else {
            end = this.getLength();
        }

        ArrayList<NMEvent> sum = new ArrayList<>();

        int i = 0;
        while ((i < this.size()) && (events.get(i).getTime() < start)) {
            i++;
        }
        while ((i < this.size()) && (events.get(i).getTime() < end)) {
            if ((events.get(i).getChanel() >= lowerChanel) && (events.get(i).getChanel() <= upperChanel)) {
                sum.add(getEvents().get(i));
            }

            i++;
        }

        return new Frame(start, end, sum);

    }

    /**
     * Same as CountInFrame, but it does not copy all of the event times, only
     * total count in frame.
     *
     * @param start
     * @param length
     * @return
     */
    public Frame countInFrameFast(double start, double length, int lowerChanel, int upperChanel) {
        //PENDING  самый долгий метод
        if (start > this.getLength()) {
            throw new IllegalArgumentException();
        }

        double end;

        if (start + length < this.getLength()) {
            end = start + length;
        } else {
            end = this.getLength();
        }

        int sumCount = 0;

        int i = 0;
        while ((i < this.size()) && (events.get(i).getTime() < start)) {
            i++;
        }
        while ((i < this.size()) && (events.get(i).getTime() < end)) {
            if ((events.get(i).getChanel() >= lowerChanel) && (events.get(i).getChanel() <= upperChanel)) {
                sumCount++;
            }
            i++;
        }

        return new Frame(start, end, sumCount);

    }

    public List<Frame> getBunches() {
        return bunches;
    }

    /**
     * Возвращает скорректированную скорость счета по всему интервалу
     *
     * @return
     */
    public double getCountRate() {
        return this.size() / this.getLength();
    }

    /**
     * Медленный метод, вызывать минимальное количество рах
     *
     * @return
     */
    public List<NMEvent> getDebunchedEvents() {
        List<NMEvent> res = getEvents();
        for (Frame frame : getBunches()) {
            res = removeFrame(res, frame);
        }
        return res;
    }

    /**
     * Медленный метод, вызывать минимальное количество рах
     *
     * @return
     */
    public double getDebunchedLength() {
        double res = length;
        for (Frame frame : getBunches()) {
            res -= frame.length();
        }
        if (res > 0) {
            return res;
        } else {
            throw new RuntimeException("Zero length point after debunching");
        }
    }

    public List<NMEvent> getEvents() {
        return events;
    }

    /**
     * @return the length
     */
    public double getLength() {
        return length;
    }

    public void setAsBunch(Frame bunch) {
        //FIXME сделать проверку пересечения кадров        
        this.bunches.add(bunch);
    }

    public void setAsBunch(double from, double to) {
        assert to > from;
        setAsBunch(countInFrame(from, to - from,0, RawNMPoint.MAX_CHANEL));
    }

    public long size() {
        return this.getEvents().size();
    }

    private static class EventComparator implements Comparator<NMEvent> {

        @Override
        public int compare(NMEvent o1, NMEvent o2) {
            return (int) Math.signum(o1.getTime() - o2.getTime());
        }

    }

}
