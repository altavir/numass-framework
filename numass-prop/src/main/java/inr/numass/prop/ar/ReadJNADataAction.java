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
package inr.numass.prop.ar;

import hep.dataforge.actions.OneToOneAction;
import hep.dataforge.context.Context;
import hep.dataforge.data.DataPoint;
import hep.dataforge.data.DataSet;
import hep.dataforge.data.FileData;
import hep.dataforge.description.TypedActionDef;
import hep.dataforge.description.ValueDef;
import hep.dataforge.exceptions.ContentException;
import hep.dataforge.io.ColumnedDataReader;
import hep.dataforge.io.IOUtils;
import hep.dataforge.io.log.Logable;
import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaBuilder;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 *
 * @author Darksnake
 */
@TypedActionDef(
        name = "readJNA",
        description = "Read data in the format provided by JNA",
        inputType = FileData.class,
        outputType = JNAEpisode.class
)
@ValueDef(name = "numBins", def = "820", type = "NUMBER", info = "Number of bins in the histogram")
@ValueDef(name = "loChanel", def = "0", type = "NUMBER", info = "Lower chanel")
@ValueDef(name = "upChanel", def = "4100", type = "NUMBER", info = "Upper chanel")
@ValueDef(name = "bkgChanel", def = "30", type = "NUMBER", info = "All chanels lower than the background are excluded from analisys")
@ValueDef(name = "temperatureFile", info = "The location of temperature data file")
@ValueDef(name = "timeFile", info = "The location of time data file")
public class ReadJNADataAction extends OneToOneAction<FileData, JNAEpisode> {

    public ReadJNADataAction(Context context, Meta annotation) {
        super(context, annotation);
    }

    @Override
    protected JNAEpisode execute(Logable log, Meta reader, FileData input){
        try {
            InputStream stream = new BufferedInputStream(new FileInputStream(input.getInputFile()));

            String timeFileName = reader.getString("timeFile", input.getInputFile().getName().replace("ar37", "tar37"));
            File timeFile = IOUtils.getFile(input.getInputFile(), timeFileName);
            Scanner timeScanner = new Scanner(timeFile);

            String tempFileName = reader.getString("temperatureFile", "");
            DataSet tempData = null;
            if (!tempFileName.isEmpty()) {
                String[] format = {"time", "T2", "T4", "T5", "T6"};
                File tempFile = IOUtils.getFile(input.getInputFile(), tempFileName);
                ColumnedDataReader fileReader = new ColumnedDataReader(tempFile, format);
                tempData = fileReader.toDataSet(tempFileName);
            }

            List<JNASpectrum> spectra = new ArrayList<>();

            int counter = 1;

            while (timeScanner.hasNext()) {
                log.log("reading spectrum number {}", counter);

                double time = timeScanner.nextDouble();
                Meta annotation = prepareAnnotation(input.meta(), time, time + 1d / 24d, tempData);

                String spName = input.getName() + "_" + counter;

                spectra.add(readSingleSpectrum(spName, annotation, stream));
                counter++;
            }

            JNAEpisode res = new JNAEpisode(input.getName(), spectra);

            //TODO configure list
            return res;
        } catch (FileNotFoundException ex) {
            throw new ContentException("Can't open file " + input.getName());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

    }

    private Meta prepareAnnotation(Meta parent, double startTime, double stopTime, DataSet tempData) {
        MetaBuilder meta = parent.getBuilder();
        meta.putValue("relativeStartTime", startTime);
        meta.putValue("relativeStopTime", stopTime);
        if (tempData != null) {
            MetaBuilder tempBuilder = new MetaBuilder("temperature");
            double t2 = 0;
            double t4 = 0;
            double t5 = 0;
            double t6 = 0;
            int counter = 0;

            for (DataPoint dp : tempData) {
                double time = dp.getDouble("time");
                if (time >= startTime && time <= stopTime) {
                    counter++;
                    t2 += dp.getDouble("T2");
                    t4 += dp.getDouble("T4");
                    t5 += dp.getDouble("T5");
                    t6 += dp.getDouble("T6");
                }
            }

            t2 /= counter;
            t4 /= counter;
            t5 /= counter;
            t6 /= counter;
            //TODO добавить ошибки температуры?

            tempBuilder.putValue("T2", t2);
            tempBuilder.putValue("T4", t4);
            tempBuilder.putValue("T5", t5);
            tempBuilder.putValue("T6", t6);
            meta.putNode(tempBuilder);
        }

        return meta.build();
    }

    private JNASpectrum readSingleSpectrum(String name, Meta annotation, InputStream stream) throws IOException {
        int numBins = getInt("numBins");
        int loChanel = getInt("loChanel");
        int upChanel = getInt("upChanel");
        double binSize = ((double) (upChanel - loChanel)) / numBins;

        Map<Double, Long> spectrum = new LinkedHashMap<>(numBins);
        byte[] buffer = new byte[4];
        for (int i = 0; i < numBins + 1; i++) {
            Double binCenter = i * binSize + binSize / 2;
            stream.read(buffer);
            long count = (buffer[0] & 0xFF) | (buffer[1] & 0xFF) << 8 | (buffer[2] & 0xFF) << 16 | (buffer[3] & 0xFF) << 24;
            if (binCenter > getDouble("bkgChanel")) {
                spectrum.put(binCenter, count);
            }
        }
        return new JNASpectrum(name, annotation, spectrum);
    }

}
