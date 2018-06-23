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

package inr.numass.control.dante

import hep.dataforge.kodex.orElse
import hep.dataforge.meta.Meta
import hep.dataforge.meta.buildMeta
import inr.numass.control.dante.DanteClient.Companion.CommandType.*
import inr.numass.control.dante.DanteClient.Companion.Register.*
import inr.numass.data.NumassProto
import inr.numass.data.api.NumassPoint
import inr.numass.data.storage.ProtoNumassPoint
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.Channel
import org.slf4j.LoggerFactory
import java.io.DataInputStream
import java.io.OutputStream
import java.lang.Math.pow
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import kotlin.collections.HashMap
import kotlin.coroutines.experimental.buildSequence
import kotlin.math.ceil

internal val Byte.positive
    get() = toInt() and 0xFF

internal val Int.positive
    get() = toLong() and 0xFFFFFFFF

internal val Int.byte: Byte
    get() {
        if (this >= 256) {
            throw RuntimeException("Number less than 256 is expected")
        } else {
            return toByte()
        }
    }

internal val Long.int: Int
    get() {
        if (this >= 0xFFFFFFFF) {
            throw RuntimeException("Number less than ${Int.MAX_VALUE * 2L} is expected")
        } else {
            return toInt()
        }
    }

internal val ByteArray.hex
    get() = this.joinToString(separator = "") { it.positive.toString(16).padStart(2, '0') }


//TODO implement using Device
class DanteClient(val ip: String, chainLength: Int) : AutoCloseable {
    private val RESET_COMMAND = byteArrayOf(0xDD.toByte(), 0x55, 0xDD.toByte(), 0xEE.toByte())

    private val logger = LoggerFactory.getLogger(javaClass)

    private val packetNumber = AtomicLong(0)

    private lateinit var parentJob: Job
    private val pool = newFixedThreadPoolContext(8, "Dante")

    private val connections: MutableMap<Int, Pair<Socket, Job>> = HashMap()

    private val sendChannel = Channel<ByteArray>(capacity = Channel.UNLIMITED)
    private lateinit var output: OutputStream
    private lateinit var outputJob: Job

    /**
     * Synchronous reading and writing of registers
     */
    private val comChannel = Channel<DanteMessage>(capacity = Channel.UNLIMITED)

    /**
     * Data packets channel
     */
    private val dataChannel = Channel<DanteMessage>(capacity = Channel.UNLIMITED)
    //private val statisticsChannel = Channel<Communications.DanteMessage>(capacity = Channel.UNLIMITED)

    /**
     * @param num number
     * @param name the name of the board
     */
    private data class BoardState(val num: Int, var meta: Meta? = null)

    private val boards = (0 until chainLength).map { BoardState(it) }


    fun open() {
        //create a new parent job
        this.parentJob = Job()
        (0..3).forEach {
            openPort(it)
        }
    }

    override fun close() {
        //TODO send termination signal
        connections.values.forEach {
            it.first.close()
            it.second.cancel()
        }
        parentJob.cancel(CancellationException("Server stopped"))
    }

    /**
     * Disconnect and reconnect without clearing configuration
     */
    fun reconnect() {
        close()
        runBlocking {
            clearData()
        }
        open()
    }

    /**
     * Reset everything
     */
    fun reset() {
        async {
            sendChannel.send(RESET_COMMAND)
        }
    }

