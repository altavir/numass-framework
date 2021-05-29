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

import hep.dataforge.NamedKt;
import hep.dataforge.exceptions.AnonymousNotAlowedException;
import hep.dataforge.exceptions.NamingException;
import hep.dataforge.names.Name;
import hep.dataforge.values.Value;
import hep.dataforge.values.ValueFactory;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.*;

/**
 * A mutable annotation node equipped with observers.
 *
 * @author Alexander Nozik
 */
@MorphTarget(target = SealedNode.class)
public abstract class MutableMetaNode<T extends MutableMetaNode> extends MetaNode<T>
        implements Serializable {

    protected T parent;

    protected MutableMetaNode() {
        super();
    }

    protected MutableMetaNode(String name) {
        super(name);
        this.parent = null;
    }

    protected MutableMetaNode(Meta meta) {
        this.name = meta.getName();
        meta.getValueNames().forEach(valName -> setValue(valName, meta.getValue(valName), false));
        meta.getNodeNames().forEach(nodeName -> setNode(nodeName, meta.getMetaList(nodeName), false));
    }

    /**
     * Parent aware name of this node including query string
     *
     * @return
     */
    public Name getQualifiedName() {
        if (parent == null) {
            return Name.Companion.ofSingle(getName());
        } else {
            int index = parent.indexOf(this);
            if (index >= 0) {
                return Name.Companion.ofSingle(String.format("%s[%d]", getName(), index));
            } else {
                return Name.Companion.ofSingle(getName());
            }
        }
    }

    /**
     * Full name including all ncestors
     *
     * @return
     */
    public Name getFullName() {
        if (parent == null) {
            return Name.Companion.of(getName());
        } else {
            return Name.Companion.join(parent.getFullName(), getQualifiedName());
        }
    }

    /**
     * Notify all observers that element is changed
     *
     * @param name
     * @param oldItem
     * @param newItem
     */
    protected void notifyNodeChanged(Name name, List<? extends Meta> oldItem, List<? extends Meta> newItem) {
        if (parent != null) {
            parent.notifyNodeChanged(getQualifiedName().plus(name), oldItem, newItem);
        }
    }

    /**
     * Notify all observers that value is changed
     *
     * @param name
     * @param oldItem
     * @param newItem
     */
    protected void notifyValueChanged(Name name, Value oldItem, Value newItem) {
        if (parent != null) {
            parent.notifyValueChanged(getQualifiedName().plus(name), oldItem, newItem);
        }
    }

    @Nullable
    protected T getParent() {
        return parent;
    }

    /**
     * Add a copy of given meta to the node list with given name. Create a new
     * one if it does not exist
     *
     * @param node
     * @param notify notify listeners
     */
    public T putNode(String name, Meta node, boolean notify) {
        if (!isValidElementName(name)) {
            throw new NamingException(String.format("\"%s\" is not a valid element name in the annotation", name));
        }

        //do not put empty nodes
        if (node.isEmpty()) {
            return self();
        }

        T newNode = transformNode(name, node);
        List<T> list = super.nodes.get(name);
        List<T> oldList = list != null ? new ArrayList<>(list) : Collections.emptyList();
        if (list == null) {
            List<T> newList = new ArrayList<>();
            newList.add(newNode);
            this.setNodeItem(name, newList);
            list = newList;
        } else {
            //Adding items to existing list. No need to update parents and listeners
            list.add(newNode);
        }
        if (notify) {
            notifyNodeChanged(Name.Companion.of(node.getName()), oldList, new ArrayList<>(list));
        }
        return self();
    }

    /**
     * putNode(element,true)
     *
     * @param element
     * @return
     */
    public T putNode(Meta element) {
        if (element.isEmpty()) {
            return self();
        }
        if (NamedKt.isAnonymous(element) && !element.hasValue("@name")) {
            throw new AnonymousNotAlowedException();
        }
        return putNode(element.getName(), element, true);
    }

    /**
     * Same as {@code putNode(Meta)}, but also renames new node
     *
     * @param name
     * @param element
     * @return
     */
    public T putNode(String name, Meta element) {
        return putNode(name, element, true);
    }

    /**
     * Add new value to the value item with the given name. Create new one if it
     * does not exist. null arguments are ignored (Value.NULL is still could be
     * used)
     *
     * @param name
     * @param value
     * @param notify notify listeners
     */
    public T putValue(Name name, Value value, boolean notify) {
        if (value != null) {
            Optional<Value> oldValue = optValue(name);
            if (oldValue.isPresent()) {
                List<Value> list = new ArrayList<>(oldValue.get().getList());
                list.add(value);

                Value newValue = Value.Companion.of(list);

                setValueItem(name, newValue);

                if (notify) {
                    notifyValueChanged(name, oldValue.get(), newValue);
                }
            } else {
                setValueItem(name, value);
            }
        }
        return self();
    }

    public T putValue(String name, Value value, boolean notify) {
        if (!isValidElementName(name)) {
            throw new NamingException(String.format("'%s' is not a valid element name in the meta", name));
        }
        return putValue(Name.Companion.of(name), value, notify);
    }

    /**
     * setValue(name, value, true)
     *
     * @param name
     * @param value
     * @return
     */
    public T putValue(String name, Value value) {
        return putValue(name, value, true);
    }

    /**
     * <p>
     * setNode.</p>
     *
     * @param element
     */
    public T setNode(Meta element) {
        if (NamedKt.isAnonymous(element)) {
            throw new AnonymousNotAlowedException();
        }

        String nodeName = element.getName();
        if (!isValidElementName(nodeName)) {
            throw new NamingException(String.format("\"%s\" is not a valid element name in the meta", nodeName));
        }
        this.setNode(nodeName, element);
        return self();
    }

    /**
     * Set or replace current node or node list with this name
     *
     * @param name
     * @param elements
     * @param notify
     * @return
     */
    public T setNode(String name, List<? extends Meta> elements, boolean notify) {
        if (elements != null && !elements.isEmpty()) {
            List<T> oldNodeItem;
            if (hasMeta(name)) {
                oldNodeItem = new ArrayList<>(getMetaList(name));
            } else {
                oldNodeItem = Collections.emptyList();
            }
            setNodeItem(name, elements);
            if (notify) {
                notifyNodeChanged(Name.Companion.of(name), oldNodeItem, getMetaList(name));
            }
        } else {
            removeNode(name);
        }
        return self();
    }

    /**
     * setNode(name,elements,true)
     *
     * @param name
     * @param elements
     * @return
     */
    public T setNode(String name, List<? extends Meta> elements) {
        return setNode(name, elements, true);
    }

    /**
     * Добавляет новый элемент, стирая старый
     *
     * @param name
     * @param elements
     */
    public T setNode(String name, Meta... elements) {
        return setNode(name, Arrays.asList(elements));
    }

    /**
     * Replace a value item with given name. If value is null, remove corresponding value from node.
     *
     * @param name
     * @param value
     */
    public T setValue(Name name, Value value, boolean notify) {
        if (value == null ) {
            removeValue(name);
        } else {
            Optional<Value> oldValueItem = optValue(name);

            setValueItem(name, value);
            if (notify) {
                notifyValueChanged(name, oldValueItem.orElse(null), value);
            }
        }
        return self();
    }

    public T setValue(String name, Value value, boolean notify) {
        return setValue(Name.Companion.of(name), value, notify);
    }

    /**
     * setValue(name, value, true)
     *
     * @param name
     * @param value
     * @return
     */
    public T setValue(String name, Value value) {
        return setValue(name, value, true);
    }


    public T setValue(String name, Object object) {
        if(object ==null){
            removeValue(name);
            return self();
        } else {
            return setValue(name, ValueFactory.of(object));
        }
    }

    /**
     * Adds new value to the list of values with given name. Ignores null value.
     * Does not replace old Value!
     *
     * @param name
     * @param value
     * @return
     */
    public T putValue(String name, Object value) {
        if (value != null) {
            putValue(name, Value.Companion.of(value));
        }
        return self();
    }

    public T putValues(String name, Object[] values) {
        if (values != null && values.length > 0) {
            for (Object obj : values) {
                putValue(name, obj);
            }
        }
        return self();
    }

    public T putValues(String name, String... values) {
        if (values != null && values.length > 0) {
            for (Object obj : values) {
                putValue(name, obj);
            }
        }
        return self();
    }

    /**
     * Rename this node
     *
     * @param name
     */
    protected T setName(String name) {
        if (parent != null) {
            throw new RuntimeException("Can't rename attached node");
        }
        this.name = name;
        return self();
    }

    /**
     * Remove node list at given path (including descending tree) and notify
     * listener
     *
     * @param path
     */
    public T removeNode(String path) {
        if (hasMeta(path)) {
            List<T> oldNode = getMetaList(path);

            if (nodes.containsKey(path)) {
                nodes.remove(path);
            } else {
                Name namePath = Name.Companion.of(path);
                if (namePath.getLength() > 1) {
                    //FIXME many path to string and string to path conversions
                    getHead(namePath).removeNode(namePath.cutFirst().toString());
                }
            }

            notifyNodeChanged(Name.Companion.of(path), oldNode, Collections.emptyList());
        }
        return self();
    }

    /**
     * Replace or remove given direct descendant child node if it is present and
     * notify listeners.
     *
     * @param child
     */
    public void replaceChildNode(T child, Meta node) {
        String nodeName = child.getName();
        if (hasMeta(nodeName)) {
            List<T> oldNode = getMetaList(nodeName);
            int index = nodes.get(nodeName).indexOf(child);
            if (node == null) {
                nodes.get(nodeName).remove(index);
            } else {
                nodes.get(nodeName).set(index, transformNode(child.getName(), node));
            }
            notifyNodeChanged(Name.Companion.ofSingle(nodeName), oldNode, getMetaList(nodeName));
        }
    }

    /**
     * Remove value with given path and notify listener
     *
     * @param path
     */
    public void removeValue(Name path) {
        Optional<Value> oldValue = optValue(path);
        if (oldValue.isPresent()) {
            if (path.getLength() > 1) {
                getHead(path).removeValue(path.cutFirst().toString());
            } else {
                this.values.remove(path.getUnescaped());
            }
            notifyValueChanged(path, oldValue.get(), null);
        }
    }

    public void removeValue(String path) {
        removeValue(Name.Companion.of(path));
    }

    /**
     * Replaces node with given path with given item or creates new one
     *
     * @param path
     * @param elements
     */
    protected void setNodeItem(String path, List<? extends Meta> elements) {
        if (!nodes.containsKey(path)) {
            Name namePath = Name.Companion.of(path);
            if (namePath.getLength() > 1) {
                String headName = namePath.getFirst().entry();
                T headNode;
                if (nodes.containsKey(headName)) {
                    headNode = getHead(namePath);
                } else {
                    headNode = createChildNode(headName);
                    attachNode(headNode);
                }

                headNode.setNodeItem(namePath.cutFirst().toString(), elements);
            } else {
                //single token path
                this.nodes.put(path, transformNodeItem(path, elements));
            }
        } else {
            // else reset contents of the node
            this.nodes.put(path, transformNodeItem(path, elements));
        }
    }

    protected void setValueItem(Name namePath, Value value) {
        if (namePath.getLength() > 1) {
            String headName = namePath.getFirst().entry();
            T headNode;
            if (nodes.containsKey(headName)) {
                headNode = getHead(namePath);
            } else {
                headNode = createChildNode(headName);
                attachNode(headNode);
            }
            headNode.setValueItem(namePath.cutFirst(), value);
        } else {
            //single token path
            this.values.put(namePath.getUnescaped(), value);
        }
    }

    protected void setValueItem(String path, Value value) {
        setValueItem(Name.Companion.of(path), value);
    }

    /**
     * Transform list of nodes changing their name and parent
     *
     * @param name
     * @param item
     * @return
     */
    private List<T> transformNodeItem(String name, List<? extends Meta> item) {
        List<T> res = new ArrayList<>();
        item.stream().map((an) -> transformNode(name, an)).peek((el) -> el.parent = this).forEach(res::add);
        return res;
    }

    private T transformNode(String name, Meta node) {
        T el = cloneNode(node);
        el.setName(name);
        el.parent = this;
        return el;
    }

    /**
     * Create but do not attach new child node
     *
     * @param name
     * @return
     */
    protected abstract T createChildNode(String name);

//    /**
//     * Create a deep copy of the node but do not set parent or name. Deep copy
//     * does not clone listeners
//     *
//     * @param node
//     * @return
//     */
//    protected abstract T cloneNode(Meta node);

    /**
     * Attach node item without transformation. Each node's parent is changed to
     * this
     *
     * @param name
     * @param nodes
     */
    public void attachNodeItem(String name, List<T> nodes) {
        nodes.forEach((T node) -> {
            node.parent = this;
            node.name = name;
        });
        List<T> oldList = this.nodes.get(name);
        this.nodes.put(name, nodes);
        notifyNodeChanged(Name.Companion.ofSingle(name), oldList, nodes);
    }

    /**
     * Add new node to the current list of nodes with the given name. Replace
     * its parent with this.
     *
     * @param node
     */
    public void attachNode(String nodeName, T node) {
        if (node == null) {
            throw new IllegalArgumentException("Can't attach null node");
        }
        node.parent = this;
        node.name = nodeName;
        List<T> list;
        if (nodes.containsKey(nodeName)) {
            list = nodes.get(nodeName);
        } else {
            list = new ArrayList<>();
            nodes.put(nodeName, list);
        }
        List<T> oldList = new ArrayList<>(list);
        list.add(node);
        notifyNodeChanged(Name.Companion.ofSingle(nodeName), oldList, list);
    }

    public void attachNode(T node) {
        attachNode(node.getName(), node);
    }
}
