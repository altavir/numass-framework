/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.utils;

import hep.dataforge.meta.Meta;

/**
 *
 * @author Alexander Nozik
 */
@FunctionalInterface
public interface MetaFactory<T> {
    T build(Meta meta);
}
