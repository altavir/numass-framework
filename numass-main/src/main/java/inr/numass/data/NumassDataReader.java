/* 
 * Copyright 2015 Alexander Nozik.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package inr.numass.data;

import hep.dataforge.data.FileDataFactory;
import hep.dataforge.data.binary.Binary;
import hep.dataforge.meta.Meta;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

/**
 *
 * @author Darksnake, based on program by S.V.Zadorozhny, 1996
 */
public class NumassDataReader {

    private String name;
    private final InputStream stream;
    private double HVdev = 2.468555393226049;
    private boolean noUset = false;

    public NumassDataReader(Binary file, Meta config) throws IOException {
        this(file.getStream(), config.getString(FileDataFactory.FILE_NAME_KEY), config);
    }
    
    public NumassDataReader(File file) throws IOException {
        this(new FileInputStream(file), file.getName(), Meta.empty());
    }    

    public NumassDataReader(String file, String fname, Meta config) throws FileNotFoundException {
        this(new FileInputStream(file), fname, config);
        if ((fname == null) || (fname.isEmpty())) {
            name = file;
        }
    }

    public NumassDataReader(InputStream is, String fname, Meta config) {
        this.stream = new BufferedInputStream(is);
        this.name = fname;
        HVdev = config.getDouble("HVdev", 2.468555393226049);
        noUset = config.getBoolean("noUset", false);
    }

    public RawNMFile read() throws IOException {
        return readFile(name);
    }

    private int[] readBlock(int length) throws IOException {
        int[] res = new int[length];
        for (int i = 0; i < res.length; i++) {
            res[i] = readByte();
        }
        return res;
    }

    private int readByte() throws IOException {
        return stream.read();
    }

    private short readChanel(int hb) throws IOException {
        assert hb < 127;
        return (short) (readByte() + 256 * hb);
    }

    private LocalDateTime readDate(String head) throws IOException {
        // Должны считать 14 символов
        Scanner sc = new Scanner(head);
        sc.nextLine();
        String dateStr = sc.nextLine().trim();
        //DD.MM.YY HH:MM
        //12:35:16 19-11-2013
        DateTimeFormatter format = DateTimeFormatter.ofPattern("HH:mm:ss dd-MM-yyyy");

        LocalDateTime date = LocalDateTime.parse(dateStr, format);

        return date;
    }

    private NMEvent readEvent(int b, double timeDiv) throws IOException {
        short chanel;
        long time;

        int hb = (b & 0x0f);
        int lab = (b & 0xf0);

        switch (lab) {
            case 0x10:
                chanel = readChanel(hb);
                time = readTime();
                break;
            case 0x20:
                chanel = 0;
                time = readTime();
                break;
            case 0x40:
                time = 0;
                chanel = readChanel(hb);
                break;
            case 0x80:
                time = 0;
                chanel = 0;
                break;
            default:
                throw new IOException("Event head expected");
        }

        return new NMEvent(chanel, time / timeDiv);
    }

    private RawNMFile readFile(String name) throws IOException {

        RawNMFile file = new RawNMFile(name);
        String head = readHead();//2048
        file.setHead(head.replaceAll("\u0000", ""));

        LocalDateTime filedate = readDate(head);

        int lab = readByte();
        do {
            RawNMPoint point = readPoint(lab, filedate);
            //Устанавливаем дату точки
            file.putPoint(point);
            lab = readByte();
        } while (lab != 0xff);

        //Check if input file head is numass data file
        return file;
    }

    private String readHead() throws IOException {
        byte[] bytes = new byte[2048];
        stream.read(bytes);
        return new String(bytes);
    }

