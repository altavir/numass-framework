/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.control.magnet.test

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import hep.dataforge.context.Global
import hep.dataforge.control.ports.GenericPortController
import hep.dataforge.control.ports.Port
import hep.dataforge.control.ports.PortFactory
import hep.dataforge.exceptions.PortException
import hep.dataforge.utils.DateTimeUtils
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.Duration
import java.util.*


/**
 * @param args the command line arguments
 */

fun main(args: Array<String>) {
    Locale.setDefault(Locale.US)
    val rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as ch.qos.logback.classic.Logger
    rootLogger.level = Level.INFO

    var portName = "/dev/ttyr00"

    if (args.isNotEmpty()) {
        portName = args[0]
    }
    val handler: Port
    handler = PortFactory.build(portName)
    val controller = GenericPortController(Global, handler, "\r")

    //        LambdaMagnet controller = new LambdaMagnet(handler, 1);
    val reader = BufferedReader(InputStreamReader(System.`in`))

    System.out.printf("INPUT > ")
    var nextString = reader.readLine()

    while ("exit" != nextString) {
        try {
            val start = DateTimeUtils.now()
            val answer = controller.sendAndWait(nextString + "\r", Duration.ofSeconds(1))
            //String answer = controller.request(nextString);
            System.out.printf("ANSWER (latency = %s): %s;%n", Duration.between(start, DateTimeUtils.now()), answer.trim { it <= ' ' })
        } catch (ex: PortException) {
            ex.printStackTrace()
        }

        System.out.printf("INPUT > ")
        nextString = reader.readLine()
    }

    handler.close()

}
