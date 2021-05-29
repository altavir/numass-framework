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

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.connections;

import hep.dataforge.context.Context;
import hep.dataforge.meta.Meta;

import java.util.Objects;
import java.util.Optional;

/**
 * A connection which could be applied to object that could receive connection.
 * Usually connection does not invoke {@code open} method itself, but delegates it to {@code Connectible}
 *
 * @author Alexander Nozik
 */
public interface Connection extends AutoCloseable {

    /**
     * Create a connection using context connection factory provider if it is possible
     *
     * @param context
     * @param meta
     * @return
     */
    static Optional<Connection> buildConnection(Connectible obj, Context context, Meta meta) {
        String type = meta.getString("type");
        return Optional.ofNullable(context.findService(ConnectionFactory.class, it -> Objects.equals(it.getType(), type)))
                .map(it -> it.build(obj, context, meta));
    }

    String LOGGER_ROLE = "log";
    String EVENT_HANDLER_ROLE = "eventHandler";

    default boolean isOpen() {
        return true;
    }

    default void open(Object object) throws Exception {
        //do nothing
    }

    @Override
    default void close() throws Exception {
        //do nothing
    }
}
