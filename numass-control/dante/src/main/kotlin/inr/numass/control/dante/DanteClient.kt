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

import inr.numass.control.dante.Communications.CommandType.*
import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import org.slf4j.LoggerFactory
import java.io.OutputStream
import java.net.Socket
import java.util.concurrent.atomic.AtomicLong

//TODO implement using Device
class DanteClient(val ip: String) : AutoCloseable {
    private val RESET_COMMAND = arrayOf(0xDD, 0x55, 0xDD, 0xEE)

    private val logger = LoggerFactory.getLogger(javaClass)

    private val packetNumber = AtomicLong(0)

    private var parentJob = Job()

    private val connections: MutableMap<Int, Pair<Socket, Job>> = HashMap()

    private val commandChannel = Channel<ByteArray>(capacity = Channel.UNLIMITED)
    private lateinit var output: OutputStream
    private lateinit var outputJob: Job


    fun open() {
        (0..3).forEach {
            openPort(it)
        }
    }

    private fun openPort(port: Int) {
        //closing existing connection
        connections[port]?.let {
            it.first.close()
            it.second.cancel()
        }

        val socket = Socket(ip, 8000 + port)

        //Create command queue on port 0
        if (port == 0) {
            outputJob.cancel()
            output = socket.getOutputStream()
            outputJob = launch(parent = parentJob) {
                output.write(commandChannel.receive())
            }
        }

        val channel = Communications.readStream(socket.getInputStream(), parentJob)

        val job = launch {
            while (true) {
                handle(channel.receive())
                //TODO handle errors and reconnect
            }
        }

        connections[port] = Pair(socket, job)
    }

    fun send(command: ByteArray) {
        runBlocking {
            commandChannel.send(command)
        }
    }

    fun send(command: Communications.CommandType, board: Byte, packet: Byte, start: Communications.Register, data: ByteArray) {
        send(Communications.wrapCommand(command, board, packet, start, data))
    }

    suspend fun handle(response: Communications.DanteMessage) {
        logger.debug("Received {}", response.toString())
        when (response.command) {
            READ -> TODO()
            WRITE -> TODO()
            SINGLE_SPECTRUM_MODE -> TODO()
            MAP_MODE -> TODO()
            LIST_MODE -> TODO()
            WAVEFORM_MODE -> TODO()
        }
    }

    /*
        async def __write_config(self, board, register, data, mask=None):
        assert isinstance(data, (bytes, bytearray))
        data_len = len(data)
        if mask:
            assert isinstance(mask, (bytes, bytearray))
            assert len(mask) == data_len
            resp = await self.__send_message(
                COMMANDS['READ'], board, register, data_len // 4)

            data_bin = bitarray(endian='big')
            data_bin.frombytes(data)

            resp_payload = bitarray(endian='big')
            resp_payload.frombytes(resp['payload'])

            mask_bin = bitarray(endian='big')
            mask_bin.frombytes(mask)

            data_masked = data_bin & mask_bin

            mask_bin.invert()

            data_out = (data_masked | (resp_payload & mask_bin)).tobytes()
        else:
            data_out = data

        return await self.__send_message(
            COMMANDS['WRITE'], board, register, data_len // 4, data_out)
     */

    private fun writeParameter(board: Byte, register: Byte, data: ByteArray) {
        val packet = packetNumber.incrementAndGet() % 256
                send(WRITE, board, register, data)
    }


    override fun close() {
        //TODO send termination signal
        connections.values.forEach {
            it.first.close()
            it.second.cancel()
        }
        parentJob.cancel(CancellationException("Server stopped"))
    }

