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
package inr.numass.utils;

import hep.dataforge.context.Global;
import hep.dataforge.tables.ListTable;
import hep.dataforge.tables.Table;
import hep.dataforge.tables.ValueMap;
import hep.dataforge.values.Values;
import inr.numass.data.SpectrumDataAdapter;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Locale;
import java.util.Scanner;

import static java.util.Locale.setDefault;

/**
 *
 * @author Darksnake
 */
public class OldDataReader {

    public static Table readConfig(String path) throws FileNotFoundException {
        String[] list = {"X", "time", "ushift"};
        ListTable.Builder res = new ListTable.Builder(list);
        File file = Global.instance().io().getFile(path);
        Scanner sc = new Scanner(file);
        sc.nextLine();

        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            Scanner lineScan = new Scanner(line);
            int time = lineScan.nextInt();
            double u = lineScan.nextDouble();
            double ushift = 0;
            if (lineScan.hasNextDouble()) {
                ushift = lineScan.nextDouble();
            }
            Values point = new ValueMap(list, u, time, ushift);
            res.row(point);
        }
        return res.build();
    }

    public static Table readData(String path, double Elow) {
        SpectrumDataAdapter factory = new SpectrumDataAdapter();
        ListTable.Builder res = new ListTable.Builder(factory.getFormat());
        File file = Global.instance().io().getFile(path);
        double x;
        int count;
        int time;

        setDefault(Locale.ENGLISH);

        Scanner sc;
        try {
            sc = new Scanner(file);
        } catch (FileNotFoundException ex) {
            throw new RuntimeException(ex.getMessage());
        }
        double dummy;
//        sc.skip("\\D*");       
        while (!sc.hasNextDouble()) {
            sc.nextLine();
        }
        while (sc.hasNextDouble() | sc.hasNextInt()) {
            /*Надо сделать, чтобы считывало весь файл*/
            x = sc.nextDouble();

            dummy = sc.nextInt();

            time = sc.nextInt();

            dummy = sc.nextInt();
            dummy = sc.nextInt();
            dummy = sc.nextInt();
            dummy = sc.nextInt();
            dummy = sc.nextInt();
            dummy = sc.nextInt();

            count = sc.nextInt();
//            count = (int) (count / (1 - 2.8E-6 / time * count));

            dummy = sc.nextInt();
            dummy = sc.nextDouble();
            dummy = sc.nextDouble();
            dummy = sc.nextDouble();
            Values point = factory.buildSpectrumDataPoint(x, count, time);
            if (x >= Elow) {
                res.row(point);
            }

        }
        return res.build();
    }

    public static Table readDataAsGun(String path, double Elow) {
        SpectrumDataAdapter factory = new SpectrumDataAdapter();
        ListTable.Builder res = new ListTable.Builder(factory.getFormat());
        File file = Global.instance().io().getFile(path);
        double x;
        long count;
        int time;

        setDefault(Locale.ENGLISH);

        Scanner sc;
        try {
            sc = new Scanner(file);
        } catch (FileNotFoundException ex) {
            throw new RuntimeException(ex.getMessage());
        }
        double dummy;
        sc.nextLine();
        while (sc.hasNext()) {
            x = sc.nextDouble();
            time = sc.nextInt();
            dummy = sc.nextInt();
            count = sc.nextLong();
            dummy = sc.nextDouble();
            dummy = sc.nextDouble();
            Values point = factory.buildSpectrumDataPoint(x, count, time);
            if (x > Elow) {
                res.row(point);
            }
        }
        return res.build();
    }

    public static Table readSpectrumData(String path) {
        SpectrumDataAdapter factory = new SpectrumDataAdapter();
        ListTable.Builder res = new ListTable.Builder(factory.getFormat());
        File file = Global.instance().io().getFile(path);
        double x;
        double count;
        double time;

        double cr;
        double crErr;

        setDefault(Locale.ENGLISH);

        Scanner sc;
        try {
            sc = new Scanner(file);
        } catch (FileNotFoundException ex) {
            throw new RuntimeException(ex.getMessage());
        }

        while (sc.hasNext()) {
            String line = sc.nextLine();
            if (!line.startsWith("*")) {
                Scanner lsc = new Scanner(line);
                if (lsc.hasNextDouble() || lsc.hasNextInt()) {

                    x = lsc.nextDouble();
                    lsc.next();
                    time = lsc.nextDouble();
                    lsc.next();
                    lsc.next();
                    count = lsc.nextDouble();
                    cr = lsc.nextDouble();
                    crErr = lsc.nextDouble();
                    Values point = factory.buildSpectrumDataPoint(x, (long) (cr * time), crErr * time, time);
//            SpectrumDataPoint point = new SpectrumDataPoint(x, (long) count, time);

                    res.row(point);
                }
            }
        }
        return res.build();
    }

}
