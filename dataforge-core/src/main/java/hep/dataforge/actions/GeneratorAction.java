/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.actions;

import hep.dataforge.context.Context;
import hep.dataforge.data.DataNode;
import hep.dataforge.goals.GeneratorGoal;
import hep.dataforge.goals.Goal;
import hep.dataforge.io.history.Chronicle;
import hep.dataforge.meta.Meta;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

/**
 * An action that does not take any input data, only generates output. Each
 * output token is generated separately.
 *
 * @author Alexander Nozik
 */
public abstract class GeneratorAction<R> extends GenericAction<Void, R> {

    public GeneratorAction(@NotNull String name,@NotNull Class<R> outputType) {
        super(name, Void.class, outputType);
    }

    @Override
    public DataNode<R> run(Context context, DataNode<? extends Void> data, Meta actionMeta) {
        Chronicle log = context.getHistory().getChronicle(getName());

        Stream<ActionResult<R>> results = nameStream().map(name -> {
            Goal<R> goal = new GeneratorGoal<>(getExecutorService(context, actionMeta), () -> generateData(name));
            return new ActionResult<>(name, getOutputType(), goal, generateMeta(name), log);
        });

        return wrap(resultNodeName(), actionMeta, results);
    }

    protected abstract Stream<String> nameStream();

    protected abstract Meta generateMeta(String name);

    protected abstract R generateData(String name);

    protected String resultNodeName() {
        return "";
    }

}
