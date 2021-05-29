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
import hep.dataforge.values.ValueFactory;

import java.util.List;

/**
 * <p>CustomMergeRule class.</p>
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
public class CustomMergeRule extends MergeRule {

    protected ListMergeRule<Value> valueMerger;
    protected ListMergeRule<Meta> elementMerger;

    /**
     * <p>Constructor for CustomMergeRule.</p>
     *
     * @param itemMerger    a {@link hep.dataforge.meta.ListMergeRule} object.
     * @param elementMerger a {@link hep.dataforge.meta.ListMergeRule} object.
     */
    public CustomMergeRule(ListMergeRule<Value> itemMerger, ListMergeRule<Meta> elementMerger) {
        this.valueMerger = itemMerger;
        this.elementMerger = elementMerger;
    }

    protected CustomMergeRule() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String mergeName(String mainName, String secondName) {
        return mainName;
    }

    @Override
    protected Value mergeValues(Name valueName, Value first, Value second) {
        return ValueFactory.of(valueMerger.merge(valueName.toString(), first.getList(), second.getList()));
    }

    @Override
    protected List<? extends Meta> mergeNodes(Name nodeName, List<? extends Meta> mainNodes, List<? extends Meta> secondaryNodes) {
        return elementMerger.merge(nodeName.toString(), mainNodes, secondaryNodes);
    }
}
