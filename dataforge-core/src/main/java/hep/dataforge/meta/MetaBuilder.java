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
import hep.dataforge.utils.GenericBuilder;
import hep.dataforge.values.Value;
import hep.dataforge.values.ValueProvider;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/**
 * A convenient builder to construct immutable or mutable annotations. or
 * configurations. All passed annotations are recreated as AnnotationBuilders,
 * "forgetting" previous parents and listeners if any.
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
public class MetaBuilder extends MutableMetaNode<MetaBuilder> implements GenericBuilder<Meta, MetaBuilder> {

    public MetaBuilder() {
        super();
    }

    //    private ValueProvider valueContext;
    public MetaBuilder(String name) {
        super(name);
    }


    /**
     * A deep copy constructor
     *
     * @param meta
     */
    public MetaBuilder(Meta meta) {
        super(meta.getName());
        meta.getValueNames(true).forEach((valueName) -> {
            setValueItem(valueName, meta.getValue(valueName));
        });

        meta.getNodeNames(true).forEach((elementName) -> {
            List<MetaBuilder> item = meta.getMetaList(elementName).stream()
                    .map(MetaBuilder::new)
                    .collect(Collectors.toList());
            setNodeItem(elementName, new ArrayList<>(item));
        });
    }

    /**
     * return an immutable meta based on this builder
     *
     * @return a {@link hep.dataforge.meta.Meta} object.
     */
    @Override
    public Meta build() {
        return new SealedNode(this);
    }

    public MetaBuilder rename(String newName) {
        if (Objects.equals(this.getName(), newName)) {
            return this;
        } else {
            return super.setName(newName);
        }
    }

    @Override
    public MetaBuilder getParent() {
        return super.getParent();
    }

    public MetaBuilder setNode(String name, Collection<? extends Meta> elements) {
        if (elements == null || elements.isEmpty()) {
            super.removeNode(name);
        } else {
            super.setNode(name);
            for (Meta element : elements) {
                MetaBuilder newElement = new MetaBuilder(element);
                if (!name.equals(newElement.getName())) {
                    newElement.rename(name);
                }
                super.putNode(newElement);
            }
        }
        return self();
    }

    public MetaBuilder updateNode(String name, Meta element) {
        return updateNode(name, MergeRule.replace(), element);
    }

    /**
     * Update an element or element item using given merge rule
     *
     * @param name
     * @param rule
     * @param elements
     * @return
     */
    public MetaBuilder updateNode(String name, MergeRule rule, Meta... elements) {
        if (!hasMeta(name)) {
            MetaBuilder.this.setNode(name, elements);
        } else {
            Name n = Name.Companion.of(name);
            if (n.getLength() == 1) {
                optChildNodeItem(name).ifPresent(list -> {
                    if (list.size() != elements.length) {
                        throw new RuntimeException("Can't update element item with an item of different size");
                    } else {
                        MetaBuilder[] newList = new MetaBuilder[list.size()];
                        for (int i = 0; i < list.size(); i++) {
                            newList[i] = rule.merge(elements[i], list.get(i)).rename(name);
                        }
                        super.setNode(name, newList);
                    }
                });
            } else {
                getMeta(n.cutLast().toString()).updateNode(n.getLast().toString(), rule, elements);
            }
        }


        return this;
    }

    /**
     * Update this annotation with new Annotation
     *
     * @param annotation    a {@link hep.dataforge.meta.MetaBuilder} object.
     * @param valueMerger   a {@link hep.dataforge.meta.ListMergeRule} object.
     * @param elementMerger a {@link hep.dataforge.meta.ListMergeRule} object.
     * @return a {@link hep.dataforge.meta.MetaBuilder} object.
     */
    public MetaBuilder update(Meta annotation,
                              ListMergeRule<Value> valueMerger,
                              ListMergeRule<Meta> elementMerger) {
        return new CustomMergeRule(valueMerger, elementMerger).mergeInPlace(annotation, this);
    }

    public MetaBuilder update(Meta meta) {
        MergeRule rule;
        switch (meta.getString("@mergeRule", "replace")) {
            case "join":
                rule = new JoinRule();
                break;
            default:
                rule = new ReplaceRule();
        }
        return rule.mergeInPlace(meta, this);
    }

    /**
     * Update values (replacing existing ones) from map
     *
     * @param values
     * @return
     */
    public MetaBuilder update(Map<String, ?> values) {
        values.forEach(this::setValue);
        return self();
    }

    @Override
    public MetaBuilder self() {
        return this;
    }

    /**
     * Create an empty child node
     *
     * @param name
     * @return
     */
    @Override
    protected MetaBuilder createChildNode(String name) {
        return new MetaBuilder(name);
    }

    @Override
    protected MetaBuilder cloneNode(Meta node) {
        return new MetaBuilder(node);
    }

    /**
     * Attach node without cloning it first to this one and change its parent.
     * This is much faster then set or put operation but all external
     * changes to the attached node will reflect on this one.
     *
     * @param node
     */
    @Override
    public void attachNode(MetaBuilder node) {
        super.attachNode(node);
    }

    /**
     * Attach a list of nodes, changing each node's parent to this node
     *
     * @param name
     * @param nodes
     */
    @Override
    public void attachNodeItem(String name, List<MetaBuilder> nodes) {
        super.attachNodeItem(name, nodes);
    }

    /**
     * Recursively apply node and value transformation to node. If node
     * transformation creates new node, then new node is returned.
     * <p>
     * The order of transformation is the following:
     * </p>
     * <ul>
     * <li> Parent node transformation</li>
     * <li> Parent node values transformation (using only values after node transformation is applied)</li>
     * <li> Children nodes transformation (using only nodes after parent node transformation is applied)</li>
     * </ul>
     *
     * @param nodeTransform
     * @return
     */
    public MetaBuilder transform(final UnaryOperator<MetaBuilder> nodeTransform, final BiFunction<String, Value, Value> valueTransform) {
        MetaBuilder res = nodeTransform.apply(this);
        res.values.replaceAll(valueTransform);
        res.nodes.values().forEach((item) -> {
            item.replaceAll((MetaBuilder t) -> t.transform(nodeTransform, valueTransform));
        });
        return res;
    }

    /**
     * Make a transformation substituting values in place using substitution pattern and given valueProviders
     *
     * @return
     */
    public MetaBuilder substituteValues(ValueProvider... providers) {
        return transform(UnaryOperator.identity(), (String key, Value val) -> MetaUtils.transformValue(val, providers));
    }

}
