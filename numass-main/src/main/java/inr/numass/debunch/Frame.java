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

import inr.numass.data.api.NumassEvent;
import org.apache.commons.math3.distribution.PoissonDistribution;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Darksnake
 */
public class Frame {

    private final double begin;
    private final double end;
    private List<NumassEvent> events;
    private final int eventsCount;

    public Frame(double begin, double end, List<NumassEvent> events) {
        assert end > begin;
        this.begin = begin;
        this.end = end;
        this.events = events;
        this.eventsCount = events.size();
    }

    /**
     * Сокращенная версия для экономии памяти
     *
     * @param begin
     * @param end
     * @param count
     */
    public Frame(double begin, double end, int count) {
        assert end > begin;
        this.begin = begin;
        this.end = end;
        this.eventsCount = count;
    }

    public Frame cloneFast() {
        return new Frame(begin, end, eventsCount);
    }

    public double getBegin() {
        return begin;
    }

    public int getCount() {
        if (this.events != null) {
            return events.size();
        } else {
            return eventsCount;
        }
    }
    
    public double getCountRate(){
        return this.getCount() / this.length();
    }
    
    public double getCountRateError(){
        return Math.sqrt(this.getCount()) / this.length();
    }
    
    public double getEnd() {
        return end;
    }

    public List<NumassEvent> getEvents() {
        if(events!=null)
            return events;
        else
            return new ArrayList<>();
    }
    
    public double getProbability(double cr){
        PoissonDistribution distr = new PoissonDistribution(cr * this.length());
        return distr.probability(getCount());
    }

    public double length() {
        assert this.end > this.begin;
        return this.end - this.begin;
    }

}
