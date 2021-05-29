/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.stat.models;

import hep.dataforge.Named;
import hep.dataforge.context.Context;
import hep.dataforge.description.Described;
import hep.dataforge.meta.Meta;
import hep.dataforge.utils.ContextMetaFactory;
import org.jetbrains.annotations.NotNull;

/**
 * A factory
 *
 * @author Alexander Nozik
 */
public interface ModelFactory extends ContextMetaFactory<Model>, Named, Described {
    String MODEL_TARGET = "model";

    static ModelFactory build(String name, ContextMetaFactory<Model> factory) {
        return new ModelFactory() {

            @NotNull
            @Override
            public String getName() {
                return name;
            }

            @Override
            public Model build(Context context, Meta meta) {
                return factory.build(context, meta);
            }
        };
    }
}