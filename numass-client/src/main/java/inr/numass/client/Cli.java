/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.client;

import hep.dataforge.meta.Meta;
import hep.dataforge.storage.commons.StorageManager;
import hep.dataforge.values.Value;
import org.apache.commons.cli.*;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

/**
 * CLI interface for numass client
 * @author Alexander Nozik
 */
public class Cli {

    public static void main(String[] args) {
        new StorageManager().startGlobal();

        Options options = buildOptions();

        CommandLineParser parser = new DefaultParser();

        CommandLine cli;
        try {
            cli = parser.parse(options, args, false);
        } catch (ParseException ex) {
            System.out.println("Error: command line");
            LoggerFactory.getLogger("NumassClient").error("Error while parsing command line", ex);
            System.exit(1);
            return;
        }

        runComand(cli.getOptionValue("a", "192.168.11.1"), Integer.valueOf(cli.getOptionValue("p", "8335")), cli.getArgs());

    }

    public static void runComand(String ip, int port, String... args) {
        checkArgLength(1, args);
        try (NumassClient client = new NumassClient(ip, port)) {
            switch (args[0]) {
                case "getRun":
                    Meta getRun = client.getCurrentRun();
                    if (getRun.getBoolean("success", true)) {
                        System.out.println(getRun.getString("run.path"));
                    } else {
                        System.out.println("Error: operaton failed");
                    }
                    return;
                case "setRun":
                    checkArgLength(2, args);
                    Meta setRun = client.startRun(args[1]);
                    if (setRun.getBoolean("success", true)) {
                        System.out.println(setRun.getString("run.path"));
                    } else {
                        System.out.println("Error: operaton failed");
                    }
                    return;
                case "getState":
                    checkArgLength(2, args);
                    String stateName = args[1];
                    Map<String, Value> states = client.getStates(stateName);
                    if (states != null) {
                        System.out.println(states.get(stateName).stringValue());
                    } else {
                        System.out.println("Error: operaton failed");
                    }
                    return;
                case "setState":
                    checkArgLength(3, args);
                    String setStateName = args[1];
                    String setStateValue = args[2];
                    Meta setStateMeta = client.setState(setStateName, setStateValue);
                    if (setStateMeta.getBoolean("success", true)) {
                        System.out.println("OK");
                    } else {
                        System.out.println("Error: operaton failed");
                    }
                    return;
                case "pushPoint":
                    checkArgLength(2, args);
                    String path;
                    String fileName;
                    if (args.length == 2) {
                        path = "";
                        fileName = args[1];
                    } else {
                        path = args[1];
                        fileName = args[2];
                    }

                    Meta pushPoint = client.sendNumassData(path, fileName);
//                    LoggerFactory.getLogger("Numass-client").debug(pushPoint.toString());
                    if (pushPoint.getBoolean("success", true)) {
                        System.out.println("OK");
                    } else {
                        System.out.println("Error: operaton failed");
                    }
                    return;
                case "addNote":
//                    checkArgLength(2, args);
//                    String note = args[1];
                    String note = new BufferedReader(new InputStreamReader(System.in)).readLine();
                    Meta addNote = client.addNote(note, null);
                    if (addNote.getBoolean("success", true)) {
                        System.out.println("OK");
                    } else {
                        System.out.println("Error: operaton failed");
                    }
            }

        } catch (IOException ex) {
            System.out.println("Error: connection failed");
            LoggerFactory.getLogger("NumassClient").error("Error while initializing connection", ex);
            System.exit(1);
        }
    }

    private static void checkArgLength(int length, String... args) {
        if (args.length < length) {
            LoggerFactory.getLogger("NumassClient").error("Command line to short");
            System.exit(1);
        }
    }

    private static Options buildOptions() {
        Options options = new Options();

        options.addOption("a", "ip", true, "IP address of the server. Default: 192.168.111.1");
        options.addOption("p", "port", true, "Server port. Default: 8335");

        return options;
    }
}
