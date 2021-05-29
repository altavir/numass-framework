/*
 * Copyright  2018 Alexander Nozik.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package hep.dataforge.connections;

import hep.dataforge.context.Context;
import hep.dataforge.meta.Meta;

/**
 * A factory SPI class for connections
 */
public interface ConnectionFactory {
    String getType();

    /**
     *
     * @param obj an object for which this connections is intended
     * @param context context of the connection (could be different from connectible context)
     * @param meta configuration for connection
     * @param <T> type of the connectible
     * @return
     */
    <T extends Connectible> Connection build(T obj, Context context, Meta meta);
}
