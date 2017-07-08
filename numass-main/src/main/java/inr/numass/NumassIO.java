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
package inr.numass;

import hep.dataforge.data.FileDataFactory;
import hep.dataforge.data.binary.Binary;
import hep.dataforge.io.BasicIOManager;
import hep.dataforge.meta.Meta;
import hep.dataforge.names.Name;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.output.TeeOutputStream;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Darksnake
 */
public class NumassIO extends BasicIOManager {

    public static final String NUMASS_OUTPUT_CONTEXT_KEY = "numass.outputDir";

    public static RawNMFile readAsDat(Binary source, Meta config) throws IOException {
        return new LegacyDataReader(source, config).read();
    }

//    private File getOutputDir() {
//        String outputDirPath = getContext().getString(NUMASS_OUTPUT_CONTEXT_KEY, ".");
//        File res = new File(getRootDirectory(), outputDirPath);
//        if (!res.exists()) {
//            res.mkdir();
//        }
//        return res;
//
//    }

//    public static RawNMFile readAsPaw(Binary source, Meta config) throws IOException {
//        return new NumassPawReader().readPaw(source, config.getString(FileDataFactory.FILE_NAME_KEY));
//    }

    public static RawNMFile getNumassData(Binary binary, Meta config) {
        try {
            RawNMFile dataFile;
            String extension = FilenameUtils.getExtension(config.getString(FileDataFactory.FILE_NAME_KEY)).toLowerCase();
            switch (extension) {
                case "dat":
                    dataFile = readAsDat(binary, config);
                    break;
                default:
                    throw new RuntimeException("Wrong file format");
            }
            return dataFile;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public OutputStream out(Name stage, Name name) {
        List<String> tokens = new ArrayList<>();
        if (getContext().hasValue("numass.path")) {
            String path = getContext().getString("numass.path");
            if (path.contains(".")) {
                tokens.addAll(Arrays.asList(path.split(".")));
            } else {
                tokens.add(path);
            }
        }

        if (stage != null) {
            tokens.addAll(Arrays.asList(stage.asArray()));
        }

        String dirName = String.join(File.separator, tokens);
        String fileName = name.removeNameSpace().toString() + ".out";
        return buildOut(getWorkDirectory(), dirName, fileName);
    }

    protected OutputStream buildOut(File parentDir, String dirName, String fileName) {
        File outputFile;

        if (!parentDir.exists()) {
            throw new RuntimeException("Working directory does not exist");
        }
        if (dirName != null && !dirName.isEmpty()) {
            parentDir = new File(parentDir, dirName);
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }
        }

//        String output = source.meta().getString("output", this.meta().getString("output", fileName + ".onComplete"));
        outputFile = new File(parentDir, fileName);
        try {
            if (getContext().getBoolean("numass.consoleOutput", false)) {
                return new TeeOutputStream(new FileOutputStream(outputFile), System.out);
            } else {
                return new FileOutputStream(outputFile);
            }
        } catch (FileNotFoundException ex) {
            throw new RuntimeException(ex);
        }
    }
}
