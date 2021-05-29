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
package hep.dataforge.stat.models;

import hep.dataforge.Named;
import hep.dataforge.context.BasicPlugin;
import hep.dataforge.context.Plugin;
import hep.dataforge.context.PluginDef;
import hep.dataforge.context.PluginFactory;
import hep.dataforge.meta.Meta;
import hep.dataforge.providers.Provides;
import hep.dataforge.providers.ProvidesNames;
import hep.dataforge.utils.ContextMetaFactory;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * The library of available models
 *
 * @author Alexander Nozik
 */
@PluginDef(group = "hep.dataforge", name = "models", info = "Storage plugin for fit models")
public class ModelLibrary extends BasicPlugin {
    public static final String MODEL_TARGET = "model";

    public static String MODEL_NAME = "modelName";


    private final Set<ModelFactory> factories = new HashSet<>();

    public void addModel(ModelFactory factory) {
        factories.add(factory);
    }

    public void addModel(String name, ContextMetaFactory<Model> mf) {
        addModel(ModelFactory.build(name, mf));
    }

    @Provides(MODEL_TARGET)
    public Optional<ModelFactory> findFactory(String name) {
        return factories.stream().filter(it -> it.getName().equals(name)).findFirst();
    }

    @ProvidesNames(MODEL_TARGET)
    public Stream<String> listModels() {
        return factories.stream().map(Named::getName);
    }

    @Override
    public String getDefaultTarget() {
        return MODEL_TARGET;
    }

    public static class Factory extends PluginFactory {
        @Override
        public Plugin build(Meta meta) {
            return new ModelLibrary();
        }

        @Override
        public Class<? extends Plugin> getType() {
            return ModelLibrary.class;
        }
    }
}
