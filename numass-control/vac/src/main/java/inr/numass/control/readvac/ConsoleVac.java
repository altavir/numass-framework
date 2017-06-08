package inr.numass.control.readvac;

import hep.dataforge.control.devices.Sensor;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

import java.time.Duration;
import java.time.Instant;

/**
 * A console based application to test vacuum readings
 * Created by darksnake on 06-Dec-16.
 */
public class ConsoleVac {
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        Options options = new Options();

        options.addOption("c", "class", true, "A short or long class name for vacuumeter device");
        options.addOption("n", "name", true, "A device name");
        options.addOption("p", "port", true, "Port name in dataforge-control notation");
        options.addOption("d", "delay", true, "A delay between measurements in Duration notation");

        if (args.length == 0) {
            new HelpFormatter().printHelp("vac-console", options);
            return;
        }

        DefaultParser parser = new DefaultParser();
        CommandLine cli = parser.parse(options, args);

        String className = cli.getOptionValue("c");
        if (!className.contains(".")) {
            className = "inr.numass.readvac.devices." + className;
        }
        String name = cli.getOptionValue("n", "sensor");
        String port = cli.getOptionValue("p", "com::/dev/ttyUSB0");
        Duration delay = Duration.parse(cli.getOptionValue("d", "PT1M"));

        if (className == null) {
            throw new RuntimeException("Vacuumeter class not defined");
        }
        Sensor<Double> sensor = (Sensor<Double>) Class.forName(className)
                .getConstructor(String.class).newInstance(port);
        try {
            sensor.init();
            while (true) {
                System.out.printf("(%s) %s -> %g%n", Instant.now().toString(), name, sensor.read());
                Thread.sleep(delay.toMillis());
            }
        } finally {
            sensor.shutdown();
        }
    }
}
