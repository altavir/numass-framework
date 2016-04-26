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
package inr.numass.prop;

import hep.dataforge.context.GlobalContext;
import hep.dataforge.tables.FileData;
import hep.dataforge.datafitter.MINUITPlugin;
import hep.dataforge.io.ColumnedDataWriter;
import hep.dataforge.meta.MetaBuilder;
import inr.numass.prop.ar.FitJNAData;
import inr.numass.prop.ar.JNAEpisode;
import inr.numass.prop.ar.ReadJNADataAction;
import java.io.File;
import java.io.FileNotFoundException;
import hep.dataforge.tables.Table;

/**
 *
 * @author Darksnake
 */
public class TestFit {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws FileNotFoundException, InterruptedException {
        GlobalContext.instance().loadPlugin(new MINUITPlugin());

        File sourceDir = new File("c:\\Users\\Darksnake\\Dropbox\\jna_data");

        FileData file = new FileData(new File(sourceDir, "ar37e2.dat"));
        file.setMeta(new MetaBuilder("meta")
                .putValue("timeFile", "tar37e2.dat")
                .putValue("temperatureFile", "e2temp.txt")
        );
        JNAEpisode spectra = new ReadJNADataAction(GlobalContext.instance(), null).runOne(file);

        Table data = new FitJNAData(GlobalContext.instance(), null).runOne(spectra);

        ColumnedDataWriter.writeDataSet(System.out, data, "***RESULT***");
    }

}
