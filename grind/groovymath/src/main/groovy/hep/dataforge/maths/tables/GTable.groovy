package hep.dataforge.maths.tables

/**
 * A dynamic Groovy table
 * Created by darksnake on 26-Oct-15.
 */
class GTable implements Iterable<GColumn> {
    List<GColumn> columns = new ArrayList<>();

    /**
     * empty constructor
     */
    GTable() {
    }

    /**
     * From a list of columns
     * @param columns
     */
    GTable(List<GColumn> columns) {
        this.columns = columns
    }

    GColumn getAt(int index) {
        return columns[index];
    }

    GColumn getAt(String name) {
        return columns.find { it.name == name }
    }

    /**
     * A double index access. First index is a column, second is a value in column
     * @param index1
     * @param index2
     * @return
     */
    def getAt(index1, index2) {
        return getAt(index1).getAt(index2);
    }

    /**
     * Put or replace column with given index. The method uses copy-constructor of the GColumn class.
     * @param index
     * @param column
     * @return
     */
    def putAt(int index, Object column) {
        columns[index] = (new GColumn(column));
    }

    /**
     * Add new column with the given name.
     * The method uses copy-constructor of the GColumn class and changes the name of the column.
     * @param name
     * @param column
     * @return
     */
    int addColumn(String name, Object column) {
        //Using list constructor or copy constructor
        GColumn col = new GColumn(column);
        //replacing name
        col.name = name;
        ///adding to column list
        columns << col
        return columns.size() - 1;
    }


    def putAt(String name, Object column) {
        addColumn(name, column)
    }

    def synchronized addRow(GRow row) {
        //filling blank spaces
        fillNulls();

        for (e in row.asMap()) {
            getAt(e.key).add(e.value);
        }
    }

    def addRow(List row) {
        addRow(new MapRow(getColumnNames(), row))
    }

    /**
     * add a GColumn
     * @param column
     */
    def leftShift(GColumn column) {
        columns += column;
    }

    /**
     * Add a GRow
     * @param row
     * @return
     */
    def leftShift(MapRow row) {
        addRow(row)
    }

    GRow row(int index) {
        return new TableRow(this, index);
    }

    /**
     * List of all rows. Missing values are automatically replaced by apropriate nulls
     * @return
     */
    List<GRow> getRows() {
        return [0..maxColumnLength()].collect { row(it) };
    }

    /**
     * Iterator for better performance and less memory impact work with rows (does not store all rows in separate structure simultaneously)
     * @return
     */
    Iterator<GRow> getRowIterator() {
        return new Iterator<MapRow>() {
            int index = 0;

            @Override
            boolean hasNext() {
                return index < maxColumnLength() - 1;
            }

            @Override
            MapRow next() {
                index++;
                return row(index);
            }
        }
    }

    /**
     * The length of the longest column
     * @return
     */
    protected int maxColumnLength() {
        columns.parallelStream().mapToInt { it.size() }.max();
    }

    /**
     * Fill all existing columns with nulls to the maximum column length
     */
    protected fillNulls() {
        int maxSize = maxColumnLength();
        columns.each {
            if (it.size() < maxSize) {
                for (int i = it.size(); i < maxSize; i++) {
                    it[i] = it.nullValue();
                }
            }
        }
    }

    List<List<?>> getValues() {
        getRows().collect { it.asList() }
    }

    List<String> getColumnNames() {
        columns.withIndex().collect { item, index -> item.name ?: index; }
    }

    List<String> getTypes() {
        columns.collect { it.type }
    }

    @Override
    Iterator<GColumn> iterator() {
        return columns.iterator();
    }
}
