/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.meta;

import hep.dataforge.exceptions.NamingException;
import hep.dataforge.io.IOUtils;
import hep.dataforge.names.Name;
import hep.dataforge.values.*;
import kotlin.Pair;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Utilities to work with meta
 *
 * @author Alexander Nozik
 */
public class MetaUtils {

    /**
     * Find all nodes with given path that satisfy given condition. Return empty
     * list if no such nodes are found.
     *
     * @param root
     * @param path
     * @param condition
     * @return
     */
    public static List<Meta> findNodes(Meta root, String path, Predicate<Meta> condition) {
        if (!root.hasMeta(path)) {
            return Collections.emptyList();
        } else {
            return root.getMetaList(path).stream()
                    .filter(condition)
                    .collect(Collectors.<Meta>toList());
        }
    }

    /**
     * Return the first node with given path that satisfies condition. Null if
     * no such nodes are found.
     *
     * @param root
     * @param path
     * @param condition
     * @return
     */
    public static Optional<Meta> findNode(Meta root, String path, Predicate<Meta> condition) {
        List<? extends Meta> list = findNodes(root, path, condition);
        if (list.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(list.get(0));
        }
    }

    /**
     * Find node by given key-value pair
     *
     * @param root
     * @param path
     * @param key
     * @param value
     * @return
     */
    public static Optional<Meta> findNodeByValue(Meta root, String path, String key, Object value) {
        return findNode(root, path, (m) -> m.hasValue(key) && m.getValue(key).equals(ValueFactory.of(value)));
    }

    /**
     * The transformation which should be performed on each value before it is
     * returned to user. Basically is used to ensure automatic substitutions. If
     * the reference not found in the current annotation scope than the value is
     * returned as-is.
     * <p>
     * the notation for template is the following: {@code ${path|def}} where
     * {@code path} is the path for value in the context and {@code def} is the
     * default value.
     * </p>
     *
     * @param val
     * @param contexts a list of contexts to draw value from
     * @return
     */
    public static Value transformValue(Value val, ValueProvider... contexts) {
        if (contexts.length == 0) {
            return val;
        }
        if (val.getType().equals(ValueType.STRING) && val.getString().contains("$")) {
            String valStr = val.getString();
//            Matcher matcher = Pattern.compile("\\$\\{(?<sub>.*)\\}").matcher(valStr);
            Matcher matcher = Pattern.compile("\\$\\{(?<sub>[^|]*)(?:\\|(?<def>.*))?}").matcher(valStr);
            while (matcher.find()) {
                String group = matcher.group();
                String sub = matcher.group("sub");
                String replacement = matcher.group("def");
                for (ValueProvider context : contexts) {
                    if (context != null && context.hasValue(sub)) {
                        replacement = context.getString(sub);
                        break;
                    }
                }
                if (replacement != null) {
                    valStr = valStr.replace(group, replacement);
                }
            }
            return ValueFactory.parse(valStr, false);
        } else {
            return val;
        }
    }

    /**
     * Apply query for given list ob objects using extracted meta as reference
     *
     * @param objects
     * @param query
     * @param metaExtractor
     * @param <T>
     * @return
     */
    public static <T> List<T> query(List<T> objects, String query, Function<T, Meta> metaExtractor) {
        if (query.isEmpty()) {
            return objects;
        }
        try {
            int num = Integer.parseInt(query);
            if (num < 0 || num >= objects.size()) {
                throw new NamingException("No list element with given index");
            }
            return Collections.singletonList(objects.get(num));
        } catch (NumberFormatException ex) {
            List<Predicate<Meta>> predicates = new ArrayList<>();
            String[] tokens = query.split(",");
            for (String token : tokens) {
                predicates.add(buildQueryPredicate(token));
            }
            Predicate<Meta> predicate = meta -> {
                AtomicBoolean res = new AtomicBoolean(true);
                predicates.forEach(p -> {
                    if (!p.test(meta)) {
                        res.set(false);
                    }
                });
                return res.get();
            };
            return objects.stream().filter(obj -> predicate.test(metaExtractor.apply(obj))).collect(Collectors.toList());
        }


    }

    /**
     * Build a meta predicate for a given single token
     *
     * @param token
     * @return
     */
    private static Predicate<Meta> buildQueryPredicate(String token) {
        String[] split = token.split("=");
        if (split.length == 2) {
            String key = split[0].trim();
            String value = split[1].trim();
            //TODO implement compare operators
            return meta -> meta.getValue(key, ValueFactory.NULL).equals(ValueFactory.of(value));
        } else {
            throw new NamingException("'" + token + "' is not a valid query");
        }
    }

    /**
     * Apply query to node list
     *
     * @param <T>
     * @param nodeList
     * @param query
     * @return
     */
    public static <T extends MetaNode> List<T> query(List<T> nodeList, String query) {
        return query(nodeList, query, it -> it);
    }

