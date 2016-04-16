/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.server;

import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaBuilder;
import java.io.Serializable;
import java.time.Instant;

/**
 * A progress note for numass run
 *
 * @author Alexander Nozik
 */
public class NumassNote implements Serializable {

    public static NumassNote buildFrom(Meta meta) {
        String text = meta.getString("text", "");
        if (meta.hasValue("time")) {
            Instant time = meta.getValue("time").timeValue();
            return new NumassNote(text, time);
        } else {
            return new NumassNote(text);
        }
    }

    private final String content;
    private final String ref;
    private final Instant time;

    public NumassNote(String content, Instant time) {
        this.content = content;
        this.time = time;
        this.ref = "#" + time.hashCode();
    }

    public NumassNote(String content) {
        this.content = content;
        this.time = Instant.now();
        this.ref = "#" + time.hashCode();
    }

    /**
     * Text content
     *
     * @return
     */
    public String content() {
        return content;
    }

    /**
     *
     * @return
     */
    public Instant time() {
        return time;
    }

    /**
     * Unique note name for references
     *
     * @return
     */
    public String ref() {
        return ref;
    }

    public Meta toMeta() {
        return new MetaBuilder("note").putValue("time", time)
                .putValue("ref", ref)
                .putValue("text", content);
    }
}
