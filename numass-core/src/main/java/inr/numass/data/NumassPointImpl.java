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

import hep.dataforge.tables.ValueMap;
import hep.dataforge.values.Values;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * @author Darksnake
 */
public class NumassPointImpl implements NumassPoint {
    //TODO andThen to annotated and move some parameters to meta
    private final int[] spectrum;
    private Instant startTime;
    private long eventsCount;
    private double pointLength;
    private double u;

    public NumassPointImpl(double u, Instant startTime, double pointLength, int[] spectrum) {
        this.startTime = startTime;
        this.pointLength = pointLength;
        this.spectrum = spectrum;
        this.u = u;
        this.eventsCount = IntStream.of(spectrum).sum();
    }

    /**
     * @return the absouteTime
     */
    @Override
    public Instant getStartTime() {
        if (startTime == null) {
            return Instant.EPOCH;
        } else {
            return startTime;
        }
    }

    @Override
    public int getCount(int chanel) {
        return spectrum[chanel];
    }

    @Override
    public int getCountInWindow(int from, int to) {
        int res = 0;
        for (int i = from; i <= to; i++) {
            res += spectrum[i];
        }
        if (res == Integer.MAX_VALUE) {
            throw new RuntimeException("integer overflow in spectrum calculation");
        }
        return res;
    }

    @Override
    public List<Values> getData() {
        List<Values> data = new ArrayList<>();
        for (int i = 0; i < RawNMPoint.MAX_CHANEL; i++) {
            data.add(new ValueMap(dataNames, i, spectrum[i]));
        }
        return data;
    }

    /**
     * Events count including overflow
     *
     * @return
     */
    @Override
    public long getTotalCount() {
        return eventsCount;
    }


    /**
     * @return the pointLength
     */
    @Override
    public double getLength() {
        return pointLength;
    }

    /**
     * @return the u
     */
    @Override
    public double getVoltage() {
        return u;
    }

    @Override
    public int[] getSpectrum() {
        return spectrum;
    }
}
