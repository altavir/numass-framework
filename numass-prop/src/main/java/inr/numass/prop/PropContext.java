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

import hep.dataforge.actions.ActionManager;
import hep.dataforge.datafitter.FitPlugin;
import hep.dataforge.datafitter.models.XYModel;
import hep.dataforge.functions.ParametricFunction;
import inr.numass.NumassContext;
import inr.numass.models.NBkgSpectrum;
import inr.numass.prop.ar.FitJNAData;
import inr.numass.prop.ar.MergeJNADataAction;
import inr.numass.prop.ar.ReadJNADataAction;

/**
 *
 * @author Darksnake
 */
public class PropContext extends NumassContext {

    public PropContext() {
        super();
        ActionManager actions = ActionManager.buildFrom(this);
        actions.registerAction(ReadJNADataAction.class);
        actions.registerAction(FitJNAData.class);
        actions.registerAction(MergeJNADataAction.class);
        super.provide("hep.dataforge:fitting",FitPlugin.class).getFitManager().getModelManager()
                .addModel("prop-response", (context, an) -> {
                    PropResolution trans = new PropResolution(new BaseFunction());

                    ParametricFunction response = trans.getResponseFunction("pos");
                    NBkgSpectrum spectrum = new NBkgSpectrum(response);

                    XYModel model = new XYModel("prop-response", spectrum, new PoissonAdapter(null, null));

                    return model;
                });
    }

}
