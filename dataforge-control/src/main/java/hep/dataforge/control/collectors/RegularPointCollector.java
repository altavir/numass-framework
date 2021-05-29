/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.control.collectors;

import hep.dataforge.utils.DateTimeUtils;
import hep.dataforge.values.Value;
import hep.dataforge.values.ValueFactory;
import hep.dataforge.values.ValueMap;
import hep.dataforge.values.Values;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * An averaging DataPoint collector that starts timer on first put operation and
 * forces collection when timer expires. If there are few Values with same time
 * during this period, they are averaged.
 *
 * @author <a href="mailto:altavir@gmail.com">Alexander Nozik</a>
 */
public class RegularPointCollector implements ValueCollector {

    private final Map<String, List<Value>> values = new ConcurrentHashMap<>();
    private final Consumer<Values> consumer;
    private final Duration duration;
    private Instant startTime;
    /**
     * The names that must be in the dataPoint
     */
    private List<String> names = new ArrayList<>();
    private Timer timer;

    public RegularPointCollector(Duration duration, Consumer<Values> consumer) {
        this.consumer = consumer;
        this.duration = duration;
    }

    public RegularPointCollector(Duration duration, Collection<String> names, Consumer<Values> consumer) {
        this(duration, consumer);
        this.names = new ArrayList<>(names);
    }

    @Override
    public void collect() {
        collect(DateTimeUtils.now());
    }

    public synchronized void collect(Instant time) {
        if(!values.isEmpty()) {
            ValueMap.Builder point = new ValueMap.Builder();

            Instant average = Instant.ofEpochMilli((time.toEpochMilli() + startTime.toEpochMilli()) / 2);

            point.putValue("timestamp", average);

            for (Map.Entry<String, List<Value>> entry : values.entrySet()) {
                point.putValue(entry.getKey(), entry.getValue().stream().mapToDouble(Value::getDouble).sum() / entry.getValue().size());
            }

            // filling all missing values with nulls
            for (String name : names) {
                if (!point.build().hasValue(name)) {
                    point.putValue(name, ValueFactory.NULL);
                }
            }

            startTime = null;
            values.clear();
            consumer.accept(point.build());
        }
    }

    @Override
    public synchronized void put(String name, Value value) {
        if (startTime == null) {
            startTime = DateTimeUtils.now();
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    collect();
                }
            }, duration.toMillis());
        }

        if (!values.containsKey(name)) {
            values.put(name, new ArrayList<>());
        }
        values.get(name).add(value);
    }

    private void cancel() {
        if (timer != null && startTime != null) {
            timer.cancel();
        }
    }

    @Override
    public void clear() {
        values.clear();

    }

    public void stop() {
        cancel();
        clear();
        startTime = null;
    }


}
