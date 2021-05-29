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
package hep.dataforge.meta;

import hep.dataforge.names.Name;
import hep.dataforge.values.Value;

import java.util.List;

/**
 * Always use the element from main meta
 *
 * @author Alexander Nozik
 */
class ReplaceRule extends MergeRule {


    /**
     * {@inheritDoc}
     */
    @Override
    protected String mergeName(String mainName, String secondName) {
        return mainName;
    }


    @Override
    protected Value mergeValues(Name valueName, Value first, Value second) {
        if (first.isNull()) {
            return second;
        } else return first;
    }

    @Override
    protected List<? extends Meta> mergeNodes(Name nodeName, List<? extends Meta> mainNodes, List<? extends Meta> secondaryNodes) {
        if (mainNodes.isEmpty()) {
            return secondaryNodes;
        } else return mainNodes;
    }
}
