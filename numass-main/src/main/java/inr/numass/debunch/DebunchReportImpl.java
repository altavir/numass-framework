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
import java.util.List;

/**
 *
 * @author Darksnake
 */
public class DebunchReportImpl implements DebunchReport {

    private final List<Frame> bunches;
    private final RawNMPoint pointAfter;
    private final RawNMPoint pointBefore;

    public DebunchReportImpl(RawNMPoint pointBefore, RawNMPoint pointAfter, List<Frame> bunches) {
        this.pointBefore = pointBefore;
        this.pointAfter = pointAfter;
        this.bunches = bunches;
    }

    DebunchReportImpl(RawNMPoint pointBefore, DebunchData debunchData) {
        this.pointBefore = pointBefore;
        pointAfter = new RawNMPoint(pointBefore.getUset(),pointBefore.getUread(), 
                debunchData.getDebunchedEvents(), debunchData.getDebunchedLength(),pointBefore.getStartTime());
        this.bunches = debunchData.getBunches();
    }
    
    
    
    @Override
    public double eventsFiltred() {
        return 1-(double)getPoint().getEventsCount()/getInitialPoint().getEventsCount();
    }

    @Override
    public List<NMEvent> getBunchEvents() {
        List<NMEvent> res = new ArrayList<>();
        for (Frame interval : getBunches()) {
            res.addAll(interval.getEvents());
        }
        return res;
    }

    @Override
    public List<Frame> getBunches() {
        return bunches;
    }

    @Override
    public RawNMPoint getInitialPoint() {
        return pointBefore;
    }

    @Override
    public RawNMPoint getPoint() {
        return pointAfter;
    }

    @Override
    public double timeFiltred() {
        return 1-getPoint().getLength()/getInitialPoint().getLength();
    }
    
    
}
