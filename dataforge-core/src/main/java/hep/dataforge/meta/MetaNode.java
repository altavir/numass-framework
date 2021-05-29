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

import hep.dataforge.exceptions.NameNotFoundException;
import hep.dataforge.names.Name;
import hep.dataforge.providers.Provides;
import hep.dataforge.values.Value;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An immutable representation of annotation node. Descendants could be mutable
 * <p>
 * TODO Documentation!
 * </p>
 *
 * @author Alexander Nozik
 */
@MorphTarget(target = SealedNode.class)
public abstract class MetaNode<T extends MetaNode> extends Meta implements MetaMorph {
    public static final String DEFAULT_META_NAME = "meta";

    private static final long serialVersionUID = 1L;

    protected String name;
    protected final Map<String, List<T>> nodes = new LinkedHashMap<>();
    protected final Map<String, Value> values = new LinkedHashMap<>();


    protected MetaNode() {
        this(DEFAULT_META_NAME);
    }

    protected MetaNode(String name) {
        if(name == null){
            this.name = "";
        } else {
            this.name = name;
        }
    }

    protected MetaNode(Meta meta){
        this.name = meta.getName();

        meta.getValueNames(true).forEach(valueName -> {
            this.values.put(valueName, meta.getValue(valueName));
        });

        meta.getNodeNames(true).forEach(nodeName -> {
            this.nodes.put(nodeName, meta.getMetaList(nodeName).stream().map(this::cloneNode).collect(Collectors.toList()));
        });
    }

    /**
     * get the node corresponding to the first token of given path
     *
     * @param path
     * @return
     */
    protected T getHead(Name path) {
        return optHead(path)
                .orElseThrow(() -> new NameNotFoundException(path.toString()));
    }

    protected Optional<T> optHead(Name path) {
        Name head = path.getFirst();
        return optChildNodeItem(head.entry())
                .map(child -> MetaUtils.query(child, head.getQuery()).get(0));
    }

    @Provides(META_TARGET)
    @Override
    public Optional<Meta> optMeta(String path) {
        if(path.isEmpty()){
            return Optional.of(this);
        } else {
            return getMetaList(path).stream().findFirst().map(it -> it);
        }
    }

    /**
     * Return a node list using path notation and null if node does not exist
     *
     * @param path
     * @return
     */
    @SuppressWarnings("unchecked")
    protected List<T> getMetaList(Name path) {
        if (path.getLength() == 0) {
            throw new RuntimeException("Empty path not allowed");
        }
        List<T> res;
        if (path.getLength() == 1) {
            res = optChildNodeItem(path.ignoreQuery().toString()).orElse(Collections.emptyList());
        } else {
            res = optHead(path).map(it -> it.getMetaList(path.cutFirst())).orElse(Collections.emptyList());
        }

        if (!res.isEmpty() && path.hasQuery()) {
            //Filtering nodes using query
            return MetaUtils.query(res, path.getQuery());
        } else {
            return res;
        }
    }

    /**
     * Return a value using path notation and null if it does not exist
     *
     * @param path
     * @return
     */
    @SuppressWarnings("unchecked")
    public Optional<Value> optValue(Name path) {
        if (path.getLength() == 0) {
            throw new RuntimeException("Empty path not allowed");
        }
        if (path.getLength() == 1) {
            return Optional.ofNullable(values.get(path.toString()));
        } else {
            return optHead(path).flatMap( it -> it.optValue(path.cutFirst()));
        }
    }

    /**
     * Return a list of all nodes for given name filtered by query if it exists.
     * If node not found or there are no results for the query, the exception is
     * thrown.
     *
     * @param name
     * @return
     */
    @Override
    public List<T> getMetaList(String name) {
        return getMetaList(Name.Companion.of(name));
    }

    @Override
    public T getMeta(String path) {
        return getMetaList(path).stream().findFirst().orElseThrow(() -> new NameNotFoundException(path));
    }

    @Override
    public Optional<Value> optValue(@NotNull String name) {
        return optValue(Name.Companion.of(name));
    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    @Override
    public Stream<String> getNodeNames(boolean includeHidden) {
        Stream<String> res = this.nodes.keySet().stream();
        if (includeHidden) {
            return res;
        } else {
            return res.filter(it -> !it.startsWith("@"));
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    @Override
    public Stream<String> getValueNames(boolean includeHidden) {
        Stream<String> res = this.values.keySet().stream();
        if (includeHidden) {
            return res;
        } else {
            return res.filter(it -> !it.startsWith("@"));
        }
    }

    /**
     * Return a direct descendant node with given name. Return null if it is not found.
     *
     * @param name
     * @return
     */
    protected Optional<List<T>> optChildNodeItem(String name) {
        return Optional.ofNullable(nodes.get(name));
    }

    /**
     * Return value of this node with given name. Return null if it is not found.
     *
     * @param name
     * @return
     */
    protected Value getChildValue(String name) {
        return values.get(name);
    }

    protected boolean isValidElementName(String name) {
        return !(name.contains("[") || name.contains("]") || name.contains("$"));
    }


    /**
     * Create a deep copy of the node but do not set parent or name. Deep copy
     * does not clone listeners
     *
     * @param node
     * @return
     */
    protected abstract T cloneNode(Meta node);

    /**
     * Return type checked variant of this node. The operation does not create new node, just exposes generic-free variant of the node.
     *
     * @return
     */
    public abstract T self();


    @Override
    public boolean isEmpty() {
        return this.nodes.isEmpty() && this.values.isEmpty();
    }
}