    /**
     * A stream containing pairs
     *
     * @param prefix
     * @return
     */
    private static Stream<Pair<Name, Meta>> nodeStream(Name prefix, Meta node, boolean includeRoot) {
        Stream<Pair<Name, Meta>> subNodeStream = node.getNodeNames().flatMap(nodeName -> {
            List<? extends Meta> metaList = node.getMetaList(nodeName);
            Name nodePrefix;
            if (prefix == null || prefix.isEmpty()) {
                nodePrefix = Name.Companion.of(nodeName);
            } else {
                nodePrefix = prefix.plus(nodeName);
            }
            if (metaList.size() == 1) {
                return nodeStream(nodePrefix, metaList.get(0), true);
            } else {
                return IntStream.range(0, metaList.size()).boxed()
                        .flatMap(i -> {
                            String subPrefix = String.format("%s[%d]", nodePrefix, i);
                            Meta subNode = metaList.get(i);
                            return nodeStream(Name.Companion.ofSingle(subPrefix), subNode, true);
                        });
            }
        });
        if (includeRoot) {
            return Stream.concat(Stream.of(new Pair<>(prefix, node)), subNodeStream);
        } else {
            return subNodeStream;
        }
    }

    public static Stream<Pair<Name, Meta>> nodeStream(Meta node) {
        return nodeStream(Name.Companion.empty(), node, false);
    }

    public static Stream<Pair<Name, Value>> valueStream(Meta node) {
        return nodeStream(Name.Companion.empty(), node, true).flatMap((Pair<Name, Meta> entry) -> {
            Name key = entry.getFirst();
            Meta childMeta = entry.getSecond();
            return childMeta.getValueNames().map((String valueName) -> new Pair<>(key.plus(valueName), childMeta.getValue(valueName)));
        });
    }

    /**
     * Write Meta node to binary output stream.
     *
     * @param out
     * @param meta        node to serialize
     * @param includeName include node name in serialization
     */
    public static void writeMeta(ObjectOutput out, Meta meta, boolean includeName) {
        try {
            // write name if it is required
            if (includeName) {
                IOUtils.writeString(out, meta.getName());
            }
            out.writeShort((int) meta.getValueNames(true).count());
            //writing values in format [name length, name, value]
            meta.getValueNames(true).forEach(valName -> {
                try {
                    IOUtils.writeString(out, valName);
                    ValueUtilsKt.writeValue(out, meta.getValue(valName));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            out.writeShort((int) meta.getNodeNames(true).count());
            meta.getNodeNames(true).forEach(nodeName -> {
                try {
                    IOUtils.writeString(out, nodeName);
                    List<? extends Meta> metas = meta.getMetaList(nodeName);
                    out.writeShort(metas.size());
                    for (Meta m : metas) {
                        //ignoring names for children
                        writeMeta(out, m, false);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeMeta(ObjectOutput out, Meta meta) {
        writeMeta(out, meta, true);
    }

    /**
     * Read Meta node from serial stream as MetaBuilder
     *
     * @param in
     * @param name the name of the node. If null, then the name is being read from stream
     * @return
     * @throws IOException
     */
    public static MetaBuilder readMeta(ObjectInput in, String name) throws IOException {
        MetaBuilder res = new MetaBuilder(name);
        if (name == null) {
            res.setName(IOUtils.readString(in));
        }
        short valSize = in.readShort();
        for (int i = 0; i < valSize; i++) {
            String valName = IOUtils.readString(in);
            Value val = ValueUtilsKt.readValue(in);
            res.setValue(valName, val);
        }
        short nodeSize = in.readShort();
        for (int i = 0; i < nodeSize; i++) {
            String nodeName = IOUtils.readString(in);
            short listSize = in.readShort();
            List<MetaBuilder> nodeList = new ArrayList<>();
            for (int j = 0; j < listSize; j++) {
                nodeList.add(readMeta(in, nodeName));
            }
            res.setNodeItem(nodeName, nodeList);
        }

        return res;
    }

    public static MetaBuilder readMeta(ObjectInput in) throws IOException {
        return readMeta(in, null);
    }

    /**
     * Check each of given paths in the given node. Return first subnode that do actually exist
     *
     * @param meta
     * @param paths
     * @return
     */
    public static Optional<Meta> optEither(Meta meta, String... paths) {
        return Stream.of(paths).map(meta::optMeta).filter(Optional::isPresent).findFirst().map(Optional::get);
    }

    public static Optional<Value> optEitherValue(Meta meta, String... paths) {
        return Stream.of(paths).map(meta::optValue).filter(Optional::isPresent).findFirst().map(Optional::get);
    }

}
