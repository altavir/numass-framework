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
package hep.dataforge.tables;

import hep.dataforge.exceptions.NameNotFoundException;
import hep.dataforge.names.NameList;
import hep.dataforge.values.Value;
import hep.dataforge.values.Values;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;

/**
 * <p>
 * MaskPoint class.</p>
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
public class MaskPoint implements Values {

    private final Map<String, String> nameMap;
    private final Values source;
    private final NameList names;

    public MaskPoint(Values source, Map<String, String> nameMap) {
        this.source = source;
        this.nameMap = nameMap;
        names = new NameList(nameMap.keySet());
    }

    @Override
    public boolean hasValue(@NotNull String path) {
        return nameMap.containsKey(path);
    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    @Override
    public NameList getNames() {
        return names;
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public Optional<Value> optValue(@NotNull String name) throws NameNotFoundException {
        return source.optValue(nameMap.get(name));
    }

}
