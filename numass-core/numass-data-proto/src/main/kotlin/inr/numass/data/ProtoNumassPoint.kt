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

package inr.numass.data

import hep.dataforge.io.envelopes.Envelope
import hep.dataforge.meta.Meta
import inr.numass.data.api.*
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.util.stream.IntStream
import java.util.stream.Stream
import java.util.zip.Inflater

/**
 * Protobuf based numass point
 * Created by darksnake on 09.07.2017.
 */
class ProtoNumassPoint(override val meta: Meta, val protoBuilder: () -> NumassProto.Point) : NumassPoint {

    val proto: NumassProto.Point
        get() = protoBuilder()

    override val blocks: List<NumassBlock>
        get() = proto.channelsList
            .flatMap { channel ->
                channel.blocksList
                    .map { block -> ProtoBlock(channel.id.toInt(), block, this) }
                    .sortedBy { it.startTime }
            }

    override val channels: Map<Int, NumassBlock>
        get() = proto.channelsList.groupBy { it.id.toInt() }.mapValues { entry ->
            MetaBlock(entry.value.flatMap { it.blocksList }.map { ProtoBlock(entry.key, it, this) })
        }

    override val voltage: Double = meta.getDouble("external_meta.HV1_value", super.voltage)

    override val index: Int = meta.getInt("external_meta.point_index", super.index)

    override val startTime: Instant
        get() = if (meta.hasValue("start_time")) {
            meta.getValue("start_time").time
        } else {
            super.startTime
        }

    override val length: Duration
        get() = if (meta.hasValue("acquisition_time")) {
            Duration.ofMillis((meta.getDouble("acquisition_time") * 1000).toLong())
        } else {
            super.length
        }

    companion object {
        fun readFile(path: Path): ProtoNumassPoint {
            return fromEnvelope(NumassFileEnvelope(path))
        }


        /**
         * Get valid data stream utilizing compression if it is present
         */
        private fun Envelope.dataStream(): InputStream = if (this.meta.getString("compression", "none") == "zlib") {
            //TODO move to new type of data
            val inflatter = Inflater()
            val array: ByteArray = with(data.buffer) {
                if (hasArray()) {
                    array()
                } else {
                    ByteArray(this.limit()).also {
                        this.position(0)
                        get(it)
                    }
                }
            }
            inflatter.setInput(array)
            val bos = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (!inflatter.finished()) {
                val size = inflatter.inflate(buffer)
                bos.write(buffer, 0, size)
            }
            val unzippeddata = bos.toByteArray()
            inflatter.end()
            ByteArrayInputStream(unzippeddata)
        } else {
            this.data.stream
        }

        fun fromEnvelope(envelope: Envelope): ProtoNumassPoint = ProtoNumassPoint(envelope.meta) {
            envelope.dataStream().use {
                NumassProto.Point.parseFrom(it)
            }
        }

//        fun readFile(path: String, context: Context = Global): ProtoNumassPoint {
//            return readFile(context.getFile(path).absolutePath)
//        }

        fun ofEpochNanos(nanos: Long): Instant {
            val seconds = Math.floorDiv(nanos, 1e9.toInt().toLong())
            val reminder = (nanos % 1e9).toInt()
            return Instant.ofEpochSecond(seconds, reminder.toLong())
        }
    }
}

class ProtoBlock(
    override val channel: Int,
    private val block: NumassProto.Point.Channel.Block,
    val parent: NumassPoint? = null
) : NumassBlock {

    override val startTime: Instant
        get() = ProtoNumassPoint.ofEpochNanos(block.time)

    override val length: Duration = when {
        block.length > 0 -> Duration.ofNanos(block.length)
        parent?.meta?.hasValue("acquisition_time") ?: false ->
            Duration.ofMillis((parent!!.meta.getDouble("acquisition_time") * 1000).toLong())
        parent?.meta?.hasValue("params.b_size") ?: false ->
            Duration.ofNanos((parent!!.meta.getDouble("params.b_size") * 320).toLong())
        else -> {
            error("No length information on block")
//            LoggerFactory.getLogger(javaClass).warn("No length information on block. Trying to infer from first and last events")
//            val times = events.map { it.timeOffset }.toList()
//            val nanos = (times.max()!! - times.min()!!)
//            Duration.ofNanos(nanos)
//            Duration.ofMillis(380)
        }
    }

    override val events: Stream<NumassEvent>
        get() = if (block.hasEvents()) {
            val events = block.events
            if (events.timesCount != events.amplitudesCount) {
                LoggerFactory.getLogger(javaClass)
                    .error("The block is broken. Number of times is ${events.timesCount} and number of amplitudes is ${events.amplitudesCount}")
            }
            IntStream.range(0, events.timesCount)
                .mapToObj { i -> NumassEvent(events.getAmplitudes(i).toUShort(), events.getTimes(i), this) }
        } else {
            Stream.empty()
        }


    override val frames: Stream<NumassFrame>
        get() {
            val tickSize = Duration.ofNanos(block.binSize)
            return block.framesList.stream().map { frame ->
                val time = startTime.plusNanos(frame.time)
                val data = frame.data.asReadOnlyByteBuffer()
                NumassFrame(time, tickSize, data.asShortBuffer())
            }
        }
}