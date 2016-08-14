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
import hep.dataforge.data.binary.Binary;
import hep.dataforge.description.NodeDef;
import hep.dataforge.description.TypedActionDef;
import hep.dataforge.description.ValueDef;
import hep.dataforge.exceptions.ContentException;
import hep.dataforge.meta.Laminate;
import inr.numass.storage.NMFile;
import inr.numass.storage.RawNMFile;

import static inr.numass.NumassIO.getNumassData;

/**
 *
 * @author Darksnake
 */
@TypedActionDef(name = "readData",
        inputType = Binary.class, outputType = NMFile.class, info = "Read binary numass data file")
@ValueDef(name = "fileName", info = "The name of the file. By default equals file name.")
@ValueDef(name = "HVdev", info = "Divider for HV measurements. Should be set to 1.0 for numass data 2014",
        def = "2.468555393226049", type = "NUMBER")
@ValueDef(name = "noUset", info = "If 'true', then Uset = Uread")
@NodeDef(name = "debunch", target = "class::inr.numass.actions.DebunchAction", info = "If given, governs debunching")
public class ReadNumassDataAction extends OneToOneAction<Binary, NMFile> {

    @Override
    protected NMFile execute(String name, Laminate meta, Binary source) throws ContentException {
//        log.logString("File '%s' started", source.getName());
        RawNMFile raw = getNumassData(source, meta);
        if (meta.getBoolean("paw", false)) {
            raw.generatePAW(buildActionOutput(name + ".paw"));
        }

        if (meta.getNodeNames(false).contains("debunch")) {
            DebunchAction debunch = new DebunchAction();
            Laminate laminate = new Laminate(meta.getNode("debunch"))
                    .setValueContext(getContext())
                    .setDescriptor(debunch.getDescriptor());
            raw = debunch.execute(name, laminate, raw);
        }

        NMFile result = new NMFile(raw);

        return result;
    }

}
