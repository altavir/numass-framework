package hep.dataforge.maths.tables

import org.apache.commons.math3.exception.DimensionMismatchException

/**
 * An unmodifiable row of values
 * Created by darksnake on 26-Oct-15.
 */
class MapRow implements GRow {
    LinkedHashMap<String, Object> map = new LinkedHashMap<>();

    MapRow(Map<String, Object> map) {
        //TODO clone here
        this.map.putAll(map);
    }

    MapRow(List keys, List values) {
        if (keys.size() != values.size()) {
            throw new DimensionMismatchException(values.size(), keys.size());
        }

        for (int i = 0; i < keys.size(); i++) {
            map.put(keys[i], values[i]);
        }
    }


    Object getAt(String key) {
        map.getAt(key);
    }

    Object getAt(int index) {
        return asList().get(index);
    }

    @Override
    Iterator iterator() {
        return map.values().iterator();
    }

    Map<String, Object> asMap() {
        return map.asImmutable();
    }
}
