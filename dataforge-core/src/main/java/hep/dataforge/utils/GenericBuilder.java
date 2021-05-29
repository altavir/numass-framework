/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.utils;

/**
 * A universal GenericBuilder pattern.
 * @param <T> The type of builder result
 * @param <B> the type of builder itself
 * @author Alexander Nozik 
 */
public interface GenericBuilder<T, B extends GenericBuilder> {
    /**
     * current state of the builder
     * @return 
     */
    B self();
    
    /**
     * Build resulting object
     * @return 
     */
    T build();

    //TODO seam builder after builder
}
