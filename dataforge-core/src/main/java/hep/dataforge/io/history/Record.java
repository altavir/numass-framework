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
package hep.dataforge.io.history;

import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaBuilder;
import hep.dataforge.meta.MetaMorph;
import hep.dataforge.utils.DateTimeUtils;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.String.format;

/**
 * <p>
 LogEntry class.</p>
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
public class Record implements MetaMorph {

    private static final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss.SSS").withZone(ZoneId.systemDefault());
    private final List<String> sourceTrace = new ArrayList<>();
    private final String message;
    private final Instant time;

    public Record(Meta meta) {
        this.time = meta.getValue("timestame").getTime();
        this.message = meta.getString("message");
        this.sourceTrace.addAll(Arrays.asList(meta.getStringArray("trace")));
    }

    public Record(Record entry, String traceAdd) {
        this.sourceTrace.addAll(entry.sourceTrace);
        if (traceAdd != null && !traceAdd.isEmpty()) {
            this.sourceTrace.add(0, traceAdd);
        }
        this.message = entry.message;
        this.time = entry.time;
    }

    public Record(Instant time, String message) {
        this.time = time;
        this.message = message;
    }

    public Record(String message) {
        this.time = DateTimeUtils.now();
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public Instant getTime() {
        return time;
    }
    
    public String getTraceString(){
        return String.join(".", sourceTrace);
    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    @Override
    public String toString() {
        String traceStr = getTraceString();
        if (traceStr.isEmpty()) {
            return format("(%s) %s", dateFormat.format(time), message);
        } else {
            return format("(%s) %s: %s", dateFormat.format(time), traceStr, message);
        }
    }

    @NotNull
    @Override
    public Meta toMeta() {
        return new MetaBuilder("record")
                .setValue("timestamp", time)
                .setValue("message", message)
                .setValue("trace", sourceTrace);
    }
}
