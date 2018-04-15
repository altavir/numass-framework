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

import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.produce
import java.io.InputStream
import kotlin.coroutines.experimental.buildSequence


object Communications {

    val PACKET_HEADER_START_BYTES = arrayOf(0xAA, 0xEE)

    private val Byte.positive
        get() = toInt() and 0xFF

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
        WAVEFORM_MODE(3)
    }


    /**
     * Build command header
     */
    fun buildHeader(command: CommandType, board: Byte, packet: Byte, start: Register, length: Byte): ByteArray {
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
        header[5] = start.code.toByte()
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
     */
    fun wrapCommand(command: CommandType, board: Byte, packet: Byte, start: Register, data: ByteArray): ByteArray {
        when (command) {
            CommandType.READ -> assert(data.isEmpty())
            CommandType.WRITE -> assert(data.size % 4 == 0)
            else -> throw RuntimeException("Command $command not expected")
        }

        val length: Byte = (data.size / 4).toByte()
        val header = buildHeader(command, board, packet, start, length)

        val res = (header + data).escape()
        return byteArrayOf(0xdd.toByte(), 0xaa.toByte()) + res + byteArrayOf(0xdd.toByte(), 0x55.toByte())
    }

    data class DanteMessage(val command: CommandType, val board: Byte, val packet: Byte, val payload: ByteArray) {
        override fun toString(): String {
            return "${command.name}[$board, $packet]: ${payload.size}"
        }
    }

    /**
     * Read the stream and return resulting messages in ReceiveChannel
     */
    fun readStream(stream: InputStream, parent: Job): ReceiveChannel<DanteMessage> {
        return produce(capacity = Channel.UNLIMITED, parent = parent) {
            while (true) {
                val first = stream.read()
                if (stream.read() == PACKET_HEADER_START_BYTES[0] && stream.read() == PACKET_HEADER_START_BYTES[1]) {
                    // second check is not executed unless first one is false
                    val header = ByteArray(6)
                    stream.read(header)
                    val command = CommandType.values().find { it.byte == header[0] }!!
                    val board = header[1]
                    val packet = header[2]
                    val length = header[3].positive * 0x100 + header[4].positive * 0x010 + header[5].positive
                    val payload = ByteArray(length)
                    stream.read(payload)
                    send(DanteMessage(command, board, packet, payload))
                }
            }
        }
    }
}
/*

def extract_response(stream):
    r"""Extract response messages from stream.

    Method will rstrip junk bytes before response (assuming it could be only
    \x00 values), extract every complete message and cut them from stream.
    - if stream contains uncomlete message, method leaves it untouched.

    Returns croped stream and extracted messages. Message structure:
    {
        packet_num - request packet id
        command - command or response code
        payload - payload binary data
        board - board number
    }
    """
    messages = []
    while True:
        idx = stream.find(__PACKET_HEADER_START_BYTES)
        if idx != -1:
            stream = stream[idx:]
            payload_size = struct.unpack('>I', b'\x00%s' % (stream[5:8]))[0]
            if len(stream) >= payload_size + 8:
                messages.append({
                    'command': stream[2],
                    'board': stream[3],
                    'packet_num': stream[4],
                    'payload': stream[8: payload_size * 4 + 8]
                })
                stream = stream[payload_size + 8:]
            else:
                return stream, messages
        else:
            return stream, messages




def create_response(
        command, board_num=0, packet_num=0, data=None):
    """Create response packet.

    For test purposes only!
    """
    assert data is None or isinstance(data, (bytes, bytearray))
    assert 0 <= board_num <= 255
    assert 0 <= packet_num <= 255

    if not data:
        data = b''

    header = bytearray(8)
    header[0:2] = __PACKET_HEADER_START_BYTES
    header[2] = command[0]
    header[3] = board_num
    header[4] = packet_num
    header[5:8] = struct.pack('>I', len(data))[1:]

    return b'%s%s' % (header, data)


def extract_request(stream):
    """Extract stuffed requests from stream.

    For test purposes only!
    """
    messages = []
    while True:
        start_idx = stream.find(b'\xdd\xaa')
        end_idx = stream.find(b'\xdd\x55')
        if start_idx != -1 and end_idx != -1 and end_idx > start_idx:
            packet = stream[start_idx + 2: end_idx]
            messages.append({
                'command': packet[2],
                'board': packet[3],
                'packet_num': packet[4],
                'start_addr': packet[5],
                'length': packet[6],
                'payload': packet[8:],
            })
            stream = stream[end_idx + 2:]
        else:
            return stream, messages



 */