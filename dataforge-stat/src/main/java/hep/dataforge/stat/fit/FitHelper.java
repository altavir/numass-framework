package hep.dataforge.stat.fit;

import hep.dataforge.context.Context;
import hep.dataforge.context.Global;
import hep.dataforge.io.FittingIOUtils;
import hep.dataforge.io.history.Chronicle;
import hep.dataforge.io.history.History;
import hep.dataforge.io.output.OutputKt;
import hep.dataforge.meta.Laminate;
import hep.dataforge.meta.Meta;
import hep.dataforge.stat.models.Model;
import hep.dataforge.stat.models.XYModel;
import hep.dataforge.tables.NavigableValuesSource;
import hep.dataforge.utils.Misc;
import org.jetbrains.annotations.NotNull;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static hep.dataforge.io.output.Output.TEXT_TYPE;
import static hep.dataforge.stat.fit.FitStage.STAGE_KEY;

/**
 * A helper class to run simple fits without building context and generating meta
 * Created by darksnake on 14-Apr-17.
 */
public class FitHelper {
    public static final String MODEL_KEY = "model";

    private FitManager manager;

    public FitHelper(Context context) {
        this.manager = context.getPlugins().load(FitManager.class);
    }

    public FitHelper() {
        this(Global.INSTANCE);
    }

    public FitManager getManager() {
        return manager;
    }

    private List<FitStage> buildStageList(Meta meta) {
        if (meta.hasMeta(STAGE_KEY)) {
            return meta.getMetaList(STAGE_KEY).stream().map(FitStage::new).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }


    /**
     * Meta as described in {@link FitAction}
     *
     * @param data
     * @param meta
     * @return
     */
    public FitBuilder fit(NavigableValuesSource data, Meta meta) {
        return new FitBuilder(data).update(meta);
    }

    public FitBuilder fit(NavigableValuesSource data) {
        return new FitBuilder(data);
    }

    @SuppressWarnings("deprecation")
    private static BiConsumer<FitStage, FitResult> buildDefaultListener(OutputStream stream) {
        return (stage, result) -> {
            //TODO add stage additional meta evaluation
            PrintWriter writer = new PrintWriter(stream);
            writer.printf("%n*** Result of fit stage" + stage.getType() + " ***%n");

            switch (stage.getType()) {
                case FitStage.TASK_COVARIANCE:
                    writer.printf("%n**COVARIANCE**%n");
                    result.printCovariance(writer);
                default:
                    result.printState(writer);
                    result.optState().ifPresent(state -> {
                        writer.printf("%n**RESIDUALS**%n");
                        if (state.getModel() instanceof XYModel) {
                            FittingIOUtils.printSpectrumResiduals(writer, (XYModel) state.getModel(), state.getData(), state.getParameters());
                        } else {
                            FittingIOUtils.printResiduals(writer, state);
                        }
                    });
            }
            writer.flush();
        };
    }

    public class FitBuilder {
        NavigableValuesSource data;
        Model model;
        ParamSet startPars = new ParamSet();
        History log = new Chronicle("fit", null);
        List<FitStage> stages = new ArrayList<>();
        BiConsumer<FitStage, FitResult> listener = buildDefaultListener(OutputKt.getStream(Global.INSTANCE.getConsole()));

        public FitBuilder(@NotNull NavigableValuesSource data) {
            this.data = data;
        }

        public FitBuilder update(Meta meta) {
            if (meta.hasMeta(MODEL_KEY)) {
                model(meta.getMeta(MODEL_KEY));
            } else if (meta.hasValue(MODEL_KEY)) {
                model(meta.getString(MODEL_KEY));
            }
            List<FitStage> stages = buildStageList(meta);
            if (!stages.isEmpty()) {
                allStages(stages);
            }
            params(meta);
            return this;
        }

        public FitBuilder report(History report) {
            this.log = report;
            return this;
        }

        /**
         * use context log with given name for this helper
         *
         * @param reportName
         * @return
         */
        public FitBuilder report(String reportName) {
            this.log = getManager().getContext().getHistory().getChronicle(reportName);
            return this;
        }

        /**
         * Set listener for fit stage result
         *
         * @param consumer
         * @return
         */
        public FitBuilder setListener(@NotNull BiConsumer<FitStage, FitResult> consumer) {
            this.listener = consumer;
            return this;
        }

        /**
         * Create default listener an redirect its output to given stream
         *
         * @param stream
         * @return
         */
        public FitBuilder setListenerStream(OutputStream stream) {
            this.listener = buildDefaultListener(stream);
            return this;
        }

        /**
         * Create default listener and redirect its output to Context output with default stage ang given name
         *
         * @param outputName
         * @return
         */
        public FitBuilder setListenerStream(String outputName) {
            this.listener = buildDefaultListener(OutputKt.getStream(getManager().getContext().getOutput().get("", outputName, TEXT_TYPE)));
            return this;
        }

        public FitBuilder model(Model model) {
            this.model = model;
            return this;
        }

        public FitBuilder model(String name) {
            this.model = manager.buildModel(name);
            return this;
        }

        public FitBuilder model(Meta meta) {
            this.model = manager.buildModel(meta);
            return this;
        }

//        public FitBuilder function(ParametricFunction func, XYAdapter adapter) {
//            this.model = new XYModel(func, adapter);
//            return this;
//        }

        public FitBuilder params(Meta meta) {
            if (meta instanceof Laminate) {
                ParamSet set = new ParamSet();
                Laminate laminate = (Laminate) meta;
                laminate.layersInverse().forEach(layer -> {
                    layer.optMeta("params").ifPresent(params -> {
                        set.updateFrom(new ParamSet(layer.getMeta("params")));
                    });
                });
                return params(set);
            } else {
                return params(new ParamSet(meta));
            }
        }

        public FitBuilder params(ParamSet params) {
            this.startPars.updateFrom(params);
            return this;
        }

        public FitBuilder param(String name, double value, double error) {
            this.startPars.setPar(name, value, error);
            return this;
        }


        public FitBuilder stage(FitStage stage) {
            stages.add(stage);
            return this;
        }

        /**
         * Set all fit stages clearing old ones
         *
         * @param stages
         * @return
         */
        public FitBuilder allStages(List<FitStage> stages) {
            this.stages.clear();
            this.stages.addAll(stages);
            return this;
        }

        public FitBuilder stage(String engineName, String taskName, String... freeParameters) {
            if (freeParameters.length == 0) {
                stages.add(new FitStage(engineName, taskName));
            } else {
                stages.add(new FitStage(engineName, taskName, freeParameters));
            }
            return this;
        }

        public FitResult run() {
            if (data == null) {
                throw new RuntimeException("Data not set");
            }

            if (model == null) {
                throw new RuntimeException("Model not set");
            }

            FitState state = new FitState(data, model, startPars);
            FitResult result = FitResult.build(state);
            if (stages.isEmpty()) {
                Misc.checkThread();
                FitStage defaultStage = new FitStage(QOWFitter.QOW_ENGINE_NAME, FitStage.TASK_RUN);
                result = manager.runStage(state, defaultStage, log);
                listener.accept(defaultStage, result);
            } else {
                for (FitStage stage : stages) {
                    Misc.checkThread();
                    result = manager.runStage(result.optState().get(), stage, log);
                    listener.accept(stage, result);
                }
            }

            return result;
        }
    }
}
