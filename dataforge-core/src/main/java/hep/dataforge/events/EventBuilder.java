/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.events;

import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaBuilder;
import hep.dataforge.utils.DateTimeUtils;
import hep.dataforge.utils.GenericBuilder;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static hep.dataforge.events.Event.*;

/**
 * Th builder class for events
 *
 * @author Alexander Nozik
 */
public abstract class EventBuilder<E extends EventBuilder> implements GenericBuilder<Event, E> {

    protected final MetaBuilder builder = new MetaBuilder("event");
    private final Map<String, Object> objectMap = new HashMap<>();

    protected EventBuilder(String type) {
        this.builder.setValue(Event.EVENT_TYPE_KEY, type);
    }

    public static EventBuilder make(String type) {
        return new ConcreteEventBuilder(type);
    }

    public EventBuilder setTime(Instant time) {
        builder.setValue(EVENT_TIME_KEY, time);
        return this;
    }

    /**
     * Set time of the event to current time
     *
     * @return
     */
    public EventBuilder setTime() {
        builder.setValue(EVENT_TIME_KEY, DateTimeUtils.now());
        return this;
    }

    public EventBuilder setSource(String source) {
        builder.setValue(EVENT_SOURCE_KEY, source);
        return this;
    }

    public EventBuilder setPriority(int priority) {
        builder.setValue(EVENT_PRIORITY_KEY, priority);
        return this;
    }

    public EventBuilder setMetaNode(String nodeName, Meta... nodes) {
        builder.setNode(nodeName, nodes);
        return this;
    }

    public EventBuilder setMetaValue(String valueName, Object value) {
        builder.setValue(valueName, value);
        return this;
    }

    public Meta buildEventMeta() {
        if (!builder.hasValue(EVENT_TIME_KEY)) {
            setTime();
        }
        return builder.build();
    }

    @Override
    public Event build() {
        return new Event(buildEventMeta());
    }

    private static class ConcreteEventBuilder extends EventBuilder<EventBuilder> {

        public ConcreteEventBuilder(String type) {
            super(type);
        }

        @Override
        public EventBuilder self() {
            return this;
        }

    }

}
