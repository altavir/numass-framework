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
package hep.dataforge.actions;

import hep.dataforge.data.DataNode;
import hep.dataforge.data.DataNodeBuilder;
import hep.dataforge.data.DataSet;
import hep.dataforge.data.NamedData;
import hep.dataforge.description.ValueDef;
import hep.dataforge.meta.Meta;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The class to builder groups of content with annotation defined rules
 *
 * @author Alexander Nozik
 */

public class GroupBuilder {

    /**
     * Create grouping rule that creates groups for different values of value
     * field with name {@code tag}
     *
     * @param tag
     * @param defaultTagValue
     * @return
     */
    public static GroupRule byValue(final String tag, String defaultTagValue) {
        return new GroupRule() {
            @Override
            public <T> List<DataNode<T>> group(DataNode<T> input) {
                Map<String, DataNodeBuilder<T>> map = new HashMap<>();

                input.forEach((NamedData<? extends T> data) -> {
                    String tagValue = data.getMeta().getString(tag, defaultTagValue);
                    if (!map.containsKey(tagValue)) {
                        DataNodeBuilder<T> builder = DataSet.Companion.edit(input.getType());
                        builder.setName(tagValue);
                        //builder.setMeta(new MetaBuilder(DEFAULT_META_NAME).putValue("tagValue", tagValue));
                        //PENDING share meta here?
                        map.put(tagValue, builder);
                    }
                    map.get(tagValue).add(data);
                });

                return map.values().stream().<DataNode<T>>map(DataNodeBuilder::build).collect(Collectors.toList());
            }
        };
    }

    @ValueDef(key = "byValue", required = true, info = "The name of annotation value by which grouping should be made")
    @ValueDef(key = "defaultValue", def = "default", info = "Default value which should be used for content in which the grouping value is not presented")
    public static GroupRule byMeta(Meta config) {
        //TODO expand grouping options
        if (config.hasValue("byValue")) {
            return byValue(config.getString("byValue"), config.getString("defaultValue", "default"));
        } else {
            return Collections::singletonList;
        }
    }

    public interface GroupRule {
        <T> List<DataNode<T>> group(DataNode<T> input);
    }
}
