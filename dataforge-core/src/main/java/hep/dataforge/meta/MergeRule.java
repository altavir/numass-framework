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
import hep.dataforge.values.ValueFactory;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Правило объединения двух аннотаций
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
public abstract class MergeRule implements Collector<Meta, MetaBuilder, Meta> {

    /**
     * Правило объединения по-умолчанию. Подразумевается простая замена всех
     * совподающих элементов.
     *
     * @return
     */
    public static MergeRule replace() {
        return new ReplaceRule();
    }

    public static MergeRule join() {
        return new JoinRule();
    }

    /**
     * Возвращает правило объединения в котором элементы, входящие в список
     * объединяются, а остальные заменяются
     *
     * @param toJoin
     * @return
     */
    public static MergeRule custom(String... toJoin) {
        return new ConfigurableMergeRule(toJoin);
    }

    /**
     * Выполняет объединение с заменой всех совподающих элементов
     *
     * @param main
     * @param second
     * @return
     */
    public static MetaBuilder replace(Meta main, Meta second) {
        return replace().merge(main, second);
    }

    /**
     * Выполняет объединение с объединением всех списков
     *
     * @param main   a {@link hep.dataforge.meta.Meta} object.
     * @param second a {@link hep.dataforge.meta.Meta} object.
     * @return a {@link hep.dataforge.meta.Meta} object.
     */
    public static Meta join(Meta main, Meta second) {
        return new JoinRule().merge(main, second);
    }

    /**
     * Метод, объединяющий две аннотации. Порядок имеет значение. Первая
     * аннотация является основной, вторая запасной
     *
     * @param main
     * @param second
     * @return
     */
    public MetaBuilder merge(Meta main, Meta second) {
        return mergeInPlace(main, new MetaBuilder(second));
    }

    /**
     * Apply changes from main Meta to meta builder in place
     *
     * @param main
     * @param builder
     * @return
     */
    public MetaBuilder mergeInPlace(Meta main, final MetaBuilder builder) {
        //MetaBuilder builder = new MetaBuilder(mergeName(main.getName(), second.getName()));
        builder.rename(mergeName(main.getName(), builder.getName()));

        // Overriding values
        Stream.concat(main.getValueNames(), builder.getValueNames()).collect(Collectors.toSet())
                .forEach(valueName -> {
                    writeValue(
                            builder,
                            valueName,
                            mergeValues(
                                    Name.Companion.join(builder.getFullName(), Name.Companion.of(valueName)),
                                    main.optValue(valueName).orElse(ValueFactory.NULL),
                                    builder.optValue(valueName).orElse(ValueFactory.NULL)
                            )
                    );
                });

        // Overriding nodes
        Stream.concat(main.getNodeNames(), builder.getNodeNames()).collect(Collectors.toSet())
                .forEach(nodeName -> {
                    List<? extends Meta> mainNodes = main.getMetaList(nodeName);
                    List<? extends Meta> secondNodes = builder.getMetaList(nodeName);
                    if (mainNodes.size() == 1 && secondNodes.size() == 1) {
                        writeNode(builder, nodeName, Collections.singletonList(merge(mainNodes.get(0), secondNodes.get(0))));
                    } else {
                        List<? extends Meta> item = mergeNodes(Name.Companion.join(builder.getFullName(),
                                Name.Companion.of(nodeName)), mainNodes, secondNodes);
                        writeNode(builder, nodeName, item);
                    }
                });

        return builder;
    }

    protected abstract String mergeName(String mainName, String secondName);

    /**
     * @param valueName full name of the value relative to root
     * @param first
     * @param second
     * @return
     */
    protected abstract Value mergeValues(Name valueName, Value first, Value second);

    /**
     * @param nodeName       full name of the node relative to root
     * @param mainNodes
     * @param secondaryNodes
     * @return
     */
    protected abstract List<? extends Meta> mergeNodes(Name nodeName, List<? extends Meta> mainNodes, List<? extends Meta> secondaryNodes);

    protected void writeValue(MetaBuilder builder, String name, Value item) {
        builder.setValue(name, item);
    }

    protected void writeNode(MetaBuilder builder, String name, List<? extends Meta> item) {
        builder.setNode(name, item);
    }

    @Override
    public Supplier<MetaBuilder> supplier() {
        return () -> new MetaBuilder("");
    }

    //TODO test it via Laminate collector
    @Override
    public BiConsumer<MetaBuilder, Meta> accumulator() {
        return ((builder, meta) -> mergeInPlace(meta, builder));
    }

    @Override
    public BinaryOperator<MetaBuilder> combiner() {
        return this::merge;
    }

    @Override
    public Function<MetaBuilder, Meta> finisher() {
        return MetaBuilder::build;
    }

    @Override
    public Set<Characteristics> characteristics() {
        return Collections.emptySet();
    }
}
