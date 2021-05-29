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
package hep.dataforge.providers;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * A marker utility interface for providers.
 *
 * @author Alexander Nozik
 */
public interface Provider {


    default Optional<?> provide(Path path) {
        return Providers.provide(this, path);
    }

    /**
     * Stream of available names with given target. Only top level names are listed, no chain path.
     *
     * @param target
     * @return
     */
    default Stream<String> listContent(String target) {
        if (target.isEmpty()) {
            target = getDefaultTarget();
        }
        return Providers.listContent(this, target);
    }

    /**
     * Default target for this provider
     *
     * @return
     */
    default String getDefaultTarget() {
        return "";
    }

    /**
     * Default target for next chain segment
     *
     * @return
     */
    default String getDefaultChainTarget() {
        return "";
    }


    //utils


    /**
     * Type checked provide
     *
     * @param path
     * @param type
     * @param <T>
     * @return
     */
    default <T> Optional<T> provide(String path, Class<T> type) {
        return provide(Path.of(path)).map(type::cast);
    }

    default <T> Optional<T> provide(String target, String name, Class<T> type) {
        return provide(Path.of(target, name)).map(type::cast);
    }

    default <T> Optional<T> provide(Path path, Class<T> type) {
        return provide(path).map(type::cast);
    }

    /**
     * Stream of all elements with given target
     *
     * @param target
     * @param type
     * @param <T>
     * @return
     */
    default <T> Stream<T> provideAll(String target, Class<T> type) {
        return listContent(target).map(it -> provide(target, it, type).orElseThrow(() -> new IllegalStateException("The element " + it + " is declared but not provided")));
    }
}
