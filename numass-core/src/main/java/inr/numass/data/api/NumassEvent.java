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
package inr.numass.data.api;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.time.Instant;

/**
 * A single numass event with given amplitude and time.
 *
 * @author Darksnake
 */
public class NumassEvent implements Comparable<NumassEvent>, Serializable {
    // channel
    private final short chanel;
    //The time of the block start
    private final Instant blockTime;
    //time in nanoseconds relative to block start
    private final long timeOffset;

    public NumassEvent(short chanel, Instant blockTime, long offset) {
        this.chanel = chanel;
        this.blockTime = blockTime;
        this.timeOffset = offset;
    }

    public NumassEvent(short chanel, long offset) {
        this(chanel, Instant.EPOCH, offset);
    }

    /**
     * @return the chanel
     */
    public short getChanel() {
        return chanel;
    }

    /**
     * time in nanoseconds relative to block start
     * @return the time
     */
    public long getTimeOffset() {
        return timeOffset;
    }

    public Instant getBlockTime() {
        return blockTime;
    }

    public Instant getTime() {
        return blockTime.plusNanos(timeOffset);
    }

    @Override
    public int compareTo(@NotNull NumassEvent o) {
        return this.getTime().compareTo(o.getTime());
    }
}
