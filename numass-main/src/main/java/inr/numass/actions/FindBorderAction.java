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
import hep.dataforge.context.Context;
import hep.dataforge.description.TypedActionDef;
import hep.dataforge.exceptions.ContentException;
import hep.dataforge.io.ColumnedDataWriter;
import hep.dataforge.io.reports.Reportable;
import hep.dataforge.meta.Laminate;
import inr.numass.data.NMFile;
import inr.numass.data.NMPoint;
import java.io.OutputStream;

/**
 *
 * @author Darksnake
 */
@TypedActionDef(name = "findBorder", inputType = NMFile.class, outputType = NMFile.class)
public class FindBorderAction extends OneToOneAction<NMFile, NMFile> {

    @Override
    protected NMFile execute(Context context, Reportable log, String name, Laminate meta, NMFile source) throws ContentException {
        log.report("File {} started", source.getName());

        int upperBorder = meta.getInt("upper", 4096);
        int lowerBorder = meta.getInt("lower", 0);
        double substractReference = meta.getDouble("reference", 0);

        NMPoint referencePoint = null;
        if (substractReference > 0) {
            referencePoint = source.getByUset(substractReference);
            if (referencePoint == null) {
                log.report("Reference point {} not found", substractReference);
            }
        }

        BorderData bData = new BorderData(source, upperBorder, lowerBorder, referencePoint);

        OutputStream stream = buildActionOutput(context, name);

        ColumnedDataWriter.writeDataSet(stream, bData, String.format("%s : lower = %d upper = %d", source.getName(), lowerBorder, upperBorder));

        log.report("File {} completed", source.getName());
        return source;
    }

}
