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

import java.util.*;
import java.util.stream.Collectors;

/**
 * Путь в формате target1::path1/target2::path2. Блоки между / называются
 * сегментами.
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
class SegmentedPath implements Path {

    @NotNull
    static SegmentedPath of(@NotNull String pathStr) {
        if (pathStr.isEmpty()) {
            throw new IllegalArgumentException("Empty argument in the path constructor");
        }
        String[] split = normalize(pathStr).split(PATH_SEGMENT_SEPARATOR);
        LinkedList<PathSegment> segments = new LinkedList<>();

        for (String segmentStr : split) {
            segments.add(new PathSegment(segmentStr));
        }

        String target = segments.get(0).getTarget();
        return new SegmentedPath(target, segments);
    }

    private final LinkedList<PathSegment> segments;

    /**
     * for target inheritance
     */
    private final String defaultTarget;

    SegmentedPath(String defaultTarget, Collection<PathSegment> segments) {
        if (segments.isEmpty()) {
            throw new IllegalArgumentException("Zero length paths are not allowed");
        }
        this.defaultTarget = defaultTarget;
        this.segments = new LinkedList<>(segments);
    }

    /**
     * remove leading and trailing separators
     *
     * @param path
     * @return
     */
    private static String normalize(String path) {
        String res = path.trim();
        // remove leading separators
        while (res.startsWith(PATH_SEGMENT_SEPARATOR)) {
            res = res.substring(1);
        }
        while (res.endsWith(PATH_SEGMENT_SEPARATOR)) {
            res = res.substring(0, res.length() - 1);
        }
        return res;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTarget() {
        String target = segments.getFirst().getTarget();
        if(target.isEmpty()){
            return defaultTarget;
        } else {
            return target;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Name getName() {
        return segments.getFirst().getName();
    }


    public PathSegment head() {
        return this.segments.peekFirst();
    }


    public int size() {
        return this.segments.size();
    }

    @Override
    public Optional<Path> optTail() {
        if (segments.size() <= 1) {
            return Optional.empty();
        } else {
            List<PathSegment> newSegments = segments.subList(1, segments.size());
            return Optional.of(new SegmentedPath(defaultTarget, newSegments));
        }
    }


//    public String getFinalTarget() {
//        // Идем по сегментам в обратном порядке и ищем первый раз, когда появляется объявленная цель
//        for (Iterator<PathSegment> it = segments.descendingIterator(); it.hasNext(); ) {
//            Path segment = it.next();
//            if (!segment.target().equals(EMPTY_TARGET)) {
//                return segment.target();
//            }
//        }
//        //Если цель не объявлена ни в одном из сегментов, возвращаем пустую цель
//        return EMPTY_TARGET;
//    }

    @Override
    public Path withTarget(String target) {
        return new SegmentedPath(target, segments);
    }

    @Override
    public Path append(Path segment) {
        List<PathSegment> list = new ArrayList<>(this.segments);
        list.add(new PathSegment(segment.getTarget(), segment.getName()));
        return new SegmentedPath(defaultTarget, list);
    }

    @Override
    public String toString() {
        return String.join("/",segments.stream().map(PathSegment::toString).collect(Collectors.toList()));
    }
}
