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

import hep.dataforge.exceptions.NamingException;
import hep.dataforge.names.Name;

import java.util.Arrays;
import java.util.Optional;

/**
 * Сегмент пути. Представляет собой пару цель::имя. Если цель не указана или
 * пустая, используется цель по-умолчанию для данного провайдера
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
class PathSegment implements Path {

    private Name name;
    private String target;


    public PathSegment(String target, Name name) {
        this.name = name;
        this.target = target;
    }

    public PathSegment(String path) {
        if (path == null || path.isEmpty()) {
            throw new NamingException("Empty path");
        }
        if (path.contains(TARGET_SEPARATOR)) {
            String[] split = path.split(TARGET_SEPARATOR, 2);
            this.target = split[0];
            this.name = Name.Companion.of(split[1]);
        } else {
            this.target = EMPTY_TARGET;
            this.name = Name.Companion.of(path);
        }
    }

    @Override
    public Name getName() {
        return name;
    }


    @Override
    public Optional<Path> optTail() {
        return Optional.empty();
    }

    @Override
    public String getTarget() {
        if (target == null) {
            return EMPTY_TARGET;
        } else {
            return target;
        }
    }

    @Override
    public Path withTarget(String target) {
        return new PathSegment(target, name);
    }

    @Override
    public Path append(Path segment) {
        return new SegmentedPath(getTarget(), Arrays.asList(this, new PathSegment(segment.getTarget(), segment.getName())));
    }

    @Override
    public String toString() {
        if(target.isEmpty()){
            return getName().getUnescaped();
        } else {
            return target + ":" + getName().getUnescaped();
        }
    }
}
