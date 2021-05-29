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
package hep.dataforge.maths.integration;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Alexander Nozik
 */
public class IntegratorFactory {
    private static final Map<Integer, GaussRuleIntegrator> gaussRuleMap = new HashMap<>();
    
    public static GaussRuleIntegrator getGaussRuleIntegrator(int nodes){
        if(gaussRuleMap.containsKey(nodes)){
            return gaussRuleMap.get(nodes);
        } else {
            GaussRuleIntegrator integrator = new GaussRuleIntegrator(nodes);
            gaussRuleMap.put(nodes, integrator);
            return integrator;
        }
    }
}
