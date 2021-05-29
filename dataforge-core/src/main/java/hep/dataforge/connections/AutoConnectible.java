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

import java.util.Optional;
import java.util.stream.Stream;

public interface AutoConnectible extends Connectible {

    ConnectionHelper getConnectionHelper();

    @Override
    default void connect(Connection connection, String... roles) {
        getConnectionHelper().connect(connection, roles);
    }

    @Override
    default <T> Stream<T> connections(String role, Class<T> type) {
        return getConnectionHelper().connections(role, type);
    }

    default <T> Optional<T> optConnection(String role, Class<T> type) {
        return getConnectionHelper().optConnection(role, type);
    }

    @Override
    default void disconnect(Connection connection) {
        getConnectionHelper().disconnect(connection);
    }
}
