package inr.numass.data.legacy;

import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaBuilder;
import inr.numass.data.api.*;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Stream;

import static inr.numass.data.api.NumassPoint.HV_KEY;
import static java.nio.file.StandardOpenOption.READ;

/**
 * Created by darksnake on 08.07.2017.
 */
public class NumassDatFile implements NumassSet {
    private final String name;
    private final Path path;
    private final Meta meta;

    public NumassDatFile(Path path, Meta meta) throws IOException {
        this(FilenameUtils.getBaseName(path.getFileName().toString()),path,meta);
    }

    public NumassDatFile(String name, Path path, Meta meta) throws IOException {
        this.name = name;
        this.path = path;
        String head = readHead(path);//2048
        this.meta = new MetaBuilder(meta)
                .setValue("info", head)
                .setValue(NumassPoint.START_TIME_KEY, readDate(head))
                .build();
    }

    @Override
    public Meta meta() {
        return meta;
    }

    @Override
    public String getName() {
        return name;
    }

    private double getHVdev() {
        return meta().getDouble("dat.hvDev", 2.468555393226049);
    }

    private boolean hasUset() {
        return meta().getBoolean("dat.uSet", true);
    }

    private static String readHead(Path path) throws IOException {
        try (SeekableByteChannel channel = Files.newByteChannel(path, READ)) {
            channel.position(0);
            ByteBuffer buffer = ByteBuffer.allocate(2048);
            channel.read(buffer);
            return new String(buffer.array()).replaceAll("\u0000", "");
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
    private ByteBuffer readBlock(SeekableByteChannel channel, int length) throws IOException {
        ByteBuffer res = ByteBuffer.allocate(length);
        channel.read(res);
        res.order(ByteOrder.LITTLE_ENDIAN);
        res.flip();
        return res;
    }

    /**
     * Read the point at current position
     *
     * @param channel
     * @return
     * @throws IOException
     */
    private synchronized NumassPoint readPoint(SeekableByteChannel channel) throws
            IOException {

        ByteBuffer rx = readBlock(channel, 32);

        int voltage = rx.getInt();

        short length = rx.getShort();//(short) (rx[6] + 256 * rx[7]);
        boolean phoneFlag = rx.get(19) != 0;//(rx[19] != 0);


        double timeDiv;
        switch (length) {
            case 5:
            case 10:
                timeDiv = 2e7;
                break;
            case 15:
            case 20:
                timeDiv = 1e7;
                break;
            case 50:
                timeDiv = 5e6;
                break;
            case 100:
                timeDiv = 2.5e6;
                break;
            case 200:
                timeDiv = 1.25e6;
                break;
            default:
                throw new IOException("Unknown time divider in input data");
        }

        if (phoneFlag) {
            timeDiv /= 20.0;
            length *= 20;
        }

        List<NumassEvent> events = new ArrayList<>();
        int lab = readBlock(channel, 1).get();

        while (lab == 0xBF) {
            ByteBuffer buffer = readBlock(channel, 5);
            lab = buffer.get(4);
        }

        do {
            events.add(readEvent(channel, lab, timeDiv));
            lab = readBlock(channel, 1).get();
        } while (lab != 0xAF);

        //point end
        ByteBuffer ending = readBlock(channel, 64);

        int hours = ending.get(37);
        int minutes = ending.get(38);

        LocalDateTime start = LocalDateTime.from(getStartTime());
        LocalDateTime absoluteTime = start.withHour(hours).withMinute(minutes);

        //проверяем, не проскочили ли мы полночь
        if (absoluteTime.isBefore(start)) {
            absoluteTime = absoluteTime.plusDays(1);
        }


        int uRead = ending.getInt(39);

        double uSet;
        if (!this.hasUset()) {
            uSet = uRead / 10d / getHVdev();
        } else {
            uSet = voltage / 10d;
        }

        NumassBlock block = new SimpleBlock(absoluteTime.toInstant(ZoneOffset.UTC), Duration.ofSeconds(length), events);

        Meta pointMeta = new MetaBuilder("point")
                .setValue(HV_KEY, uSet)
                .setValue("uRead", uRead / 10 / getHVdev())
                .setValue("source", "legacy");


        return new SimpleNumassPoint(pointMeta, Collections.singletonList(block));
    }

    @Override
    public Stream<NumassPoint> getPoints() {
        try (SeekableByteChannel channel = Files.newByteChannel(path, READ)) {

            //int lab = readBlock(channel,1).get();
            int lab;
            List<NumassPoint> points = new ArrayList<>();
            do {
                //TODO check point start
                points.add(readPoint(channel));
                lab = readBlock(channel, 1).get();
            } while (lab != 0xff);
            return points.stream();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

    }

    private LocalDateTime readDate(String head) throws IOException {
        // Должны считать 14 символов
        Scanner sc = new Scanner(head);
        sc.nextLine();
        String dateStr = sc.nextLine().trim();
        //DD.MM.YY HH:MM
        //12:35:16 19-11-2013
        DateTimeFormatter format = DateTimeFormatter.ofPattern("HH:mm:ss dd-MM-yyyy");

        return LocalDateTime.parse(dateStr, format);
    }

    private NumassEvent readEvent(SeekableByteChannel channel, int b, double timeDiv) throws IOException {
        short chanel;
        long time;

        int hb = (b & 0x0f);
        int lab = (b & 0xf0);

        switch (lab) {
            case 0x10:
                chanel = readChanel(channel, hb);
                time = readTime(channel);
                break;
            case 0x20:
                chanel = 0;
                time = readTime(channel);
                break;
            case 0x40:
                time = 0;
                chanel = readChanel(channel, hb);
                break;
            case 0x80:
                time = 0;
                chanel = 0;
                break;
            default:
                throw new IOException("Event head expected");
        }

        return new NumassEvent(chanel, (long) (time / timeDiv));
    }

    private short readChanel(SeekableByteChannel channel, int hb) throws IOException {
        assert hb < 127;
        ByteBuffer buffer = readBlock(channel, 1);
        return (short) (buffer.get() + 256 * hb);
    }

    private long readTime(SeekableByteChannel channel) throws IOException {
        ByteBuffer rx = readBlock(channel, 4);
        return rx.getLong();//rx[0] + 256 * rx[1] + 65536 * rx[2] + 256 * 65536 * rx[3];
    }

//    private void skip(int length) throws IOException {
//        long n = stream.skip(length);
//        if (n != length) {
//            stream.skip(length - n);
//        }
//    }
}