    private fun openPort(port: Int) {
        //closing existing connection
        connections[port]?.let {
            it.first.close()
            it.second.cancel()
        }

        val socket = Socket(ip, 8000 + port)

        logger.info("Opened socket {}", "${socket.inetAddress}:${socket.port}")

        //Create command queue on port 0
        if (port == 0) {
            //outputJob.cancel()
            output = socket.getOutputStream()
            outputJob = launch(context = pool, parent = parentJob) {
                while (true) {
                    val command = sendChannel.receive()
                    output.write(command)
                    logger.trace("Sent {}", command.hex)
                }
            }
        }


        val job = launch(context = pool, parent = parentJob) {
            val stream = socket.getInputStream()
            while (true) {
                if (stream.read() == PACKET_HEADER_START_BYTES[0] && stream.read() == PACKET_HEADER_START_BYTES[1]) {
                    // second check is not executed unless first one is true
                    val header = ByteArray(6)
                    stream.read(header)
                    val command = CommandType.values().find { it.byte == header[0] }
                            ?: throw RuntimeException("Unknown command code: ${header[0]}")
                    val board = header[1]
                    val packet = header[2]
                    val length = (header[3].positive * 0x100 + header[4].positive * 0x010 + header[5].positive) * 4
                    if (length > 8) {
                        logger.trace("Received long message with header $header")
                    }
                    val payload = ByteArray(length)
                    DataInputStream(stream).readFully(payload)
                    handle(DanteMessage(command, board.positive, packet.positive, payload))
                }
            }
            //TODO handle errors and reconnect
        }

        connections[port] = Pair(socket, job)
    }

    private suspend fun send(command: CommandType, board: Int, packet: Int, register: Int, data: ByteArray = ByteArray(0), length: Int = (data.size / 4)) {
        logger.debug("Sending {}[{}, {}] of size {}*4 to {}", command.name, board, packet, length, register)
        sendChannel.send(wrapCommand(command, board, packet, register, length, data))
    }

    private suspend fun handle(response: DanteMessage) {
        logger.debug("Received {}", response.toString())
        when (response.command) {
            READ, WRITE -> comChannel.send(response)
            SINGLE_SPECTRUM_MODE, MAP_MODE, LIST_MODE, WAVEFORM_MODE -> dataChannel.send(response)
        }
    }

    /**
     * Generate next packet number
     */
    private fun nextPacket(): Int {
        return (packetNumber.incrementAndGet() % 256).toInt()
    }

    private fun List<Long>.asBuffer(): ByteBuffer {
        val buffer = ByteBuffer.allocate(this.size * 4)
        buffer.order(ByteOrder.BIG_ENDIAN)
        this.forEach { buffer.putInt(it.int) } //safe transform to 4-byte integer
        return buffer
    }

    private val ByteBuffer.bits: BitSet
        get() = BitSet.valueOf(this)

    /**
     * Write single or multiple registers
     *
     */
    private suspend fun writeRegister(board: Int, register: Int, data: List<Long>, mask: Long? = null) {
        //representing data as byte buffer
        val buffer = if (mask != null) {
            val oldData = withTimeout(5000) { readRegister(board, register, data.size) }
            //(oldData & not_mask) | (newData & mask);
            (0 until data.size).map { (oldData[it] and mask.inv()).or(data[it] and mask) }.asBuffer();
        } else {
            data.asBuffer()
        }

        send(WRITE, board, nextPacket(), register, buffer.array())
    }

    private suspend fun writeRegister(board: Int, register: Int, data: Long, mask: Long? = null) {
        writeRegister(board, register, listOf(data), mask)
    }

    private suspend fun readRegister(board: Int, register: Int, length: Int = 1): List<Long> {
        val packet = nextPacket()
        send(READ, board, packet, register, length = length)
        var message: DanteMessage? = null
        //skip other responses
        while (message == null || !(message.command == READ && message.packet == packet)) {
            message = comChannel.receive()
        }

        return buildSequence {
            val intBuffer = ByteBuffer.wrap(message.payload).asIntBuffer()
            while (intBuffer.hasRemaining()) {
                yield(intBuffer.get())
            }
        }.map { it.positive }.toList()
    }

    suspend fun getFirmwareVersion(): Long {
        return readRegister(0, Register.FIRMWARE_VERSION.code)[0]
    }

    /**
     * Write a single DPP parameter
     * @param register number of parameter register
     * @param value value of the parameter as unsigned integer
     */
    suspend fun writeDPP(board: Int, register: Int, value: Long) {
        //sending register number and value in the same command
        writeRegister(board, DPP_REGISTER_1.code, listOf(register.toLong(), value))
        writeRegister(board, DPP_CONFIG_COMMIT_OFFSET.code, 0x00000001, 0x00000001)
    }