    private RawNMPoint readPoint(int head, LocalDateTime filedate) throws IOException {

        int[] rx;

        int voltage;
        short time_out;
        double timeDiv = 0;
        boolean phoneFlag;

        RawNMPoint point = new RawNMPoint();

        int lab = head;

        //point head
        if (lab != 0x0f) {
            //Ожидается, что это голова точки
            throw new IOException("Point head expected");
        }

        rx = readBlock(32);

        voltage = rx[2] + 256 * rx[3];
        voltage = 65536 * voltage + rx[0] + 256 * rx[1];

        time_out = (short) (rx[6] + 256 * rx[7]);
        phoneFlag = (rx[19] != 0);

        switch (time_out) {
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
            time_out *= 20;
        }

        lab = readByte();
        while (lab == 0xBF) {
            skip(4);//badHV
            lab = readByte();
        }
        do {
            point.putEvent(readEvent(lab, timeDiv));
            lab = readByte();
        } while (lab != 0xAF);

        //point end
        skip(37);

        int hours = readByte();
        int minutes = readByte();

        LocalDateTime absoluteTime = filedate.withHour(hours).withMinute(minutes);

        //проверяем, не проскочили ли мы полночь
        if (absoluteTime.isBefore(filedate)) {
            absoluteTime = absoluteTime.plusDays(1);
        }

        point.setStartTime(absoluteTime.toInstant(ZoneOffset.UTC));

        rx = readBlock(4);
        int Uread = rx[2] + 256 * rx[3];
        Uread = 65536 * Uread + rx[0] + 256 * rx[1];

        skip(21);

        point.setLength(time_out);
        point.setUread(Uread / 10d / HVdev);
        if (noUset) {
            point.setUset(Uread / 10d / HVdev);
        } else {
            point.setUset(voltage / 10d);
        }

        return point;
    }

    private long readTime() throws IOException {
        int[] rx = readBlock(4);
        long time = rx[0] + 256 * rx[1] + 65536 * rx[2] + 256 * 65536 * rx[3];
        return time;
    }

    private void skip(int length) throws IOException {
        long n = stream.skip(length);
        if (n != length) {
            stream.skip(length - n);
        }
    }
}

