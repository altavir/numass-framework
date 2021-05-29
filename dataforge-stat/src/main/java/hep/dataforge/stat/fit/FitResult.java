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
package hep.dataforge.stat.fit;

import hep.dataforge.context.Context;
import hep.dataforge.io.FittingIOUtils;
import hep.dataforge.maths.NamedMatrix;
import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaBuilder;
import hep.dataforge.meta.MetaMorph;
import hep.dataforge.meta.SimpleMetaMorph;
import hep.dataforge.stat.models.Model;
import hep.dataforge.stat.models.ModelFactory;
import hep.dataforge.tables.ListOfPoints;
import hep.dataforge.tables.NavigableValuesSource;
import hep.dataforge.utils.Optionals;
import org.jetbrains.annotations.NotNull;

import java.io.PrintWriter;
import java.util.Optional;

import static hep.dataforge.io.FittingIOUtils.printParamSet;

/**
 * A metamorph representation of fit result
 *
 * @author Alexander Nozik
 */

public class FitResult extends SimpleMetaMorph {

    private transient FitState state = null;

    public static FitResult build(FitState state, boolean valid, String... freeParameters) {

        MetaBuilder builder = new MetaBuilder("fitResult")
                .setNode("data", new ListOfPoints(state.getData()).toMeta())//setting data
                .setValue("dataSize", state.getDataSize())
                .setNode("params", state.getParameters().toMeta())
                .setValue("isValid", valid);

        if (valid) {
            builder.setValue("chi2", state.getChi2());
        }

        //TODO add residuals to data
        if (freeParameters.length == 0) {
            builder.setValue("freePars", state.getParameters().namesAsArray());
        } else {
            builder.setValue("freePars", freeParameters);
        }

        if (state.hasCovariance()) {
            builder.setNode("covariance", state.getCovariance().toMeta());
        }

        //FIXME add interval estimate


        //setting model if possible
        builder.setNode("model", state.getModel().getMeta());

        FitResult res = new FitResult(builder.build());
        res.state = state;
        return res;
    }

    public static FitResult build(FitState state, String... freeParameters) {
        return build(state, false, freeParameters);
    }

    public FitResult(@NotNull Meta meta) {
        super(meta);
    }

    public ParamSet getParameters() {
        return optState().map(FitState::getParameters)
                .orElseGet(() -> MetaMorph.Companion.morph(ParamSet.class, getMeta().getMeta("params")));
    }

    public Optional<NamedMatrix> optCovariance() {
        return Optionals.either(optState().map(FitState::getCovariance))
                .or(() -> {
                    if (getMeta().hasMeta("covariance")) {
                        return Optional.of(MetaMorph.Companion.morph(NamedMatrix.class, getMeta().getMeta("covariance")));
                    } else {
                        return Optional.empty();
                    }
                }).opt();
    }


    public String[] getFreePars() {
        return getMeta().getStringArray("freePars");
    }

    public boolean isValid() {
        return getMeta().getBoolean("isValid", true);
    }


    public int ndf() {
        return this.getDataSize() - this.getFreePars().length;
    }

    private int getDataSize() {
        return getMeta().getInt("dataSize");
    }

    public double normedChi2() {
        return getChi2() / ndf();
    }

    public double getChi2() {
        return getMeta().getDouble("chi2", Double.NaN);
    }

    public Optional<FitState> optState() {
        return Optional.ofNullable(state);
    }

    private static Optional<Model> restoreModel(Context context, Meta meta) {
        return context.provideAll(ModelFactory.MODEL_TARGET, ModelFactory.class)
                .filter(it -> it.getName().equals(meta.getString("name")))
                .findFirst().map(it ->it.build(context,meta));
    }

    /**
     * Provide a model if possible
     *
     * @param context
     * @return
     */
    public Optional<Model> optModel(Context context) {
        return Optionals.either(optState().map(FitState::getModel))
                .or(() -> getMeta().optMeta("model").flatMap(meta -> restoreModel(context, getMeta())))
                .opt();
    }

    public NavigableValuesSource getData() {
        return optState().map(FitState::getData)
                .orElseGet(() -> MetaMorph.Companion.morph(ListOfPoints.class, getMeta().getMeta("data")));
    }

    /**
     * TODO replace by Markup
     *
     * @param out
     */
    @Deprecated
    public void printState(PrintWriter out) {
        //out.println("***FITTING RESULT***");
        this.printAllValues(out);
        this.printFitParsValues(out);

        optState().ifPresent(state -> {
            if (state.hasCovariance()) {
                out.println();
                out.println("Correlation matrix:");
                FittingIOUtils.printNamedMatrix(out, state.getCorrelationMatrix());
            }

            state.getIntervalEstimate().ifPresent(
                    intervalEstimate -> intervalEstimate.print(out)
            );

        });

        out.println();
        double chi2 = getChi2();
        out.printf("Chi squared over degrees of freedom: %g/%d = %g", chi2, this.ndf(), chi2 / this.ndf());
        out.println();
        out.flush();
    }

    @Deprecated
    private void printAllValues(PrintWriter out) {
        out.println();
        out.println("All function parameters are: ");
        printParamSet(out, getParameters());
    }

    @Deprecated
    public void printCovariance(PrintWriter out) {
        optState().ifPresent(state -> {
            if (state.getCovariance() != null) {
                out.println();
                out.printf("%n***COVARIANCE***%n");

                FittingIOUtils.printNamedMatrix(out, state.getCovariance());

            }

        });
    }

    @Deprecated
    private void printFitParsValues(PrintWriter out) {
        out.println();
        out.println("The best fit values are: ");
        printParamSet(out, getParameters().getSubSet(getFreePars()));
    }
}
