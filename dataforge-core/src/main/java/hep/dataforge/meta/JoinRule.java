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

import java.util.ArrayList;
import java.util.List;

/**
 * <p>JoinRule class.</p>
 *
 * @author darksnake
 * @version $Id: $Id
 */
public class JoinRule extends MergeRule {

    /**
     * {@inheritDoc}
     */
    @Override
    protected String mergeName(String mainName, String secondName) {
        return mainName;
    }

    @Override
    protected Value mergeValues(Name valueName, Value first, Value second) {
        List<Value> list = new ArrayList<>();
        list.addAll(first.getList());
        list.addAll(second.getList());
        return ValueFactory.of(list);
    }

    @Override
    protected List<? extends Meta> mergeNodes(Name nodeName, List<? extends Meta> mainNodes, List<? extends Meta> secondaryNodes) {
        List<Meta> list = new ArrayList<>();
        list.addAll(mainNodes);
        list.addAll(secondaryNodes);
        return list;
    }

}
