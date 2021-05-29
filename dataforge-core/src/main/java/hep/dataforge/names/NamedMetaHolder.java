/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.names;

import hep.dataforge.Named;
import hep.dataforge.exceptions.AnonymousNotAlowedException;
import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaHolder;


public class NamedMetaHolder extends MetaHolder implements Named {

    private String name;

    public NamedMetaHolder(String name, Meta meta) {
        super(meta);
        if ((name == null || name.isEmpty()) && getClass().isAnnotationPresent(AnonymousNotAlowed.class)) {
            throw new AnonymousNotAlowedException();
        }
        this.name = name;
    }

    /**
     * Create anonymous instance if it is allowed
     *
     * @param meta
     */
    public NamedMetaHolder(Meta meta) {
        this(null, meta);
    }

    /**
     * An instance with blank meta
     *
     * @param name
     */
    public NamedMetaHolder(String name) {
        this(name, null);
    }

    /**
     * An instance with blank meta
     */
    public NamedMetaHolder() {
        this(null, null);
    }

    @Override
    public String getName() {
        return this.name;
    }

    /**
     * Protected method to set name later. Use it with caution
     *
     * @param name
     */
    protected final void setName(String name) {
        this.name = name;
    }

}
