package hep.dataforge.maths.tables

/**
 * The row representing fixed number row in the table. This row is changed whenever underlying table is changed.
 * Created by darksnake on 13-Nov-16.
 */
class TableRow implements GRow {
    private final GTable table;
    private final int rowNum;

    TableRow(GTable table, int rowNum) {
        this.table = table
        this.rowNum = rowNum
    }

    @Override
    Object getAt(String key) {
        return table[key, rowNum];
    }

    @Override
    Object getAt(int index) {
        return table[index, rowNum];
    }

    @Override
    Map<String, Object> asMap() {
        def res = new LinkedHashMap<String, Object>();
        for (column in table) {
            res.put(column.name, column[rowNum])
        }
        return res;
    }

    @Override
    Iterator<Object> iterator() {
        return new Iterator<Object>() {
            private Iterator<GColumn> columnIterator = table.iterator();

            @Override
            boolean hasNext() {
                return columnIterator.hasNext();
            }

            @Override
            Object next() {
                return (++columnIterator)[rowNum];
            }
        }
    }
}
