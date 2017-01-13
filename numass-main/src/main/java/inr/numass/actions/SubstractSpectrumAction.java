/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.actions;

import hep.dataforge.actions.OneToOneAction;
import hep.dataforge.context.Context;
import hep.dataforge.description.TypedActionDef;
import hep.dataforge.io.ColumnedDataReader;
import hep.dataforge.io.ColumnedDataWriter;
import hep.dataforge.meta.Laminate;
import hep.dataforge.tables.DataPoint;
import hep.dataforge.tables.ListTable;
import hep.dataforge.tables.MapPoint;
import hep.dataforge.tables.Table;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;

/**
 * @author <a href="mailto:altavir@gmail.com">Alexander Nozik</a>
 */
@TypedActionDef(name = "substractSpectrum", inputType = Table.class, outputType = Table.class, info = "Substract reference spectrum (background)")
public class SubstractSpectrumAction extends OneToOneAction<Table, Table> {

    @Override
    protected Table execute(Context context, String name, Table input, Laminate inputMeta) {
        try {
            String referencePath = inputMeta. getString("file", "empty.dat");
            File referenceFile = context.io().getFile(referencePath);
            Table referenceTable = new ColumnedDataReader(referenceFile).toTable();
            ListTable.Builder builder = new ListTable.Builder(input.getFormat());
            input.stream().forEach(point -> {
                MapPoint.Builder pointBuilder = new MapPoint.Builder(point);
                Optional<DataPoint> referencePoint = referenceTable.stream()
                        .filter(p -> Math.abs(p.getDouble("Uset") - point.getDouble("Uset")) < 0.1).findFirst();
                if (referencePoint.isPresent()) {
                    pointBuilder.putValue("CR", Math.max(0, point.getDouble("CR") - referencePoint.get().getDouble("CR")));
                    pointBuilder.putValue("CRerr", Math.sqrt(Math.pow(point.getDouble("CRerr"), 2d) + Math.pow(referencePoint.get().getDouble("CRerr"), 2d)));
                } else {
                    report(context, name, "No reference point found for Uset = {}", point.getDouble("Uset"));
                }
                builder.row(pointBuilder.build());
            });

            Table res = builder.build();
            OutputStream stream = buildActionOutput(context, name);
            ColumnedDataWriter.writeDataSet(stream, res, inputMeta.toString());
            return res;
        } catch (IOException ex) {
            throw new RuntimeException("Could not read reference file", ex);
        }
    }

}