    fun reconnect() {
        close()
        //create a new parent job
        parentJob = Job()
        open()
    }
}
/*
    def __make_binary_base():
        point = dfparser.Point()
        for board in BOARDS:
            channel = point.channels.add(id=board)
            channel.blocks.add()
        return point

    async def __write_dpp(
            self, board: int, register: int, value: bytes):
        await self.__write_config(
            board, ORDER['DPP_REGISTER_1'], struct.pack('>I', register))
        await self.__write_config(board, ORDER['DPP_REGISTER_2'], value)
        await self.__write_config(
            board, ORDER['DPP_CONFIG_COMMIT_OFFSET'],
            b'\x00\x00\x00\x01', b'\x00\x00\x00\x01')

    async def __initialize_boards(self):
        dpp_params = SETTINGS['DANTE']['dpp']

        gain = np.double(dpp_params['gain'])
        det_thresh = np.uint32(dpp_params['detection_thresold'])
        pileup_thr = np.uint32(dpp_params['pileup_thresold'])
        en_fil_peak_time = np.uint32(
            dpp_params['energy_filter']['peaking_time'])
        en_fil_flattop = np.uint32(dpp_params['energy_filter']['flat_top'])
        fast_peak_time = np.uint32(dpp_params['fast_filter']['peaking_time'])
        fast_flattop = np.uint32(dpp_params['fast_filter']['flat_top'])
        recovery_time = np.uint32(dpp_params['recovery_time'])
        zero_peak_rate = np.uint32(dpp_params['zero_peak_rate'])
        inverted_input = np.uint32(dpp_params['inverted_input'])

        assert 1 <= en_fil_peak_time <= 511
        assert 0.01 <= gain <= en_fil_peak_time * 2 - 0.01
        assert 0 <= det_thresh <= 4096
        assert 0 <= pileup_thr <= 4096
        assert 1 <= en_fil_flattop <= 15
        assert 1 <= fast_peak_time <= 31
        assert 1 <= fast_flattop <= 31
        assert 0 <= recovery_time <= 2**24 - 1
        assert 0 <= zero_peak_rate <= 500
        assert inverted_input in [0, 1]
        assert (en_fil_peak_time + en_fil_flattop) * 2 < 1023

        for board in BOARDS:
            logger.info('start %s board initialisation', board)

            val_128 = struct.pack('>I', 0 + inverted_input * (1 << 24))
            await self.__write_dpp(board, 128, val_128)

            val_162 = struct.pack('>I', recovery_time)
            await self.__write_dpp(board, 162, val_162)

            await self.__write_dpp(board, 181, b'\x00\x00\x00\x00')

            await self.__write_dpp(board, 185, b'\x00\x00\x00\x00')

            await self.__write_dpp(board, 175, b'\x00\x00\x00\x01')

            val_160 = struct.pack('>I', int(pileup_thr / gain) + 1 * (1 << 31))
            await self.__write_dpp(board, 160, val_160)

            val_160 = struct.pack(
                '>I', int(pileup_thr / gain * 2) + 0 * (1 << 31))
            await self.__write_dpp(board, 160, val_160)

            val_152 = struct.pack(
                '>I', int(det_thresh / gain * fast_peak_time))
            await self.__write_dpp(board, 152, val_152)

            if fast_flattop == 1:
                await self.__write_dpp(board, 154, b'\x00\x00\x00\x00')
            else:
                val_154 = np.uint32(np.ceil(fast_flattop) / 2)
                await self.__write_dpp(board, 154, val_154)

            if zero_peak_rate == 0:
                await self.__write_dpp(board, 142, b'\x00\x00\x00\x00')
            else:
                val_142 = np.uint32(1 / zero_peak_rate / 10 * 1e6 + (1 << 31))
                await self.__write_dpp(board, 142, val_142)

            val_180 = struct.pack('>I', fast_flattop + 1)
            await self.__write_dpp(board, 180, val_180)

            if (2 * fast_peak_time + fast_flattop) > 4 * en_fil_flattop:
                val_140 = struct.pack('>I', np.uint32(np.ceil(
                    (2 * fast_peak_time + fast_flattop) * 4) + 1))
                await self.__write_dpp(board, 140, val_140)
            else:
                await self.__write_dpp(board, 140, en_fil_flattop)

            val_141 = struct.pack('>I', fast_peak_time + fast_flattop + 4)
            await self.__write_dpp(board, 141, val_141)

            val_156 = struct.pack('>I', fast_peak_time + 0 * (1 << 28))
            await self.__write_dpp(board, 156, val_156)

            val_150 = struct.pack('>I', fast_flattop + 0 * (1 << 28))
            await self.__write_dpp(board, 150, val_150)

            val_149 = struct.pack('>I', en_fil_peak_time + 1 * (1 << 28))
            await self.__write_dpp(board, 149, val_149)

            val_150 = struct.pack('>I', en_fil_flattop + 1 * (1 << 28))
            await self.__write_dpp(board, 150, val_150)

            val_153 = struct.pack(
                '>I',
                en_fil_peak_time * 2 + en_fil_flattop * 2 + 1 * (1 << 28))
            await self.__write_dpp(board, 153, val_153)

            val_184 = struct.pack(
                '>I', int(gain * (1 << 24) / en_fil_peak_time) + 1 * (1 << 28))
            await self.__write_dpp(board, 184, val_184)

            await self.__write_dpp(board, 148, struct.pack('>I', 1 + (1 << 1)))

            val_128 = struct.pack(
                '>I', 1 * (1 << 2) + inverted_input * (1 << 24))
            await self.__write_dpp(board, 128, val_128)

            val_128 = struct.pack('>I', 1 + inverted_input * (1 << 24))
            await self.__write_dpp(board, 128, val_128)

        self.__send_reply_ok()

    async def __start_acquisition(self, acq_time_ms, map_points=10):
        acq_time_ms = acq_time_ms
        logger.info('start point acquisition %s ms', acq_time_ms)
        for board in BOARDS:
            await self.__write_config(
                board, ORDER['ACQUISITION_SETTINGS'],
                b'\x00\x00\x00\x04', b'\x00\x00\x00\x07')
            await self.__write_config(
                board, ORDER['ACQUISITION_TIME'],
                struct.pack('>I', acq_time_ms))
            await self.__write_config(
                board, ORDER['MAP_POINTS'],
                struct.pack('>I', acq_time_ms // map_points))
            await self.__write_config(
                board, ORDER['TIME_PER_MAP_POINT'],
                struct.pack('>I', map_points))
        await self.__write_config(
            0, ORDER['ACQUISITION_STATUS'],
            b'\x00\x00\x00\x01', b'\x00\x00\x00\x01')

    def __get_packet_number(self):
        """Generate available packet number for board."""
        packet_num = ALL_PACKETS.difference(PACKET_NUMBERS).pop()
        PACKET_NUMBERS.add(packet_num)
        EVENTS[packet_num] = asyncio.Event()
        return packet_num

    def __put_packet_number(self, packet_num):
        """Release packet number to available."""
        PACKET_NUMBERS.remove(packet_num)
        del EVENTS[packet_num]

    async def __send_message(
            self, command, board_num=0, start_addr=0, length=0, data=b''):
        """Send message and wait for response."""
        msg_id = self.__get_packet_number()
        cmd = create_command(
            command, board_num, msg_id, start_addr, length, data)
        logger.debug(
            'Send %s %s %s %s', command[0], board_num, start_addr, length)
        await SEND_QUEUE.put(cmd)
        logger.debug('[%s, %s] waiting for response', board_num, msg_id)
        await EVENTS[msg_id].wait()
        resp = PACKET_DATA[msg_id]
        self.__put_packet_number(msg_id)
        logger.debug('[%s, %s] get response: %s', board_num, msg_id, resp)
        return resp



    async def __send_acquired_point(self):
        while self.recv_time is None or (datetime.now() - self.recv_time)\
                .total_seconds() < WAIT_TIME_S:
            await asyncio.sleep(SETTINGS['params']['print_count_s'])
            logger.info('%s events acquired', self.events_count)
        logger.info(
            "%s seconds elapsed since acq data. Dump point.",
            (datetime.now() - self.recv_time).total_seconds())

        end_time = self.recv_time.replace(microsecond=0).isoformat()
        data = self.point.SerializeToString()
        self.point_meta['binary_size'] = len(data)
        self.point_meta['end_time'] = end_time
        self.send_message(
            meta=self.point_meta,
            data=data)

 */