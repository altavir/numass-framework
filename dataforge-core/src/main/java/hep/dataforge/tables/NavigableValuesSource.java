package hep.dataforge.tables;

import hep.dataforge.exceptions.NameNotFoundException;
import hep.dataforge.values.Value;
import hep.dataforge.values.Values;

/**
 * Created by darksnake on 14-Apr-17.
 */
public interface NavigableValuesSource extends ValuesSource {
    Values getRow(int i);

    /**
     *
     * Get a specific value
     * @param name
     * @param index
     * @return
     * @throws NameNotFoundException
     */
    default Value get(String name, int index) throws NameNotFoundException {
        return getRow(index).getValue(name);
    }

    default double getDouble(String name, int index){
        return get(name, index).getDouble();
    }

    /**
     * Number of rows in the table
     *
     * @return
     */
    int size();
}
