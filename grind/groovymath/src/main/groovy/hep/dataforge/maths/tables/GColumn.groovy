package hep.dataforge.maths.tables

import java.time.Instant

/**
 * GColumn for dynamic groovy table. Column can grow, but cannot be diminished.
 *
 * Created by darksnake on 26-Oct-15.
 */
class GColumn implements Iterable {
    /**
     * A default name for unnamed column
     */
    public static final String DEFAULT_COLUMN_NAME = "";

    /**
     * The values of the column
     */
    List values;

    /**
     * The name of the column
     */
    String name;

    /**
     * The type of the column. Currently not used.
     */
    String type;

    /**
     * An empty anonymous column
     */
    GColumn() {
    }

    /**
     * A copy constructor
     * @param column
     */
    GColumn(GColumn column) {
        this.values = column.values.clone();
        this.name = column.name;
        this.type = column.type;
    }

    /**
     * A general constructor
     * @param name
     * @param type
     * @param values
     */
    GColumn(String name = DEFAULT_COLUMN_NAME, String type = ValueType.ANY, List values) {
        this.values = values.clone()
        this.name = name
        this.type = type
    }

    /**
     * Create new GColumn, each of its values is obtained by provided transformation. Transformation gets value and index as parameters
     * @param transformation
     * @return
     */
    GColumn transform(String newName = DEFAULT_COLUMN_NAME, String newType = type, Closure transformation) {
        return new GColumn(name: newName, type: newType, values: values.withIndex().collect(transformation));
    }

    /**
     * Sum of columns
     * @param other
     * @return
     */
    GColumn plus(GColumn other) {
        return transform { value, index -> value + other[index] }
    }

    /**
     * Add a value to each column element
     * @param obj
     * @return
     */
    GColumn plus(Object obj) {
        return transform { value, index -> value + obj }
    }

    /**
     * Difference of columns
     * @param other
     * @return
     */
    GColumn minus(GColumn other) {
        return transform { value, index -> value - other[index] }
    }

    /**
     * Subtract value from each column element
     * @param obj
     * @return
     */
    GColumn minus(Object obj) {
        return transform { value, index -> value - obj }
    }

    /**
     * Element by element multiplication of columns
     * @param other
     * @return
     */
    GColumn multiply(GColumn other) {
        return transform { value, index -> value * other[index] }
    }

    /**
     * Multiply all elements by given value
     * @param obj
     * @return
     */
    GColumn multiply(Object obj) {
        return transform { value, index -> value * obj }
    }

    /**
     * Negate column
     * @param obj
     * @return
     */
    GColumn negative() {
        return transform { value, index -> -value }
    }

    /**
     * Add a value
     * @param obj
     * @return
     */
    GColumn add(Object obj) {
        values += obj;
        return this
    }

//    /**
//     * remove value
//     * @param obj
//     * @return
//     */
//    GColumn remove(Object obj) {
//        values -= obj;
//        return this
//    }

    GColumn leftShift(Object obj) {
        add(obj);
    }

    def getAt(int index) {
        if (index >= values.size()) {
            return nullValue();
        } else {
            return values[index];
        }
    }

    def putAt(int index, Object obj) {
        this.values.putAt(index, obj);
    }

    /**
     * The number of values in the column
     * @return
     */
    int size() {
        return values.size();
    }

    @Override
    Iterator iterator() {
        return values.iterator();
    }

    def nullValue() {
        switch (type) {
            case ValueType.NUMBER:
                return Double.NaN;
            case ValueType.TIME:
                return Instant.MIN;
            default:
                return "";
        }
    }
}