    /**
     * Configure single board using provided meta
     */
    suspend fun configureBoard(board: Int, meta: Meta) {
        val gain = meta.getDouble("gain")
        val det_thresh = meta.getInt("detection_threshold")
        val pileup_thr = meta.getInt("pileup_threshold")
        val en_fil_peak_time = meta.getInt("energy_filter.peaking_time")
        val en_fil_flattop = meta.getInt("energy_filter.flat_top")
        val fast_peak_time = meta.getInt("fast_filter.peaking_time")
        val fast_flattop = meta.getInt("fast_filter.flat_top")
        val recovery_time = meta.getValue("recovery_time").long
        val zero_peak_rate = meta.getInt("zero_peak_rate")
        val inverted_input = meta.getInt("inverted_input", 0)

        assert(en_fil_peak_time in 1..511)
        assert(gain in 0.01..(en_fil_peak_time * 2 - 0.01))
        assert(det_thresh in 0..4096)
        assert(pileup_thr in 0..4096)
        assert(en_fil_flattop in 1..15)
        assert(fast_peak_time in 1..31)
        assert(fast_flattop in 1..31)
        assert(recovery_time in (0.0..pow(2.0, 24.0) - 1))
        assert(zero_peak_rate in 0..500)
        assert(inverted_input in listOf(0, 1))
        assert((en_fil_peak_time + en_fil_flattop) * 2 < 1023)

        logger.info("Starting {} board configuration", board)

        writeDPP(board, 128, inverted_input * (1L shl 24))
        writeDPP(board, 162, recovery_time)
        writeDPP(board, 181, 0)
        writeDPP(board, 185, 0)
        writeDPP(board, 175, 1)
        writeDPP(board, 160, (pileup_thr / gain).toLong() + (1 shl 31))
        writeDPP(board, 160, (pileup_thr / gain * 2).toLong() and (1 shl 31).inv()) //set bit 2 to zero
        writeDPP(board, 152, (det_thresh / gain * fast_peak_time).toLong())

        if (fast_flattop == 1) {
            writeDPP(board, 154, 0)
        } else {
            writeDPP(board, 154, ceil(fast_flattop.toDouble() / 2).toLong()) // TODO check this
        }

        if (zero_peak_rate == 0) {
            writeDPP(board, 142, 0)
        } else {
            writeDPP(board, 142, (1.0 / zero_peak_rate / 10 * 1e6).toLong() + (1 shl 31))
        }

        writeDPP(board, 180, (fast_flattop + 1).toLong())

        if ((2 * fast_peak_time + fast_flattop) > 4 * en_fil_flattop) {
            writeDPP(board, 140, ((2 * fast_peak_time + fast_flattop) * 4 + 1).toLong())
        } else {
            writeDPP(board, 140, en_fil_flattop.toLong())
        }

        writeDPP(board, 141, (fast_peak_time + fast_flattop + 4).toLong())
        writeDPP(board, 156, fast_peak_time + 0 * (1L shl 28))
        writeDPP(board, 150, fast_flattop + 0 * (1L shl 28))
        writeDPP(board, 149, en_fil_peak_time + 1 * (1L shl 28))
        writeDPP(board, 150, en_fil_flattop + 1 * (1L shl 28))
        writeDPP(board, 153, en_fil_peak_time * 2 + en_fil_flattop * 2 + 1 * (1L shl 28))
        writeDPP(board, 184, (gain * (1 shl 24) / en_fil_peak_time).toLong() + 1 * (1L shl 28))
        writeDPP(board, 148, 0b11)
        writeDPP(board, 128, 0b100 + inverted_input * (1L shl 24))
        writeDPP(board, 128, 1 + inverted_input * (1L shl 24))


        logger.info("Finished {} board configuration", board)
        boards.find { it.num == board }?.meta = meta
    }

