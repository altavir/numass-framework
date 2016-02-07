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
package inr.numass.readvac;

import hep.dataforge.meta.Meta;
import hep.dataforge.context.Context;
import hep.dataforge.context.GlobalContext;
import static hep.dataforge.context.GlobalContext.out;
import hep.dataforge.io.MetaFileReader;
import hep.dataforge.storage.api.Storage;
import hep.dataforge.storage.commons.StorageManager;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 *
 * @author Darksnake
 */
public class Main {

    static String name;
//    static volatile boolean stopFlag = false;

    /**
     * @param args the command line arguments
     * @throws java.lang.Exception
     */
    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.US);
        new StorageManager().startGlobal();
        Meta config = getAnnotation(args);
        runConfig(config);
//        System.exit(0);//на всякий случай, чтобы закрыть все боковые потоки
    }

    public static void runConfig(Meta config) throws Exception {

//        VACManager daemon;
//        //Определяем, является считывает ли сервер из файла или из com порта
//        boolean direct = config.getBoolean("direct", true);
        //Префикс сеанса
        String prefix = config.getString("run", name);

        Storage server = setupServer(GlobalContext.instance(), config);

//        if (direct) {
//            daemon = VACManager.fromSerial(server, getSerialConfig(config), prefix);
//        } else {
//            daemon = VACManager.fromDirectory(server, getDataPath(config), prefix);
//        }
        VACManager daemon = VACManager.fromSerial(server, prefix, getSerialConfig(config));
        daemon.start();
    }

    private static Meta getSerialConfig(Meta config) {
        return config.getNode("serialconfig", config);
    }

    private static Storage setupServer(Context context, Meta config) {
        Meta storageConfig = config.getNode("storage");

        return context.provide("storage", StorageManager.class).buildStorage(storageConfig);
    }

//    private static String getDataPath(Meta config) {
//        return config.getString("datapath", "D:\\temp\\test");
//    }
    private static Options prepareOptions() {
        Options options = new Options();

        options.addOption("c", "config", true, "Configuration file path");
//        options.addOption("d", "dir", true, "Directory with data files");
        options.addOption("n", "name", true, "Run name");
        options.addOption("h", "home", true, "Working directory");
//        options.addOption("t", "target", true, "Target data file");

        return options;
    }

    private static Meta getAnnotation(String[] cli) throws IOException, ParseException, java.text.ParseException {
        String configPath;
        Options options = prepareOptions();
        if (cli.length == 0) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java -jar readVAC.jar [OPTIONS]", options);
            out().println("Trying to use default config location...");
            configPath = "vac-config.xml";
        } else {

            CommandLineParser parser = new DefaultParser();
            CommandLine line;
            // parse the command line arguments
            line = parser.parse(options, cli);

            if (line.hasOption("h")) {
                System.setProperty("user.dir", line.getOptionValue("h"));
            }

            if (line.hasOption("n")) {
                name = line.getOptionValue("n");
            }
            configPath = line.getOptionValue("c", "config.xml");

        }
        File configFile = new File(configPath);

        return MetaFileReader.read(configFile);

    }

}
