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

import hep.dataforge.Named;
import hep.dataforge.NamedKt;
import hep.dataforge.exceptions.AnonymousNotAlowedException;
import hep.dataforge.io.XMLMetaWriter;
import hep.dataforge.providers.Provider;
import hep.dataforge.providers.Provides;
import hep.dataforge.providers.ProvidesNames;
import hep.dataforge.values.Value;
import hep.dataforge.values.ValueProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The main building block of the DataForge.
 * <p>
 * TODO documentation here!
 * </p>
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
@MorphTarget(target = SealedNode.class)
public abstract class Meta implements Provider, Named, ValueProvider, MetaMorph, MetaProvider {

    private static final Meta EMPTY = new EmptyMeta();

    /**
     * Build an empty annotation with given name FIXME make a separate simple
     * class for empty annotation for performance
     *
     * @param name a {@link java.lang.String} object.
     * @return a {@link hep.dataforge.meta.Meta} object.
     */
    public static Meta buildEmpty(String name) {
        return new MetaBuilder(name).build();
    }

    /**
     * Empty anonymous meta
     *
     * @return
     */
    public static Meta empty() {
        return EMPTY;
    }

    /**
     * Return modifiable {@link MetaBuilder} witch copies data from this meta. Initial meta not changed.
     *
     * @return a {@link hep.dataforge.meta.MetaBuilder} object.
     */
    public MetaBuilder getBuilder() {
        return new MetaBuilder(this);
    }

    /**
     * Get guaranteed unmodifiable copy of this meta
     * @return
     */
    public SealedNode getSealed(){
        return new SealedNode(this);
    }

    /**
     * Return the meta node with given name
     *
     * @param path
     * @return
     */
    public abstract List<? extends Meta> getMetaList(String path);

    /**
     * Return index of given meta node inside appropriate meta list if it present.
     * Otherwise return -1.
     *
     * @param node
     * @return
     */
    public int indexOf(Meta node) {
        if (NamedKt.isAnonymous(node)) {
            throw new AnonymousNotAlowedException("Anonimous nodes are not allowed in 'indexOf'");
        }
        List<? extends Meta> list = getMetaList(node.getName());
        return list.indexOf(node);
    }

    @Provides(META_TARGET)
    public Optional<Meta> optMeta(String path) {
        return getMetaList(path).stream().findFirst().map(it -> it);
    }

    public abstract boolean isEmpty();

    /**
     * List value names of direct descendants. Excludes hidden values
     *
     * @return a {@link java.util.Collection} object.
     */
    @ProvidesNames(VALUE_TARGET)
    public final Stream<String> getValueNames() {
        return getValueNames(false);
    }

    public abstract Stream<String> getValueNames(boolean includeHidden);

    /**
     * List node names of direct descendants. Excludes hidden nodes
     *
     * @return a {@link java.util.Collection} object.
     */
    @ProvidesNames(META_TARGET)
    public final Stream<String> getNodeNames() {
        return getNodeNames(false);
    }

    public abstract Stream<String> getNodeNames(boolean includeHidden);


    /**
     * Return a child node with given name or empty node if child node not found
     *
     * @param path
     * @return
     */
    public final Meta getMetaOrEmpty(String path) {
        return getMeta(path, Meta.empty());
    }

    @Override
    public boolean equals(Object obj) {
        if (super.equals(obj)) {
            return true;
        } else if (obj instanceof Meta) {
            Meta other = (Meta) obj;
            return Objects.equals(getName(), other.getName()) && equalsIgnoreName(other);
        } else {
            return false;
        }
    }

    /**
     * Check if two annotations are equal ignoring their names. Names of child
     * elements are not ignored
     *
     * @param other
     * @return
     */
    public boolean equalsIgnoreName(Meta other) {
        boolean valuesEqual = getValueNames(true)
                .allMatch(valueName -> other.hasValue(valueName) && getValue(valueName).equals(other.getValue(valueName)));

        boolean nodesEqual = getNodeNames(true)
                .allMatch(nodeName -> other.hasMeta(nodeName) && getMetaList(nodeName).equals(other.getMetaList(nodeName)));

        return valuesEqual && nodesEqual;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + Objects.hashCode(getName());
        for (String valueName : getValueNames(true).collect(Collectors.toList())) {
            hash = 59 * hash + Objects.hashCode(getValue(valueName));
        }
        for (String elementName : getNodeNames(true).collect(Collectors.toList())) {
            hash = 59 * hash + Objects.hashCode(getMetaList(elementName));
        }
        return hash;
    }

    @Override
    public String toString() {
        return new XMLMetaWriter().writeString(this);
    }

    @Override
    public String getDefaultTarget() {
        return META_TARGET;
    }

    @Override
    public String getDefaultChainTarget() {
        return VALUE_TARGET;
    }

    @NotNull
    @Override
    public Meta toMeta() {
        return this;
    }


    private static class EmptyMeta extends Meta {

        @Override
        public List<? extends Meta> getMetaList(String path) {
            return Collections.emptyList();
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public Stream<String> getValueNames(boolean includeHidden) {
            return Stream.empty();
        }

        @Override
        public Stream<String> getNodeNames(boolean includeHidden) {
            return Stream.empty();
        }

        @NotNull
        @Override
        public String getName() {
            return "";
        }

        @Override
        public Optional<Value> optValue(@NotNull String path) {
            return Optional.empty();
        }
    }

}
