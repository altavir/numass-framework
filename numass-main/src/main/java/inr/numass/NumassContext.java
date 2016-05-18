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
package inr.numass;

import hep.dataforge.actions.ActionManager;
import hep.dataforge.context.Context;
import hep.dataforge.description.ActionDescriptor;
import hep.dataforge.description.DescriptorFormatter;
import hep.dataforge.description.DescriptorUtils;
import hep.dataforge.description.TextDescriptorFormatter;
import hep.dataforge.exceptions.DescriptorException;
import hep.dataforge.maths.integration.GaussRuleIntegrator;
import hep.dataforge.maths.integration.UnivariateIntegrator;
import hep.dataforge.meta.Meta;
import java.io.PrintWriter;

/**
 *
 * @author Darksnake
 */
public class NumassContext extends Context {

    public static UnivariateIntegrator defaultIntegrator = new GaussRuleIntegrator(300);
    public static UnivariateIntegrator highDensityIntegrator = new GaussRuleIntegrator(500);

    public NumassContext(Context parent, Meta config) {
        super(parent, "numass", config);
        init();
    }

    public NumassContext(Context parent) {
        super(parent, "numass");
        init();
    }

    public NumassContext() {
        super("numass");
        init();
    }

    private void init() {
        setIO(new NumassIO());
        loadPlugin("inr.numass:numass");
    }

    public static void printDescription(Context context, boolean allowANSI) throws DescriptorException {
        PrintWriter writer = new PrintWriter(context.io().out());
        DescriptorFormatter formatter = new TextDescriptorFormatter(writer, allowANSI);
        writer.println("***Data description***");
        writer.print("  ");
        formatter.showDescription(
                DescriptorUtils.buildDescriptor(
                        DescriptorUtils.findAnnotatedElement("method::hep.dataforge.data.DataManager.read")
                ));
        writer.println("***Allowed actions***");

        for (ActionDescriptor descriptor : ActionManager.buildFrom(context).listActions()) {
            writer.print("  ");
            formatter.showDescription(descriptor);
        }
        writer.println("***End of actions list***");
        writer.flush();
    }

}
