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
package inr.numass.actions;

import hep.dataforge.actions.OneToOneAction;
import hep.dataforge.description.TypedActionDef;
import hep.dataforge.exceptions.ContentException;
import hep.dataforge.io.ColumnedDataWriter;
import hep.dataforge.meta.Laminate;
import hep.dataforge.meta.Meta;
import inr.numass.storage.NMFile;
import inr.numass.storage.RawNMPoint;
import org.apache.commons.math3.util.Pair;

import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Darksnake
 */
@TypedActionDef(name = "slicing", inputType = NMFile.class, outputType = NMFile.class)
public class SlicingAction extends OneToOneAction<NMFile, NMFile> {

    public static final String name = "slicing";

    @Override
    public String getName() {
        return name;
    }

    @Override
    protected NMFile execute(String name, Laminate meta, NMFile source) throws ContentException {
        boolean normalize;
        Map<String, Pair<Integer, Integer>> slicingConfig;

        LinkedHashMap<String, Pair<Integer, Integer>> res = new LinkedHashMap<>();
        List<? extends Meta> list = meta.getNode("sliceconfig").getNodes("slicepoint");

        for (Meta slice : list) {
            String title = slice.getString("title", slice.getName());
            int from = slice.getInt("from", 0);
            int to = slice.getInt("to", RawNMPoint.MAX_CHANEL);
            res.put(title, new Pair<>(from, to));
        }
        slicingConfig = res;

        normalize = meta.getBoolean("normalize", false);

        if (slicingConfig == null) {
            throw new RuntimeException("Slice configuration not defined");
        }
        report(name, "File {} started", source.getName());

        SlicedData sData = new SlicedData(source, slicingConfig, normalize);

        OutputStream stream = buildActionOutput(name);

        ColumnedDataWriter.writeDataSet(stream, sData, null);

        report(name, "File {} completed", source.getName());

        return source;
    }

}
