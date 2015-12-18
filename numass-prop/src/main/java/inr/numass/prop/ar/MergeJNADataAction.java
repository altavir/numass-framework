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
package inr.numass.prop.ar;

import hep.dataforge.actions.OneToOneAction;
import hep.dataforge.meta.Meta;
import hep.dataforge.context.Context;
import hep.dataforge.description.ValueDef;
import hep.dataforge.description.TypedActionDef;
import hep.dataforge.io.log.Logable;
import hep.dataforge.values.ValueType;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author darksnake
 */
@TypedActionDef(name = "mergeJNA",inputType = JNAEpisode.class, outputType = JNAEpisode.class,  description = "Couple some spectra together")
@ValueDef(name = "coupling", def = "2", type = "NUMBER", info = "If not 1, than spectra are coupled and merged together with given group size")
public class MergeJNADataAction extends OneToOneAction<JNAEpisode, JNAEpisode> {

    public MergeJNADataAction(Context context, Meta annotation) {
        super(context, annotation);
    }

    @Override
    protected JNAEpisode execute(Logable log, Meta meta, JNAEpisode input){
        List<JNASpectrum> res = new ArrayList<>();
        int coupling = meta.getInt("coupling");

        int counter = 0;

        for (Iterator<JNASpectrum> iterator = input.iterator(); iterator.hasNext();) {
            List<JNASpectrum> mergeList = new ArrayList<>();
            for (int i = 0; i < coupling; i++) {
                if (iterator.hasNext()) {
                    mergeList.add(iterator.next());
                } else {
                    res.addAll(mergeList);
                    mergeList.clear();
                }

            }
            if (!mergeList.isEmpty()) {
                JNASpectrum spectrum = mergeList.get(0);
                for (int i = 1; i < mergeList.size(); i++) {
                    spectrum = spectrum.mergeWith("merge_"+Integer.toString(counter), mergeList.get(i));
                }
                res.add(spectrum);
                counter++;
            }
        }

        return new JNAEpisode(input.getName(), input.meta(), res);
    }

}
