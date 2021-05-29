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
package hep.dataforge.utils;

import hep.dataforge.context.Context;
import hep.dataforge.context.Global;
import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaBuilder;

/**
 * A generic parameterized factory interface
 *
 * @author Alexander Nozik
 * @param <T>
 */
@FunctionalInterface
public interface ContextMetaFactory<T> {
    T build(Context context, Meta meta);

    default T build(){
        return build(Global.INSTANCE, MetaBuilder.buildEmpty(null));
    }
}