    /**
     * Configure all boards
     */
    suspend fun configureAll(meta: Meta) {
        boards.forEach {
            configureBoard(it.num, meta)
        }
        logger.info("Finished configuration of all actibe boards")
    }

    /**
     * Clear unused data
     */
    private suspend fun clearData() {
        while (!dataChannel.isEmpty) {
            logger.warn("Dumping residual data packet {}", dataChannel.receive().toString())
        }
    }

    private suspend fun clearCommunications() {
        while (!dataChannel.isEmpty) {
            logger.debug("Dumping residual communication packet {}", dataChannel.receive().toString())
        }
    }

    /**
     * Handle statistics asynchronously
     */
    fun handleStatistics(channel: Int, message: ByteBuffer) {
        logger.info("Received statistics packet from board {}", channel)
        //TODO
    }

    /**
     * Gather data in list mode
     * @param length measurement time in milliseconds
     * @param statisticsPackets number of statistics packets per measurement
     */
    suspend fun readPoint(length: Int, statisticsInterval: Int = 1000): NumassPoint {
        clearData()

        logger.info("Starting list point acquisition {} ms", length)
        boards.forEach {
            writeRegister(it.num, ACQUISITION_SETTINGS.code, AcquisitionMode.LIST_MODE.long, 0x00000007)
            writeRegister(it.num, ACQUISITION_TIME.code, length.toLong())
            writeRegister(it.num, TIME_PER_MAP_POINT.code, statisticsInterval.toLong(), 0x00FFFFFF)
            writeRegister(it.num, MAP_POINTS.code, (length.toDouble() / statisticsInterval).toLong(), 0x00FFFFFF)

            if (readRegister(it.num, ACQUISITION_SETTINGS.code)[0] != AcquisitionMode.LIST_MODE.long) {
                throw RuntimeException("Set list mode failed")
            }
        }

        writeRegister(0, ACQUISITION_STATUS.code, 0x00000001, 0x00000001)

        val start = Instant.now()
        val builder = NumassProto.Point.newBuilder()

        //collecting packages

        while (Duration.between(start, Instant.now()) < Duration.ofMillis(length + 2000L)) {
            try {
                //Waiting for a packet for a second. If it is not there, returning and checking time condition
                val packet = withTimeout(1000) { dataChannel.receive() }
                if (packet.command != LIST_MODE) {
                    logger.warn("Unexpected packet type: {}", packet.command.name)
                    continue
                }
                val channel = packet.board
                // get or create new channel block
                val channelBuilder = builder.channelsBuilderList.find { it.id.toInt() == channel }
                        .orElse {
                            builder.addChannelsBuilder().setId(channel.toLong())
                                    .also {
                                        //initializing single block
                                        it.addBlocksBuilder().also {
                                            it.time = (start.epochSecond * 1e9 + start.nano).toLong()
                                            it.binSize = 8 // tick in nanos
                                            it.length = (length * 1e6).toLong() //block length in nanos
                                        }
                                    }
                        }
                val blockBuilder = channelBuilder.getBlocksBuilder(0)
                val eventsBuilder = blockBuilder.eventsBuilder

                val buffer = ByteBuffer.wrap(packet.payload)
                while (buffer.hasRemaining()) {
                    val firstWord = buffer.getInt()
                    val secondWord = buffer.getInt()
                    if (firstWord == STATISTIC_HEADER && secondWord == STATISTIC_HEADER) {
                        if (buffer.remaining() < 128) {
                            logger.error("Can't read statistics from list message, 128 bytes expected, but {} found", buffer.remaining())
                            break
                        } else {
                            val statistics = ByteArray(32 * 4)
                            buffer.get(statistics)
                            //TODO use statistics for meta
                            handleStatistics(channel, ByteBuffer.wrap(statistics))
                        }
                    } else if (firstWord == 0) {
                        //TODO handle zeros
                        logger.info("Received zero packet from board {}", channel)
                    } else {
                        val time: Long = secondWord.positive shl 14 + firstWord ushr 18
                        val amp: Short = (firstWord and 0x0000FFFF).toShort()
                        eventsBuilder.addTimes(time * 8)
                        eventsBuilder.addAmplitudes(amp.toLong())
                    }
                }
            } catch (ex: Exception) {
                if (ex !is CancellationException) {
                    logger.error("Exception raised during packet gathering", ex)
                }
            }
        }
        //Stopping acquisition just in case
        writeRegister(0, ACQUISITION_STATUS.code, 0x00000001, 0x00000000)

        val meta = buildMeta {
            boards.first().meta?.let {
                putNode("dpp", it)
            }
        }
        val proto = builder.build()

        return ProtoNumassPoint(meta) { proto }
    }

