/*
 * Copyright  2018 Alexander Nozik.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package inr.numass.server

import hep.dataforge.io.envelopes.DefaultEnvelopeReader
import hep.dataforge.io.envelopes.DefaultEnvelopeType
import hep.dataforge.io.envelopes.Envelope
import hep.dataforge.messages.errorResponseBase
import hep.dataforge.messages.isTerminator
import hep.dataforge.messages.terminator
import hep.dataforge.meta.Meta
import hep.dataforge.meta.Metoid
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket

/**
 * Abstract network listener for envelopes
 *
 * @author Darksnake
 */
abstract class AbstractNetworkListener(listnerConfig: Meta?) : Metoid, AutoCloseable {

    @Volatile
    private var finishflag = false
    private var serverSocket: ServerSocket? = null
    final override val meta: Meta

    private val port: Int
        get() = meta.getInt("port", 8335)

    init {
        if (listnerConfig == null) {
            this.meta = Meta.buildEmpty("listener")
        } else {
            this.meta = listnerConfig
        }
    }

    @Throws(IOException::class, InterruptedException::class)
    override fun close() {
        finishflag = true
        if (serverSocket != null) {
            serverSocket!!.close()
        }
        logger.info("Closing listner...")
    }

    abstract fun respond(message: Envelope): Envelope

    @Throws(Exception::class)
    open fun open() {
        val clientGroup = ThreadGroup("clients")
        Thread({
            try {
                ServerSocket(port).use { ss ->
                    serverSocket = ss
                    logger.info("Starting to listning to the port {}", port)
                    while (!finishflag) {
                        //FIXME add timeout
                        val s = ss.accept()
                        logger.info("Client accepted from {}", s.remoteSocketAddress.toString())
                        //                    new SocketProcessor(s).run();
                        val socketProcessor = SocketProcessor(s)
                        Thread(clientGroup, socketProcessor).start()
                    }
                }
            } catch (ex: IOException) {
                if (!finishflag) {
                    logger.error("Connection exception", ex)
                }
            }

            logger.info("Listener closed")
            serverSocket = null
        }, "listner").start()
    }

    /**
     * Decide to accept envelope
     *
     * @param envelope
     * @return
     */
    protected fun accept(envelope: Envelope): Boolean {
        return true
    }

    private inner class SocketProcessor (private val socket: Socket) : Runnable {
        private val inputStream: InputStream = socket.getInputStream()
        private val outputStream: OutputStream = socket.getOutputStream()

        override fun run() {
            logger.info("Starting client processing from {}", socket.remoteSocketAddress.toString())
            while (!finishflag) {
                if (socket.isClosed) {
                    finishflag = true
                    logger.debug("Socket {} closed by client", socket.remoteSocketAddress.toString())
                    break
                }
                try {
                    val request = read()
                    //Breaking connection on terminator
                    if (isTerminator(request)) {
                        logger.info("Recieved terminator message from {}", socket.remoteSocketAddress.toString())
                        break
                    }
                    if (accept(request)) {
                        var response: Envelope?
                        try {
                            response = respond(request)
                        } catch (ex: Exception) {
                            logger.error("Uncatched exception during response evaluation", ex)
                            response = errorResponseBase("", ex).build()
                        }

                        //Null respnses are ignored
                        if (response != null) {
                            write(response)
                        }
                    }
                } catch (ex: IOException) {
                    logger.error("IO exception during envelope evaluation", ex)
                    finishflag = true
                }

            }

            logger.info("Client processing finished for {}", socket.remoteSocketAddress.toString())
            if (!socket.isClosed) {
                try {
                    write(terminator)//Sending additional terminator to notify client that server is closing connection
                } catch (ex: IOException) {
                    logger.error("Terminator send failed", ex)
                }

                try {
                    socket.close()
                } catch (ex: IOException) {
                    logger.error("Can't close the socket", ex)
                }

            }
        }

        @Throws(IOException::class)
        private fun read(): Envelope {
            return DefaultEnvelopeReader().readWithData(inputStream)
        }

        @Throws(IOException::class)
        private fun write(envelope: Envelope) {
            DefaultEnvelopeType.INSTANCE.writer.write(outputStream, envelope)
            outputStream.flush()
        }


    }

    companion object {

        private val logger = LoggerFactory.getLogger("LISTENER")
    }
}
