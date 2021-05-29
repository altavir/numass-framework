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
package hep.dataforge.events;

import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaBuilder;
import hep.dataforge.meta.SimpleMetaMorph;
import hep.dataforge.utils.DateTimeUtils;

import java.time.Instant;

/**
 * A metamorph representing framework event
 *
 * @author Alexander Nozik
 */
public class Event extends SimpleMetaMorph {
    public static final String EVENT_PRIORITY_KEY = "priority";
    public static final String EVENT_TYPE_KEY = "type";
    public static final String EVENT_SOURCE_KEY = "sourceTag";
    public static final String EVENT_TIME_KEY = "time";

    /**
     * Create an event with given basic parameters and additional meta data. All
     * values except type could be null or empty
     *
     * @param type
     * @param source
     * @param priority
     * @param time
     * @param additionalMeta
     */
    public static Event make(String type, String source, int priority, Instant time, Meta additionalMeta) {
        MetaBuilder builder = new MetaBuilder("event");
        if (additionalMeta != null) {
            builder.update(additionalMeta.getBuilder());
        }

        builder.setValue(EVENT_TYPE_KEY, type);

        if (time == null) {
            time = DateTimeUtils.now();
        }
        builder.setValue(EVENT_TIME_KEY, time);
        if (source != null && !source.isEmpty()) {
            builder.setValue(EVENT_SOURCE_KEY, source);
        }
        if (priority != 0) {
            builder.setValue(EVENT_PRIORITY_KEY, priority);
        }
        return new Event(builder.build());
    }

    //TODO add source context to event?

    public Event(Meta meta) {
        super(meta);
    }

    public int priority() {
        return getMeta().getInt(EVENT_PRIORITY_KEY, 0);
    }

    public String type() {
        return getMeta().getString(EVENT_TYPE_KEY);
    }

    public String sourceTag() {
        return getMeta().getString(EVENT_SOURCE_KEY, "");
    }

    public Instant time() {
        return getMeta().getValue(EVENT_TIME_KEY).getTime();
    }

//    /**
//     * get event string representation (header) to write in logs
//     *
//     * @return
//     */
//    @Override
//    String toString();
}
