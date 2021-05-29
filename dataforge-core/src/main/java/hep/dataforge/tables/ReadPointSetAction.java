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
package hep.dataforge.tables;

import hep.dataforge.actions.OneToOneAction;
import hep.dataforge.context.Context;
import hep.dataforge.description.TypedActionDef;
import hep.dataforge.io.LineIterator;
import hep.dataforge.meta.Laminate;

import java.io.IOException;
import java.io.InputStream;

@TypedActionDef(name = "readdataset", inputType = InputStream.class, outputType = Table.class, info = "Read DataSet from text file")
public class ReadPointSetAction extends OneToOneAction<InputStream, Table> {

    public ReadPointSetAction() {
        super("readdataset", InputStream.class, Table.class);
    }

    public static final String READ_DATA_SET_ACTION_NAME = "readdataset";

    /**
     * {@inheritDoc}
     *
     * @param source
     * @return
     */
    @Override
    protected Table execute(Context context, String name, InputStream source, Laminate meta) {
        ListTable.Builder fileData;

        String encoding = meta.getString("encoding", "UTF-8");
        try {
            LineIterator iterator = new LineIterator(source, encoding);

            String dataSetName = meta.getString("dataSetName", name);

            ValuesReader dpReader;
            if (meta.hasValue("columnNames")) {
                String[] names = meta.getStringArray("columnNames");
                dpReader = new ValuesReader(iterator, names);
                fileData = new ListTable.Builder(names);
            } else {
                dpReader = new ValuesReader(iterator, iterator.next());
                fileData = new ListTable.Builder(dataSetName);
            }

            int headerLines = meta.getInt("headerLength", 0);
            if (headerLines > 0) {
                dpReader.skip(headerLines);
            }

            while (dpReader.hasNext()) {
                fileData.row(dpReader.next());
            }

        } catch (IOException ex) {
            throw new RuntimeException("Can't open data source");
        }
        return fileData.build();
    }

}
