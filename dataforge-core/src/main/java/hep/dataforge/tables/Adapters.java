package hep.dataforge.tables;

import hep.dataforge.exceptions.NameNotFoundException;
import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaBuilder;
import hep.dataforge.names.Name;
import hep.dataforge.values.Value;
import hep.dataforge.values.ValueMap;
import hep.dataforge.values.Values;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Utility methods to work with adapters
 */
public class Adapters {
    /**
     * Build a basic adapter or a custom adapter depending on @class meta value
     *
     * @param meta
     * @return
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static ValuesAdapter buildAdapter(Meta meta) {
        if (meta.hasValue("@class")) {
            try {
                Class<? extends ValuesAdapter> type = (Class<ValuesAdapter>) Class.forName(meta.getString("@class"));
                return type.getConstructor(Meta.class).newInstance(meta);
            } catch (Exception e) {
                throw new RuntimeException("Can't create an instance of custom Adapter class", e);
            }
        } else {
            return new BasicAdapter(meta);
        }
    }

    public static final String X_AXIS = "x";
    public static final String Y_AXIS = "y";
    public static final String Z_AXIS = "z";


    public static final String VALUE_KEY = "value";
    public static final String ERROR_KEY = "err";
    public static final String LO_KEY = "lo";
    public static final String UP_KEY = "up";
    public static final String TITILE_KEY = "@title";


    public static final String X_VALUE_KEY = X_AXIS;//Name.joinString(X_AXIS, VALUE_KEY);
    public static final String X_ERROR_KEY = Name.Companion.joinString(X_AXIS, ERROR_KEY);
    public static final String Y_VALUE_KEY = Y_AXIS;//Name.joinString(Y_AXIS, VALUE_KEY);
    public static final String Y_ERROR_KEY = Y_AXIS + "." + ERROR_KEY;

    public static Value getValue(ValuesAdapter adapter, String axis, Values point) {
        return adapter.getComponent(point, axis);
    }

    public static Optional<Value> optError(ValuesAdapter adapter, String axis, Values point) {
        return adapter.optComponent(point, Name.Companion.joinString(axis, ERROR_KEY));
    }

    public static Value getError(ValuesAdapter adapter, String axis, Values point) {
        return optError(adapter, axis, point).orElseThrow(() -> new NameNotFoundException("Value not found in the value set"));
    }

    public static Double getUpperBound(ValuesAdapter adapter, String axis, Values point) {
        return adapter.optComponent(point, Name.Companion.joinString(axis, UP_KEY)).map(Value::getDouble)
                .orElseGet(() ->
                        getValue(adapter, axis, point).getDouble() +
                                optError(adapter, axis, point).map(Value::getDouble).orElse(0d)
                );

    }

    public static Double getLowerBound(ValuesAdapter adapter, String axis, Values point) {
        return adapter.optComponent(point, Name.Companion.joinString(axis, LO_KEY)).map(Value::getDouble)
                .orElseGet(() ->
                        getValue(adapter, axis, point).getDouble() -
                                optError(adapter, axis, point).map(Value::getDouble).orElse(0d)
                );
    }

    /**
     * Get a title for the axis from the adapter
     *
     * @param adapter
     * @param axis
     * @return
     */
    public static String getTitle(ValuesAdapter adapter, String axis) {
        return adapter.getMeta().getString(Name.Companion.joinString(axis, TITILE_KEY), axis);
    }

    public static Value getXValue(ValuesAdapter adapter, Values point) {
        return adapter.getComponent(point, X_VALUE_KEY);
    }

    public static Optional<Double> optXError(ValuesAdapter adapter, Values point) {
        return adapter.optComponent(point, X_ERROR_KEY).map(Value::getDouble);
    }

    public static Value getYValue(ValuesAdapter adapter, Values point) {
        return adapter.getComponent(point, Y_VALUE_KEY);
    }

    public static Optional<Double> optYError(ValuesAdapter adapter, Values point) {
        return adapter.optComponent(point, Y_ERROR_KEY).map(Value::getDouble);
    }

    public static Values buildXYDataPoint(ValuesAdapter adapter, double x, double y, double yErr) {
        return ValueMap.Companion.of(new String[]{
                adapter.getComponentName(X_VALUE_KEY),
                adapter.getComponentName(Y_VALUE_KEY),
                adapter.getComponentName(Y_ERROR_KEY)
        }, x, y, yErr);
    }

    public static Values buildXYDataPoint(ValuesAdapter adapter, double x, double y) {
        return ValueMap.Companion.of(new String[]{
                adapter.getComponentName(X_VALUE_KEY),
                adapter.getComponentName(Y_VALUE_KEY)
        }, x, y);
    }

    public static Values buildXYDataPoint(double x, double y, double yErr) {
        return buildXYDataPoint(DEFAULT_XYERR_ADAPTER, x, y, yErr);
    }

    public static Values buildXYDataPoint(double x, double y) {
        return buildXYDataPoint(DEFAULT_XY_ADAPTER, x, y);
    }

    @NotNull
    public static ValuesAdapter buildXYAdapter(String xName, String yName) {
        return new BasicAdapter(new MetaBuilder().setValue(X_VALUE_KEY, xName).setValue(Y_VALUE_KEY, yName));
    }

    @NotNull
    public static ValuesAdapter buildXYAdapter(String xName, String yName, String yErrName) {
        return new BasicAdapter(new MetaBuilder()
                .setValue(X_VALUE_KEY, xName)
                .setValue(Y_VALUE_KEY, yName)
                .setValue(Y_ERROR_KEY, yErrName)
        );
    }

    @NotNull
    public static ValuesAdapter buildXYAdapter(String xName, String xErrName, String yName, String yErrName) {
        return new BasicAdapter(new MetaBuilder()
                .setValue(X_VALUE_KEY, xName)
                .setValue(X_ERROR_KEY, xErrName)
                .setValue(Y_VALUE_KEY, yName)
                .setValue(Y_ERROR_KEY, yErrName)
        );
    }


    public static ValuesAdapter DEFAULT_XY_ADAPTER = buildXYAdapter(X_VALUE_KEY, Y_VALUE_KEY);

    public static ValuesAdapter DEFAULT_XYERR_ADAPTER = buildXYAdapter(X_VALUE_KEY, Y_VALUE_KEY, Y_ERROR_KEY);

    /**
     * Return a default TableFormat corresponding to adapter. Fills all of components explicitly presented in adapter as well as given components.
     *
     * @return
     */
    public static TableFormat getFormat(ValuesAdapter adapter, String... components) {
        TableFormatBuilder builder = new TableFormatBuilder();

        Stream.concat(adapter.listComponents(), Stream.of(components)).distinct().forEach(component ->
                builder.addNumber(adapter.getComponentName(component), component)
        );

        return builder.build();
    }

}
