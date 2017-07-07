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
package inr.numass.data.legacy;

import hep.dataforge.description.ValueDef;
import hep.dataforge.meta.Meta;
import hep.dataforge.names.NamedMetaHolder;
import inr.numass.data.PointBuilders;
import inr.numass.data.api.NumassPoint;
import inr.numass.data.api.NumassSet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Объект, содержащий только спектры, но не сами события
 *
 * @author Darksnake
 */
@ValueDef(name = "numass.path", info = "Path to this data file in numass repository.")
@ValueDef(name = "numass.name", info = "The name of this data file.")
public class NMFile extends NamedMetaHolder implements NumassSet {

    private final List<NumassPoint> points;

    public NMFile(RawNMFile file) {
        super(file.getName(), file.meta());
        points = new ArrayList<>();
        for (RawNMPoint point : file.getData()) {
            points.add(PointBuilders.readRawPoint(point));
        }
    }

    public static NMFile readStream(InputStream is, String fname, Meta config) throws IOException {
        return new NMFile(new LegacyDataReader(is, fname, config).read());
    }

    public static NMFile readFile(File file) throws IOException {
        return new NMFile(new LegacyDataReader(file).read());
    }

    @Override
    public Stream<NumassPoint> getPoints() {
        return points.stream();
    }

    @Override
    public String getDescription() {
        return "";
    }
}
