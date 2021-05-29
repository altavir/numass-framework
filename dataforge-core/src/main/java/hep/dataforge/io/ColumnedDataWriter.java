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
package hep.dataforge.io;

import hep.dataforge.tables.MetaTableFormat;
import hep.dataforge.tables.Table;
import hep.dataforge.tables.TableFormat;
import hep.dataforge.utils.Misc;
import hep.dataforge.values.Values;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Scanner;

/**
 * Вывод форматированного набора данных в файл или любой другой поток вывода
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
public class ColumnedDataWriter implements AutoCloseable {

    private final PrintWriter writer;
    private final TableFormat format;


    public ColumnedDataWriter(OutputStream stream, String... names) {
        this(stream, MetaTableFormat.Companion.forNames(names));
    }


    public ColumnedDataWriter(OutputStream stream, TableFormat format) {
        this(stream, Misc.UTF, format);
    }

    public ColumnedDataWriter(OutputStream stream, Charset encoding, TableFormat format) {
        this.writer = new PrintWriter(new OutputStreamWriter(stream, encoding));
        this.format = format;
    }

    public ColumnedDataWriter(File file, boolean append, String... names) throws FileNotFoundException {
        this(file, append, Charset.defaultCharset(), MetaTableFormat.Companion.forNames(names));
    }

    public ColumnedDataWriter(File file, boolean append, Charset encoding, TableFormat format) throws FileNotFoundException {
        this(new FileOutputStream(file, append), encoding, format);
    }

    /**
     * {@inheritDoc}
     *
     * @throws java.lang.Exception
     */
    @Override
    public void close() throws Exception {
        this.writer.close();
    }

    /**
     * Добавить однострочный или многострочный комментарий
     *
     * @param str a {@link java.lang.String} object.
     */
    public void comment(String str) {
        Scanner sc = new Scanner(str);
        while (sc.hasNextLine()) {
            if (!str.startsWith("#")) {
                writer.print("#\t");
            }
            writer.println(sc.nextLine());
        }
    }

    public void writePoint(Values point) {
        writer.println(IOUtils.formatDataPoint(format, point));
        writer.flush();
    }

    public void writePointList(Collection<Values> collection) {
        collection.stream().forEach((dp) -> {
            writePoint(dp);
        });
    }

    public void writeFormatHeader() {
        writer.println(IOUtils.formatCaption(format));
        writer.flush();
    }

    public void ln() {
        writer.println();
        writer.flush();
    }

    public static void writeTable(File file, Table data, String head, boolean append, String... names) throws IOException {
        try (FileOutputStream os = new FileOutputStream(file, append)) {
            writeTable(os, data, head, names);
        }
    }

    public static void writeTable(OutputStream stream, Table data, String head, String... names) {
        ColumnedDataWriter writer;
        TableFormat format;
        if (data.getFormat().getNames().size() == 0) {
            //Если набор задан в свободной форме, то конструируется автоматический формат по первой точке
            format = MetaTableFormat.Companion.forValues(data.iterator().next());
            LoggerFactory.getLogger(ColumnedDataWriter.class)
                    .debug("No DataSet format defined. Constucting default based on the first data point");
        } else {
            format = data.getFormat();
        }

        if (names.length != 0) {
            format = TableFormat.subFormat(format, names);
        }

        writer = new ColumnedDataWriter(stream, format);
        writer.comment(head);
        writer.ln();

        writer.writeFormatHeader();
        for (Values dp : data) {
            writer.writePoint(dp);
        }
        writer.ln();
    }

}
