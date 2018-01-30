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

import hep.dataforge.actions.ActionUtils;
import hep.dataforge.context.Context;
import hep.dataforge.context.Global;
import hep.dataforge.context.IOManager;
import hep.dataforge.data.FileDataFactory;
import hep.dataforge.io.MetaFileReader;
import hep.dataforge.meta.Meta;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.util.Locale;

import static hep.dataforge.context.Global.out;
import static inr.numass.Numass.printDescription;
import static java.util.Locale.setDefault;

/**
 *
 */
public class Main {

    public static void main(String[] args) throws Exception {
        setDefault(Locale.US);

        Context context = Numass.buildContext();
        run(context, args);
    }

    public static void run(Context context, String[] args) throws Exception {
        if(context == null){
            context = Global.Companion.instance();
        }
        Logger logger = LoggerFactory.getLogger("numass-main");

        Options options = prepareOptions();
        CommandLineParser parser = new DefaultParser();
        CommandLine line;
        try {
            // parse the command line arguments
            line = parser.parse(options, args);
        } catch (ParseException exp) {
            // oops, something went wrong
            logger.error("Command line error.  Reason: " + exp.getMessage());
            return;
        }

        if (line.hasOption("l")) {
            printDescription(context);
            return;
        }

        String cfgPath;

        if (args.length == 0) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java -jar DataReader.jar [OPTIONS]", options);
            Companion.out().println("Trying to use default config location...");
        }

        if (line.hasOption("c")) {
            cfgPath = line.getOptionValue("c");
            if (cfgPath == null) {
                logger.info("Configutation path not provided.");
                return;
            }

            java.nio.file.Path configFile = context.getIo().getFile(cfgPath);

            if (!Files.exists(configFile)) {
                throw new FileNotFoundException("Configuration file not found");
            }

            Meta config = MetaFileReader.read(configFile);

            context.setValue(IOManager.ROOT_DIRECTORY_CONTEXT_KEY, configFile.getParent().toString());

            applyCLItoContext(line, context);

            ActionUtils.runConfig(context, config);
        }
    }

    public static void applyCLItoContext(CommandLine line, Context context) throws FileNotFoundException {
        File workDir = new File(context.getString(IOManager.ROOT_DIRECTORY_CONTEXT_KEY));

        if (line.hasOption("h")) {
            workDir = new File(line.getOptionValue("h"));
            context.setValue(IOManager.ROOT_DIRECTORY_CONTEXT_KEY, workDir.toString());
        }

        if (line.hasOption("d")) {
            String dataPath = line.getOptionValue("d");
            File dataDir = new File(dataPath);
            if (!dataDir.isAbsolute()) {
                dataDir = new File(workDir, dataPath);
            }
            if (dataDir.exists() && dataDir.isDirectory()) {
                context.setValue(FileDataFactory.DATA_DIR_KEY, dataDir.getAbsolutePath());
            } else {
                throw new FileNotFoundException("Data directory not found");
            }
        }

        if (line.hasOption("o")) {
            String outPath = line.getOptionValue("o");
            File outDir = new File(outPath);
            if (!outDir.isAbsolute()) {
                outDir = new File(workDir, outPath);
            }
            if (!outDir.exists()) {
                outDir.mkdirs();
            }
            context.setValue(NumassIO.Companion.getNUMASS_OUTPUT_CONTEXT_KEY(), outDir.toString());
        }
    }

    private static Options prepareOptions() {
        Options options = new Options();

        options.addOption("c", "config", true, "Configuration file path. "
                + "If this option is not present, than workbench is launched and all other parameters are ignored.");
        options.addOption("h", "home", true,
                "Working directory (by default the working directory is the directory where config file is placed)");
        options.addOption("d", "data", true, "Data directory (absolute or relative to working directory)");
        options.addOption("o", "onComplete", true, "Output directory (absolute or relative to working directory)");
        options.addOption("l", "list", false, "List of available actions");
        options.addOption("lc", "list-color", false, "List of available actions with ANSI coloring");

        return options;
    }

    private static String getFilePathFromDialog(String homeDir) throws FileNotFoundException {
        //TODO переместить в IOManager

        JFrame frame = new JFrame("Chose a configuration file");
        JFileChooser fc = new JFileChooser(homeDir);
        FileFilter xmlFilter = new FileNameExtensionFilter("XML files", "XML", "xml");
//        fc.addChoosableFileFilter(xmlFilter);

        fc.setFileFilter(xmlFilter);

        int returnVal = fc.showOpenDialog(frame);
        File file;
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            file = fc.getSelectedFile();
            frame.dispose();
            return file.getAbsolutePath();
        } else {
            frame.dispose();
            return null;
        }

    }

}
