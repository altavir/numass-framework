package hep.dataforge.control.ports

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel


class TcpPortTest {
    var job: Job? = null

    @Before
    fun startServer() {
        GlobalScope.launch {
            println("Starting server")
            val serverSocketChannel = ServerSocketChannel.open()

            serverSocketChannel.socket().bind(InetSocketAddress(9999))

            while (true) {
                delay(0)
                println("Accepting client")
                serverSocketChannel.accept().use {
                    val buffer = ByteBuffer.allocate(1024)
                    val num = it.read(buffer)
                    println("Received $num bytes")
                    buffer.flip()
                    it.write(buffer)
                    buffer.rewind()
                }
            }
        }
    }

    @After
    fun stopServer() {
        job?.cancel()
    }

    @Test
    fun testClient() {
        val channel: SocketChannel = SocketChannel.open(InetSocketAddress("localhost", 9999))
        println("Sending 3 bytes")
        val request = "ddd".toByteArray()
        channel.write(ByteBuffer.wrap(request))
        val buffer = ByteBuffer.allocate(1024)
        channel.read(buffer)
        buffer.flip()
        val response = buffer.toArray()
        assertEquals(request.size, response.size)
        assertEquals(String(request), String(response))
    }

    @Test
    fun testPort() {
        val port = TcpPort("localhost", 9999)
        port.holdBy(object : PortController {
            override fun accept(byte: Byte) {
                println(byte)
            }
        })
        port.send("ddd".toByteArray())
        Thread.sleep(500)
    }
}