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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * An observer that could be attached to annotation and listen to any change
 * within it
 *
 * @author Alexander Nozik
 */
public interface ConfigChangeListener {

    /**
     * notify that value item is changed
     *
     * @param name    the full path of value that has been changed
     * @param oldItem the item of values before change. If null, than value
     *                has not been existing before change
     * @param newItem the item of values after change. If null, then value has
     *                been removed
     */
    void notifyValueChanged(@NotNull Name name, @Nullable Value oldItem, @Nullable Value newItem);

    /**
     * notify that element item is changed
     *
     * @param name    the full path of element that has been changed
     * @param oldItem the item of elements before change. If null, than item
     *                has not been existing before change
     * @param newItem the item of elements after change. If null, then item has
     *                been removed
     */
    default void notifyNodeChanged(@NotNull Name name, @NotNull List<? extends Meta> oldItem, @NotNull List<? extends Meta> newItem) {
        //do nothing by default
    }

}
