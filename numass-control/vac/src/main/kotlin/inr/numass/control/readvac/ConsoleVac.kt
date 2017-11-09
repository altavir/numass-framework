package inr.numass.control.readvac

import hep.dataforge.control.devices.Sensor
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Options
import java.time.Duration
import java.time.Instant

/**
 * A console based application to test vacuum readings
 * Created by darksnake on 06-Dec-16.
 */
object ConsoleVac {
    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {
        val options = Options()

        options.addOption("c", "class", true, "A short or long class name for vacuumeter device")
        options.addOption("n", "name", true, "A device name")
        options.addOption("p", "port", true, "Port name in dataforge-control notation")
        options.addOption("d", "delay", true, "A delay between measurements in Duration notation")

        if (args.size == 0) {
            HelpFormatter().printHelp("vac-console", options)
            return
        }

        val parser = DefaultParser()
        val cli = parser.parse(options, args)

        var className: String? = cli.getOptionValue("c")
        if (!className!!.contains(".")) {
            className = "inr.numass.readvac.devices." + className
        }
        val name = cli.getOptionValue("n", "sensor")
        val port = cli.getOptionValue("p", "com::/dev/ttyUSB0")
        val delay = Duration.parse(cli.getOptionValue("d", "PT1M"))

        if (className == null) {
            throw RuntimeException("Vacuumeter class not defined")
        }
        val sensor = Class.forName(className)
                .getConstructor(String::class.java).newInstance(port) as Sensor<Double>
        try {
            sensor.init()
            while (true) {
                System.out.printf("(%s) %s -> %g%n", Instant.now().toString(), name, sensor.read())
                Thread.sleep(delay.toMillis())
            }
        } finally {
            sensor.shutdown()
        }
    }
}
