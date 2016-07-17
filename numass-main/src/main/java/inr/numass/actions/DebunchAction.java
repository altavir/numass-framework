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
import hep.dataforge.description.ValueDef;
import hep.dataforge.exceptions.ContentException;
import hep.dataforge.io.reports.Reportable;
import hep.dataforge.meta.Laminate;
import inr.numass.storage.RawNMFile;
import inr.numass.storage.RawNMPoint;
import inr.numass.debunch.DebunchReport;
import inr.numass.debunch.FrameAnalizer;
import java.io.PrintWriter;

/**
 *
 * @author Darksnake
 */
@TypedActionDef(name = "debunch", inputType = RawNMFile.class, outputType = RawNMFile.class)
@ValueDef(name = "upperchanel", type = "NUMBER", def = "4095", info = "An upper chanel for debuncing")
@ValueDef(name = "lowerchanel", type = "NUMBER", def = "0", info = "A lower chanel for debuncing")
@ValueDef(name = "rejectprob", type = "NUMBER", def = "1e-5", info = "Rejection probability")
@ValueDef(name = "framelength", type = "NUMBER", def = "5", info = "Frame length in seconds")
@ValueDef(name = "maxcr", type = "NUMBER", def = "100", info = "Maximum count rate for debunching")
public class DebunchAction extends OneToOneAction<RawNMFile, RawNMFile> {

    @Override
    protected RawNMFile execute(Reportable log, String name, Laminate meta, RawNMFile source) throws ContentException {
        log.report("File {} started", source.getName());

        int upper = meta.getInt("upperchanel", RawNMPoint.MAX_CHANEL);
        int lower = meta.getInt("lowerchanel", 0);
        double rejectionprob = meta.getDouble("rejectprob", 1e-5);
        double framelength = meta.getDouble("framelength", 5);
        double maxCR = meta.getDouble("maxcr", 100d);

        RawNMFile res = new RawNMFile(source.getName());
        res.setHead(source.getHead());
        source.getData().stream().map((point) -> {
            double cr = point.selectChanels(lower, upper).getCR();
            if (cr < maxCR) {
                DebunchReport report = new FrameAnalizer(rejectionprob, framelength, lower, upper).debunchPoint(point);

                log.report("Debunching file '{}', point '{}': {} percent events {} percent time in bunches",
                        source.getName(), point.getUset(), report.eventsFiltred() * 100, report.timeFiltred() * 100);
                point = report.getPoint();
            }
            return point;
        }).forEach((point) -> {
            res.putPoint(point);
        });
        log.report("File {} completed", source.getName());

        log.getReport().print(new PrintWriter(buildActionOutput(name)));

//        res.configure(source.meta());
        return res;
    }

}
