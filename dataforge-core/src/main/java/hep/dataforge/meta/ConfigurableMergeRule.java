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

import hep.dataforge.values.Value;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Настраиваемое правило объединения аннотаций
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
public class ConfigurableMergeRule extends CustomMergeRule {

    /**
     * <p>Constructor for ConfigurableMergeRule.</p>
     *
     * @param valueToJoin    a {@link java.util.Set} object.
     * @param elementsToJoin a {@link java.util.Set} object.
     */
    public ConfigurableMergeRule(Collection<String> valueToJoin, Collection<String> elementsToJoin) {
        setValueMerger(valueToJoin);
        setMetaMerger(elementsToJoin);
    }

    /**
     * <p>Constructor for ConfigurableMergeRule.</p>
     *
     * @param toJoin a {@link java.util.Set} object.
     */
    public ConfigurableMergeRule(Collection<String> toJoin) {
        this(toJoin, toJoin);
    }

    /**
     * <p>Constructor for ConfigurableMergeRule.</p>
     *
     * @param toJoin a {@link java.lang.String} object.
     */
    public ConfigurableMergeRule(String... toJoin) {
        this(Arrays.asList(toJoin));
    }

    private void setValueMerger(Collection<String> valueToJoin) {
        this.valueMerger = (String name, List<? extends Value> first, List<? extends Value> second) -> {
            //Объединяем, если элемент в списке на объединение
            if (valueToJoin.contains(name)) {
                List<Value> list = new ArrayList<>();
                list.addAll(first);
                list.addAll(second);
                return list;
            } else {
                return first;
            }
        };
    }

    private void setMetaMerger(Collection<String> metaToJoin) {
        this.elementMerger = (String name, List<? extends Meta> first, List<? extends Meta> second) -> {
            //Объединяем, если элемент в списке на объединение
            if (metaToJoin.contains(name)) {
                List<Meta> list = new ArrayList<>();
                list.addAll(first);
                list.addAll(second);
                return list;
            } else {
                // если не в списке, то заменяем
                return first;
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String mergeName(String mainName, String secondName) {
        return mainName;
    }


}
