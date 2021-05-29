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

import hep.dataforge.Named;
import hep.dataforge.exceptions.AnonymousNotAlowedException;
import hep.dataforge.utils.ReferenceRegistry;
import org.jetbrains.annotations.Nullable;
import org.slf4j.helpers.MessageFormatter;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * A in-memory log that can store a finite number of entries. The difference between logger events and log is that log
 * is usually part the part of the analysis result an should be preserved.
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
public class Chronicle implements History, Named {
    public static final String CHRONICLE_TARGET = "log";

    private final String name;
    private final ReferenceRegistry<Consumer<Record>> listeners = new ReferenceRegistry<>();
    private ConcurrentLinkedQueue<Record> entries = new ConcurrentLinkedQueue<>();
    private History parent;

    public Chronicle(String name, @Nullable History parent) {
        if (name == null || name.isEmpty()) {
            throw new AnonymousNotAlowedException();
        }
        this.name = name;
        this.parent = parent;
    }

    protected int getMaxLogSize() {
        return 1000;
    }

    @Override
    public void report(Record entry) {
        entries.add(entry);
        if (entries.size() >= getMaxLogSize()) {
            entries.poll();// Ограничение на размер лога
//            getLogger().warn("Log at maximum capacity!");
        }
        listeners.forEach((Consumer<Record> listener) -> {
            listener.accept(entry);
        });

        if (parent != null) {
            Record newEntry = pushTrace(entry, getName());
            parent.report(newEntry);
        }
    }

    /**
     * Add a weak report listener to this report
     *
     * @param logListener
     */
    public void addListener(Consumer<Record> logListener) {
        this.listeners.add(logListener, true);
    }

    private Record pushTrace(Record entry, String toTrace) {
        return new Record(entry, toTrace);
    }

    public void clear() {
        entries.clear();
    }

    public History getParent() {
        return parent;
    }

    public Stream<Record> getEntries(){
        return entries.stream();
    }

//    public void print(PrintWriter out) {
//        out.println();
//        entries.forEach((entry) -> {
//            out.println(entry.toString());
//        });
//        out.println();
//        out.flush();
//    }

    public Chronicle getChronicle() {
        return this;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void report(String str, Object... parameters) {
        Record entry = new Record(MessageFormatter.arrayFormat(str, parameters).getMessage());
        Chronicle.this.report(entry);
    }

    @Override
    public void reportError(String str, Object... parameters) {
        Chronicle.this.report(new Record("[ERROR] " + MessageFormatter.arrayFormat(str, parameters).getMessage()));
    }

}
