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
 * A single numass event with given amplitude ant time.
 *
 * @author Darksnake
 */
public class NumassEvent implements Comparable<NumassEvent>, Serializable {
    // channel
    protected final short chanel;
    //time in nanoseconds
    protected final long time;

    public NumassEvent(short chanel, long time) {
        this.chanel = chanel;
        this.time = time;
    }

    /**
     * @return the chanel
     */
    public short getChanel() {
        return chanel;
    }

    /**
     * @return the time
     */
    public long getTime() {
        return time;
    }

    public Instant getAbsoluteTime(Instant offset) {
        return offset.plusNanos(time);
    }

    @Override
    public int compareTo(@NotNull NumassEvent o) {
        return Long.compare(this.getTime(), o.getTime());
    }
}
