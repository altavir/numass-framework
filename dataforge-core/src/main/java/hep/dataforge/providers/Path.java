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

import hep.dataforge.names.Name;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * <p>
 * Path interface.</p>
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
public interface Path {

    String EMPTY_TARGET = "";
    String PATH_SEGMENT_SEPARATOR = "/";
    String TARGET_SEPARATOR = "::";

    static Path of(String path) {
        SegmentedPath p = SegmentedPath.of(path);

        if (p.optTail().isPresent()) {
            return p;
        } else {
            return p.head();
        }
    }

    /**
     * Create a path with given target override (even if it is provided by the path itself)
     *
     * @param target
     * @param path
     * @return
     */
    static Path of(String target, String path) {
        return of(path).withTarget(target);
    }

    @NotNull
    static Path of(String target, Name name) {
        return new PathSegment(target, name);
    }

    /**
     * The Name of first segment
     *
     * @return a {@link hep.dataforge.names.Name} object.
     */
    Name getName();

    /**
     * Returns non-empty optional containing the chain without first segment in case of chain path.
     *
     * @return
     */
    Optional<Path> optTail();

    /**
     * The target of first segment
     *
     * @return a {@link java.lang.String} object.
     */
    String getTarget();

    /**
     * Return new path with different target
     *
     * @return
     */
    Path withTarget(String target);

    /**
     * Create or append chain path
     *
     * @param segment
     * @return
     */
    Path append(Path segment);

    default Path append(String target, String name) {
        return append(Path.of(target, name));
    }

}