/*
 PROGRAM DAT2PAW;

 {$N+}

 {$D Copyright (C) 1996 by S.V.Zadorozhny, INR RAS}

 Uses Dos, Crt;

 CONST
 Rec_Len = 32768;  { Buffer size }

 TYPE
 Dim   = ARRAY [1..Rec_Len] OF Byte;      { Read / Write Buffer  }

 VAR
 DATA : WORD;
 TIME : LONGINT;
 TD : Double;
 i, j, k : WORD;
 Voltage, li, CN : LONGINT;
 St, St1 : String;
 f : File;
 txt : Text;
 Buf : ^Dim;
 Buf_point : Word;
 err : INTEGER;
 Monitor, Pressure : WORD;

 paths : PathStr;
 dirs : DirStr;
 nams : NameStr;
 exts : ExtStr;
 name, mask : String;
 sr : SearchRec;

 rx : Array [1..64] OF Byte;
 lab, bt : Byte;
 time_out : Word;
 hb : Word;

 {Gate_L, Gate_H : Word;}

 TimDiv : Double;
 Phone_Flag, OK : Boolean;

 live : word;

 (*----------------------------- PROCEDURES ----------------------------------*)
 PROCEDURE NextChar;
 BEGIN
 CASE (live) OF
 0 : BEGIN
 Write('|');
 Inc(live);
 END;
 1 : BEGIN
 Write(#8'/');
 Inc(live);
 END;
 2 : BEGIN
 Write(#8'--');
 Inc(live);
 END;
 3 : BEGIN
 Write(#8#8'\ ');
 Inc(live);
 END;
 4 : BEGIN
 Write(#8#8'|');
 live := 1;
 END;
 END;
 END;


 PROCEDURE ReadB(VAR bt : BYTE);
 VAR fact : INTEGER;
 BEGIN
 IF Buf_point = 0 THEN
 BEGIN
 BlockRead(f,Buf^,Rec_Len,fact);
 NextChar;
 END;
 Inc(Buf_point);
 bt := Buf^[Buf_point];
 IF Buf_point = Rec_Len THEN Buf_point:=0;
 END  { ReadB };

 PROCEDURE SaveTXT;
 BEGIN
 Writeln(txt, CN, #9, TD:10:5, #9, DATA, #9, time_out, #9, (voltage/10):7:1);
 END;

 PROCEDURE ReadFile;
 BEGIN
 Assign(f,name);
 {$I-}
 Reset(f,1);
 {$I+}
 IF (IOResult <> 0) THEN
 BEGIN
 Writeln('Can''t open data file ', name);
 Halt(3);
 END;

 BlockRead(f,Buf^,2048,i);     {Skip First Block}
 Buf_point := 0;

 IF ((Buf^[1] <> Ord('N'))OR(Buf^[2] <> Ord('M'))) THEN
 BEGIN
 Close(f);
 Writeln('File ', name, ' is not a data file !');
 Exit;
 END;

 Write('File : ',name, '   ');

 paths := name;
 Fsplit(paths, dirs, nams, exts);
 Assign(txt, nams + '.paw');
 Rewrite(txt);

 live := 0;
 CN := 0;

 REPEAT
 ReadB(lab);
 hb  := lab AND $0F;     { High data byte }
 lab := lab AND $F0;
 CASE (lab) OF
 $00 :         { New Point }
 BEGIN
 FOR i:=1 TO 32 DO ReadB(rx[i]);
 Voltage := rx[3] + 256 * Word(rx[4]);
 Voltage := 65536 * Voltage + rx[1] + 256 * Longint(rx[2]);
 time_out := rx[7] + 256 * Word(rx[8]);
 {pressure := rx[11] + 256 * Word(rx[12]);}
 {Chan := rx[19];}
 IF (rx[20] <> 0) THEN Phone_Flag := TRUE ELSE Phone_Flag := FALSE;
 CASE (time_out) OF
 5, 10 : TimDiv := 20000000.0;
 15, 20 : TimDiv := 10000000.0;
 50 : TimDiv :=  5000000.0;
 100 : TimDiv :=  2500000.0;
 200 : TimDiv :=  1250000.0;
 END;
 IF (Phone_Flag) THEN
 BEGIN
 TimDiv := TimDiv / 20.0;
 time_out := time_out * 20;
 END;
 END;

 $10 :         { Event, all present }
 BEGIN
 FOR i:=1 TO 5 DO ReadB(rx[i]);
 TIME := rx[2] + 256 * Longint(rx[3])
 + 65536 * Longint(rx[4])+ 256 * 65536 * Longint(rx[5]);
 TD := TIME / TimDiv;
 DATA := rx[1] + 256 * hb;
 Inc(CN);
 SaveTXT;
 END;

 $20 :         { Event, time only }
 BEGIN
 FOR i:=1 TO 4 DO ReadB(rx[i]);
 TIME := rx[1] + 256 * Longint(rx[2])
 + 65536 * Longint(rx[3])+ 256 * 65536 * Longint(rx[4]);
 TD := TIME / TimDiv;
 DATA := 0;
 Inc(CN);
 SaveTXT;
 END;

 $40 :         { Event, code only }
 BEGIN
 ReadB(rx[1]);
 DATA := rx[1] + 256 * hb;
 TD := 0.0;
 Inc(CN);
 SaveTXT;
 END;

 $80 :         { Event, nothing }
 BEGIN
 DATA := 0;
 TD := 0.0;
 Inc(CN);
 SaveTXT;
 END;

 $B0 :         { Bad HV }
 BEGIN
 FOR i:=1 TO 4 DO ReadB(rx[i]);
 END;

 $A0 :         { End of the point }
 BEGIN
 FOR i := 1 TO 64 DO ReadB(rx[i]);
 END;
 END;

 UNTIL (lab=$F0);
 Close(f);
 Close(txt);
 Writeln(#8#8'  ');
 END;


 PROCEDURE ReadDataFiles;
 BEGIN
 mask := ParamStr(1);
 FindFirst(mask, anyfile, sr);
 WHILE (DosError = 0) DO
 BEGIN
 name := sr.Name;
 ReadFile;
 FindNext(sr);
 END
 END;

 (*--------------------------- Main ------------------------------------------*)

 BEGIN
 ClrScr;

 Writeln('TROITSK V-MASS EXPERIMENT DATA FILE -> TEXT FILE CONVERTER');
 Writeln('WRITTEN BY S.V.ZADOROZHNY');
 Writeln;


 IF (ParamCount <> 1) THEN
 BEGIN
 Writeln('Usage : dat2paw <mask.dat>');
 Writeln;
 Writeln('--- Press any key to continue ---');
 ReadKey;
 Halt(1);
 END;

 New(Buf);

 ReadDataFiles;

 Writeln;
 Writeln('O.K.');

 Dispose(Buf);
 END.                       {-- Main --}

 */