    companion object {
        const val STATISTIC_HEADER: Int = 0xC0000000.toInt()

        val PACKET_HEADER_START_BYTES = arrayOf(0xAA, 0xEE)

        enum class Register(val code: Int) {
            FIRMWARE_VERSION(0),
            DPP_REGISTER_1(1),
            DPP_REGISTER_2(2),
            DPP_CONFIG_COMMIT_OFFSET(3),
            ACQUISITION_STATUS(4),
            ACQUISITION_TIME(5),
            ELAPSED_TIME(6),
            ACQUISITION_SETTINGS(7),
            WAVEFORM_LENGTH(8),
            MAP_POINTS(9),
            TIME_PER_MAP_POINT(10),
            ETH_CONFIGURATION_DATA(11),
            ETHERNET_COMMIT(13),
            CALIB_DONE_SIGNALS(14)
        }

        enum class CommandType(val byte: Byte) {
            READ(0xD0.toByte()),
            WRITE(0xD1.toByte()),
            SINGLE_SPECTRUM_MODE(0xD2.toByte()),
            MAP_MODE(0xD6.toByte()),
            LIST_MODE(0xD4.toByte()),
            WAVEFORM_MODE(0xD3.toByte()),
        }

        enum class AcquisitionMode(val byte: Byte) {
            SINGLE_SPECTRUM_MODE(2),
            MAP_MODE(6),
            LIST_MODE(4),
            WAVEFORM_MODE(3);

            val long = byte.toLong()
        }


        /**
         * Build command header
         */
        fun buildHeader(command: CommandType, board: Byte, packet: Byte, start: Byte, length: Byte): ByteArray {
            assert(command in listOf(CommandType.READ, CommandType.WRITE))
            assert(board in 0..255)
            assert(packet in 0..255)
            assert(length in 0..255)
            val header = ByteArray(8)
            header[0] = PACKET_HEADER_START_BYTES[0].toByte()
            header[1] = PACKET_HEADER_START_BYTES[1].toByte()
            header[2] = command.byte
            header[3] = board
            header[4] = packet
            header[5] = start
            header[6] = length
            return header
        }

        /**
         * Escape the sequence using DANTE convention
         */
        private fun ByteArray.escape(): ByteArray {
            return buildSequence {
                this@escape.forEach {
                    yield(it)
                    if (it == 0xdd.toByte()) {
                        yield(it)
                    }
                }
            }.toList().toByteArray()
        }


        /**
         * Create DANTE command and stuff it.
         * @param length size of data array/4
         */
        fun wrapCommand(command: CommandType, board: Int, packet: Int, start: Int, length: Int, data: ByteArray): ByteArray {
            if (command == CommandType.READ) {
                assert(data.isEmpty())
            } else {
                assert(data.size % 4 == 0)
                assert(length == data.size / 4)
            }

            val header = buildHeader(command, board.byte, packet.byte, start.byte, length.byte)

            val res = (header + data).escape()
            return byteArrayOf(0xdd.toByte(), 0xaa.toByte()) + res + byteArrayOf(0xdd.toByte(), 0x55.toByte())
        }

        data class DanteMessage(val command: CommandType, val board: Int, val packet: Int, val payload: ByteArray) {
            override fun toString(): String {
                return "${command.name}[$board, $packet] of size ${payload.size}"
            }
        }
    }

}