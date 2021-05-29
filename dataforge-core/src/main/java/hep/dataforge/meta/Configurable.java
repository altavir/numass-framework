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

/**
 * An object with mutable configuration
 *
 * @author Alexander Nozik
 */
public interface Configurable {

    /**
     * get editable configuration
     *
     * @return
     */
    Configuration getConfig();

    default Configurable configure(Meta config) {
        getConfig().update(config);
        return this;
    }

    default Configurable configureValue(String key, Object Value) {
        this.getConfig().setValue(key, Value);
        return this;
    }

    default Configurable configureNode(String key, Meta... node) {
        this.getConfig().setNode(key, node);
        return this;
    }
}
