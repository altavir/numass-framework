package hep.dataforge.actions;

import hep.dataforge.context.Context;
import hep.dataforge.data.Data;
import hep.dataforge.data.DataNode;
import hep.dataforge.data.DataNodeBuilder;
import hep.dataforge.data.DataTree;
import hep.dataforge.goals.Goal;
import hep.dataforge.goals.PipeGoal;
import hep.dataforge.meta.Laminate;
import hep.dataforge.meta.Meta;
import hep.dataforge.names.Name;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * A split action that creates multiple Data from each input element (some input elements could be ignored)
 * Created by darksnake on 28-Jan-17.
 */
public abstract class OneToManyAction<T, R> extends GenericAction<T, R> {

    public OneToManyAction(@NotNull String name, @NotNull Class<T> inputType, @NotNull Class<R> outputType) {
        super(name, inputType, outputType);
    }

    @Override
    public DataNode<R> run(Context context, DataNode<? extends T> data, Meta actionMeta) {
        checkInput(data);
        DataNodeBuilder<R> builder = DataTree.Companion.edit(getOutputType());
        data.forEach(datum -> {
            String inputName = datum.getName();
            Laminate inputMeta = new Laminate(datum.getMeta(), actionMeta);
            Map<String, Meta> metaMap = prepareMeta(context, inputName, inputMeta);
            metaMap.forEach((outputName, outputMeta) -> {
                Goal<R> goal = new PipeGoal<>(datum.getGoal(), input -> execute(context, inputName, outputName, input, inputMeta));
                Data<R> res = new Data<R>(getOutputType(), goal, outputMeta);
                builder.putData(placement(inputName, outputName), res, false);
            });
        });
        return builder.build();
    }

    /**
     * The placement rule for result Data. By default each input element is transformed into a node with
     *
     * @param inputName
     * @param outputName
     * @return
     */
    protected String placement(String inputName, String outputName) {
        return Name.Companion.joinString(inputName, outputName);
    }

    //TODO add node meta

    /**
     * @param context
     * @param inputName
     * @param meta
     * @return
     */
    protected abstract Map<String, Meta> prepareMeta(Context context, String inputName, Laminate meta);

    protected abstract R execute(Context context, String inputName, String outputName, T input, Laminate meta);

}
