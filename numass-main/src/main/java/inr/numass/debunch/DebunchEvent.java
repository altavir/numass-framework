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

/**
 *
 * @author Darksnake
 */
public class DebunchEvent extends NMEvent {

    public static double getEventWeight(NMEvent event) {
        if (event instanceof DebunchEvent) {
            return ((DebunchEvent) event).getWeight();
        } else {
            return 1;
        }

    }

    private double shift = 0;
    /**
     * В общем случае принимает значение от 0 (событие полностью выкинуто) до
     * 1(событие полностью принято)
     */
    private double weight;

    public DebunchEvent(NMEvent event, double weight) {
        super(event.getChanel(), event.getTime());
        this.weight = weight;
    }

    protected DebunchEvent(double weight, double shift, short chanel, double time) {
        super(chanel, time);
        this.weight = weight;
        this.shift = shift;
    }
    
    @Override
    public DebunchEvent clone() {
        return new DebunchEvent(weight, shift, chanel, time);
    }

    @Override
    public double getTime() {
        return super.getTime() + shift;
    }

    public double getWeight() {
        return this.weight;
    }

    /**
     * @param marker
     */
    public void setWeight(int marker){
        this.weight = marker;
    }

    public void shiftTime(double shift) {
        this.shift += shift;
    }

}
