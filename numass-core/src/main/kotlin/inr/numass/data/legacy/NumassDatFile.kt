package inr.numass.data.legacy

import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder
import inr.numass.data.api.*
import inr.numass.data.api.NumassPoint.Companion.HV_KEY
import org.apache.commons.io.FilenameUtils
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.SeekableByteChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption.READ
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.stream.Stream

/**
 * Created by darksnake on 08.07.2017.
 */
class NumassDatFile @Throws(IOException::class)
constructor(override val name: String, private val path: Path, meta: Meta) : NumassSet {
    override val meta: Meta

    private val hVdev: Double
        get() = meta.getDouble("dat.hvDev", 2.468555393226049)

    //TODO check point start
    override val points: Stream<NumassPoint>
        get() = try {
            Files.newByteChannel(path, READ).use { channel ->
                var lab: Int
                val points = ArrayList<NumassPoint>()
                do {
                    points.add(readPoint(channel))
                    lab = readBlock(channel, 1).get().toInt()
                } while (lab != 0xff)
                return points.stream()
            }
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        }

    @Throws(IOException::class)
    constructor(path: Path, meta: Meta) : this(FilenameUtils.getBaseName(path.fileName.toString()), path, meta) {
    }

    init {
        val head = readHead(path)//2048
        this.meta = MetaBuilder(meta)
                .setValue("info", head)
                .setValue(NumassPoint.START_TIME_KEY, readDate(head))
                .build()
    }

    private fun hasUset(): Boolean {
        return meta.getBoolean("dat.uSet", true)
    }

    @Throws(IOException::class)
    private fun readHead(path: Path): String {
        Files.newByteChannel(path, READ).use { channel ->
            channel.position(0)
            val buffer = ByteBuffer.allocate(2048)
            channel.read(buffer)
            return String(buffer.array()).replace("\u0000".toRegex(), "")
        }
    }

    /**
     * Read the block at current position
     *
     * @param channel
     * @param length
     * @return
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun readBlock(channel: SeekableByteChannel, length: Int): ByteBuffer {
        val res = ByteBuffer.allocate(length)
        channel.read(res)
        res.order(ByteOrder.LITTLE_ENDIAN)
        res.flip()
        return res
    }

    /**
     * Read the point at current position
     *
     * @param channel
     * @return
     * @throws IOException
     */
    @Synchronized
    @Throws(IOException::class)
    private fun readPoint(channel: SeekableByteChannel): NumassPoint {

        val rx = readBlock(channel, 32)

        val voltage = rx.int

        var length = rx.short//(short) (rx[6] + 256 * rx[7]);
        val phoneFlag = rx.get(19).toInt() != 0//(rx[19] != 0);


        var timeDiv: Double = when (length.toInt()) {
            5, 10 -> 2e7
            15, 20 -> 1e7
            50 -> 5e6
            100 -> 2.5e6
            200 -> 1.25e6
            else -> throw IOException("Unknown time divider in input data")
        }

        if (phoneFlag) {
            timeDiv /= 20.0
            length = (length * 20).toShort()
        }

        val events = ArrayList<Pair<Short, Long>>()
        var lab = readBlock(channel, 1).get().toInt()

        while (lab == 0xBF) {
            val buffer = readBlock(channel, 5)
            lab = buffer.get(4).toInt()
        }

        do {
            events.add(readEvent(channel, lab, timeDiv))
            lab = readBlock(channel, 1).get().toInt()
        } while (lab != 0xAF)

        //point end
        val ending = readBlock(channel, 64)

        val hours = ending.get(37).toInt()
        val minutes = ending.get(38).toInt()

        val start = LocalDateTime.from(startTime)
        var absoluteTime = start.withHour(hours).withMinute(minutes)

        //проверяем, не проскочили ли мы полночь
        if (absoluteTime.isBefore(start)) {
            absoluteTime = absoluteTime.plusDays(1)
        }


        val uRead = ending.getInt(39)

        val uSet: Double
        uSet = if (!this.hasUset()) {
            uRead.toDouble() / 10.0 / hVdev
        } else {
            voltage / 10.0
        }

        val block = SimpleBlock(absoluteTime.toInstant(ZoneOffset.UTC), Duration.ofSeconds(length.toLong())) { parent ->
            events.map { it.adopt(parent) }
        }

        val pointMeta = MetaBuilder("point")
                .setValue(HV_KEY, uSet)
                .setValue("uRead", uRead.toDouble() / 10.0 / hVdev)
                .setValue("source", "legacy")


        return SimpleNumassPoint(listOf<NumassBlock>(block), pointMeta)
    }

    @Throws(IOException::class)
    private fun readDate(head: String): LocalDateTime {
        // Должны считать 14 символов
        val sc = Scanner(head)
        sc.nextLine()
        val dateStr = sc.nextLine().trim { it <= ' ' }
        //DD.MM.YY HH:MM
        //12:35:16 19-11-2013
        val format = DateTimeFormatter.ofPattern("HH:mm:ss dd-MM-yyyy")

        return LocalDateTime.parse(dateStr, format)
    }

    @Throws(IOException::class)
    private fun readEvent(channel: SeekableByteChannel, b: Int, timeDiv: Double): OrphanNumassEvent {
        val chanel: Short
        val time: Long

        val hb = b and 0x0f
        val lab = b and 0xf0

        when (lab) {
            0x10 -> {
                chanel = readChanel(channel, hb)
                time = readTime(channel)
            }
            0x20 -> {
                chanel = 0
                time = readTime(channel)
            }
            0x40 -> {
                time = 0
                chanel = readChanel(channel, hb)
            }
            0x80 -> {
                time = 0
                chanel = 0
            }
            else -> throw IOException("Event head expected")
        }

        return Pair(chanel, (time / timeDiv).toLong())
    }

    @Throws(IOException::class)
    private fun readChanel(channel: SeekableByteChannel, hb: Int): Short {
        assert(hb < 127)
        val buffer = readBlock(channel, 1)
        return (buffer.get() + 256 * hb).toShort()
    }

    @Throws(IOException::class)
    private fun readTime(channel: SeekableByteChannel): Long {
        val rx = readBlock(channel, 4)
        return rx.long//rx[0] + 256 * rx[1] + 65536 * rx[2] + 256 * 65536 * rx[3];
    }

    //    private void skip(int length) throws IOException {
    //        long n = stream.skip(length);
    //        if (n != length) {
    //            stream.skip(length - n);
    //        }
    //    }
}
