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

import java.util.List;
import java.util.Objects;

/**
 * That class that delegates methods to other observer giving fixed name prefix.
 * Is used to make child elements of annotation observable.
 *
 * @author Alexander Nozik
 */
public class DelegateConfigChangeListener implements ConfigChangeListener {

    private final ConfigChangeListener observer;
    private final Name prefix;

    public DelegateConfigChangeListener(ConfigChangeListener observer, Name prefix) {
        this.observer = observer;
        this.prefix = prefix;
    }

    @Override
    public void notifyValueChanged(@NotNull Name name, Value oldValues, Value newItems) {
        observer.notifyValueChanged(prefix.plus(name), oldValues, newItems);
    }

    @Override
    public void notifyNodeChanged(@NotNull Name name, @NotNull List<? extends Meta> oldValues, @NotNull List<? extends Meta> newItems) {
        observer.notifyNodeChanged(prefix.plus(name), oldValues, newItems);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 47 * hash + Objects.hashCode(this.observer);
        hash = 47 * hash + Objects.hashCode(this.prefix);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DelegateConfigChangeListener other = (DelegateConfigChangeListener) obj;
        return Objects.equals(this.observer, other.observer) && Objects.equals(this.prefix, other.prefix);
    }


}
