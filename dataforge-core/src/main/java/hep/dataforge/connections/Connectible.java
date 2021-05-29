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

import hep.dataforge.UtilsKt;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Something that could be connected
 *
 * @author Alexander Nozik
 */
public interface Connectible {

    /**
     * Register a new connection with given roles
     *
     * @param connection
     * @param roles
     */
    void connect(Connection connection, String... roles);

    /**
     * Get a stream of all connections with given role and type. Role could be regexp
     *
     * @param role
     * @param type
     * @param <T>
     * @return
     */
    <T> Stream<T> connections(String role, Class<T> type);

    /**
     * Disconnect given connection
     *
     * @param connection
     */
    void disconnect(Connection connection);


    /**
     * For each connection of given class and role. Role may be empty, but type
     * is mandatory
     *
     * @param <T>
     * @param role
     * @param type
     * @param action
     */
    default <T> void forEachConnection(String role, Class<T> type, Consumer<T> action) {
        connections(role, type).forEach(action);
    }

    default <T> void forEachConnection(Class<T> type, Consumer<T> action) {
        forEachConnection(".*", type, action);
    }


    /**
     * A list of all available roles
     *
     * @return
     */
    default List<RoleDef> roleDefs() {
        return UtilsKt.listAnnotations(this.getClass(), RoleDef.class, true);
    }

    /**
     * Find a role definition for given name. Null if not found.
     *
     * @param name
     * @return
     */
    default Optional<RoleDef> optRoleDef(String name) {
        return roleDefs().stream().filter((roleDef) -> roleDef.name().equals(name)).findFirst();
    }

    /**
     * A quick way to find if this object accepts connection with given role
     *
     * @param name
     * @return
     */
    default boolean acceptsRole(String name) {
        return roleDefs().stream().anyMatch((roleDef) -> roleDef.name().equals(name));
    }
}
