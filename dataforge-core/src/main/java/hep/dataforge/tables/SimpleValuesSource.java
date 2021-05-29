/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.tables;

import hep.dataforge.values.Values;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author Alexander Nozik
 */
public class SimpleValuesSource implements ValuesSource {

    private final TableFormat format;
    private final List<Values> points;

    public SimpleValuesSource(TableFormat format, List<Values> points) {
        this.format = format;
        this.points = new ArrayList<>(points);
    }

    public SimpleValuesSource(TableFormat format) {
        this.format = format;
        this.points = new ArrayList<>();
    }

    public SimpleValuesSource(String... names) {
        this.format = MetaTableFormat.Companion.forNames(names);
        this.points = new ArrayList<>();
    }

    public TableFormat getFormat() {
        return format;
    }

    @Override
    public Iterator<Values> iterator() {
        return points.iterator();
    }

    public void addRow(Values p) {
        this.points.add(p);
    }

}
