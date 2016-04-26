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
package inr.numass.readvac;

import hep.dataforge.tables.DataPoint;
import hep.dataforge.tables.MapPoint;
import hep.dataforge.meta.Meta;
import hep.dataforge.values.Value;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortTimeoutException;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Darksnake
 */
public class VACDeviceReader implements Iterator<DataPoint>, AutoCloseable {

//    private int timeout = 100;
    private final int maxChars = 50;
    private static final String CM32Query = "MES R PM 1\r\n";
    private static final String MQuery = ":010300000002FA\r\n";
    private static final String P1Query = "@253PR5?;FF";

    SerialPort p2Port;
    SerialPort p3Port;
    SerialPort p1Port;
    SerialPort pxPort;

    Lock p1Lock;
    Lock p2Lock;
    Lock p3Lock;
    Lock pxLock;
    
    Meta p1Config;
    Meta p2Config;
    Meta p3Config;
    Meta pxConfig;

    private boolean p1PowerOn = false;

    public VACDeviceReader(String p2, String p3, String p1, String px) throws SerialPortException {
        setupPorts(p2, p3, p1, px);
    }

    public VACDeviceReader(Meta serialConfig) throws SerialPortException {
        String p2 = serialConfig.getString("P2", null);
        String p3 = serialConfig.getString("P3", null);
        String p1 = serialConfig.getString("P1", null);
        String px = serialConfig.getString("Px", p1);
//        this.timeout = serialConfig.getInt("timeout", timeout);
        setupPorts(p2, p3, p1, px);
    }

    public boolean setP1PowerStateOn(boolean state) throws SerialPortException, ResponseParseException, P1ControlException {

        p1PowerOn = getP1PowerState();
        if (state == p1PowerOn) {
            //Возвращаем то, что есть
            return p1PowerOn;
        } else {

            if (state == true) {
//                String ans = talkMKS(p1Port, "@253ENC!OFF;FF");
//                if (!ans.equals("OFF")) {
//                    LoggerFactory.getLogger(getClass()).warn("The @253ENC!OFF;FF command is not working");
//                }
                String ans = talkMKS(p1Port, "@253FP!ON;FF");
                if (!ans.equals("ON")) {
                    throw new P1ControlException("Can't set P1 cathod power state");
                }
            } else {
                String ans = talkMKS(p1Port, "@253FP!OFF;FF");
                if (!ans.equals("OFF")) {
                    throw new P1ControlException("Can't set P1 cathod power state");
                }
            }

            p1PowerOn = getP1PowerState();
            return p1PowerOn;
        }
    }

    public boolean isP1Available() {
        try {
            return !talkMKS(p1Port, "@253T?;FF").isEmpty();
        } catch (SerialPortException | ResponseParseException ex) {
            return false;
        }
    }

    private String talkMKS(SerialPort port, String request) throws SerialPortException, ResponseParseException {
        try {
            p1Lock.lock();
            if (!port.isOpened()) {
                port.openPort();
            }
            port.purgePort(SerialPort.PURGE_RXCLEAR | SerialPort.PURGE_TXCLEAR);
            port.writeString(request);
            LoggerFactory.getLogger(port.getPortName()).info("send> " + request.trim());
            String answer = readPort(p1Port, ";FF", 100);

            LoggerFactory.getLogger(port.getPortName()).info("recieve> " + answer.trim());
            if (answer.isEmpty()) {
                throw new ResponseParseException(answer);
            }
            Matcher match = Pattern.compile("@253ACK(.*);FF").matcher(answer);
            if (match.matches()) {
                return match.group(1);
            } else {
                throw new ResponseParseException(answer);
            }
        } finally {
            p1Lock.unlock();
        }
    }

    public boolean getP1PowerState() throws SerialPortException, ResponseParseException {
        String answer = talkMKS(p1Port, "@253FP?;FF");
        return answer.equals("ON");
    }

    /**
     * Считываем строку из порта пока не найдем delimeter или не потратим
     * timeout времени. Если случается таймаут, то возвращается null
     *
     * @param port
     * @param delimeter
     * @return
     * @throws SerialPortException
     */
    private String readPort(SerialPort port, String delimeter, int timeout) throws SerialPortException {

        String res = new String();
        Instant start = Instant.now();
        port.purgePort(SerialPort.PURGE_RXCLEAR | SerialPort.PURGE_TXCLEAR);
        while (res.length() < maxChars) {

            try {
                res += port.readString(1, timeout);
            } catch (SerialPortTimeoutException ex) {
                return "";
            }

            if (start.until(Instant.now(), ChronoUnit.MILLIS) > timeout) {
                return "";
            }

            if (res.endsWith(delimeter)) {
                return res;
            }
        }

        return "";
    }

    @Override
    public void close() throws Exception {
        if (p1Port.isOpened()) {
            p1Port.closePort();
        }
        if (pxPort.isOpened()) {
            pxPort.closePort();
        }
        if (p2Port.isOpened()) {
            p2Port.closePort();
        }
        if (p3Port.isOpened()) {
            p3Port.closePort();
        }
        try {
            this.setP1PowerStateOn(false);
        } catch (P1ControlException ex) {
            LoggerFactory.getLogger(getClass()).warn("Can't turn of the power on P1");
        }
    }

    private void setupPorts(String p2, String p3, String p1, String px) {
        try {
            if (p2 == null) {
                p2Port = null;
            } else {
                p2Port = new SerialPort(p2);
                p2Port.openPort();
                p2Port.setParams(2400, 8, 1, 0);
                p2Lock = new ReentrantLock();
            }
        } catch (SerialPortException ex) {
            p2Port = null;
            LoggerFactory.getLogger(getClass()).error("Can't open " + p2, ex);
        }

        try {
            if (p3 == null) {
                p3Port = null;
            } else {
                p3Port = new SerialPort(p3);
                p3Port.openPort();
                p3Port.setParams(2400, 8, 1, 0);
                p3Lock = new ReentrantLock();
            }
        } catch (SerialPortException ex) {
            p2Port = null;
            LoggerFactory.getLogger(getClass()).error("Can't open " + p3, ex);
        }

        try {
            if (p1 == null) {
                p1Port = null;
            } else {
                p1Port = new SerialPort(p1);
                p1Port.openPort();
                p1Port.setParams(9600, 8, 1, 0);
                p1Lock = new ReentrantLock();
            }
        } catch (SerialPortException ex) {
            p2Port = null;
            LoggerFactory.getLogger(getClass()).error("Can't open " + p1, ex);
        }
        if (px == null) {
            pxPort = null;
        } else {
            if (px.equals(p1)) {
                pxPort = p1Port;
                pxLock = p1Lock;
            } else {
                try {

                    pxPort = new SerialPort(px);
                    pxPort.openPort();
                    pxPort.setParams(9600, 8, 1, 0);
                    pxLock = new ReentrantLock();

                } catch (SerialPortException ex) {
                    p2Port = null;
                    LoggerFactory.getLogger(getClass()).error("Can't open " + px, ex);
                }
            }
        }
    }

    private Value readCM(SerialPort port) {
        try {
            if (!port.isOpened()) {
                port.openPort();
            }
            port.purgePort(SerialPort.PURGE_RXCLEAR | SerialPort.PURGE_TXCLEAR);
            port.writeString(CM32Query);
            LoggerFactory.getLogger(port.getPortName()).info("send> " + CM32Query.trim());
//            try {
//                Thread.sleep(200);
//            } catch (InterruptedException ex) {
//                Logger.getLogger(VACDeviceReader.class.getName()).log(Level.SEVERE, null, ex);
//            }
            String answer = readPort(port, "T--", 400);
            LoggerFactory.getLogger(port.getPortName()).info("recieve> " + answer.trim());
            if (answer.isEmpty()) {
                return Value.of("EMPTY");
            }

            if (answer.indexOf("PM1:mbar") < -1) {
                return Value.of("PARSE");
            }

            if (answer.substring(14, 17).equals("OFF")) {
                return Value.of("OFF");
            }
            return Value.of(Double.parseDouble(answer.substring(14, 17) + answer.substring(19, 23)));
        } catch (SerialPortException ex) {
            return Value.of("COM_ERR");
        }/* catch (SerialPortTimeoutException ex) {
         return Value.of("COM_TIMEOUT");
         }*/

    }

    private Value readP1() {
        if (p1Port == null) {
            return Value.of("NO_CON");
        }
        try {
            String answer = talkMKS(p1Port, P1Query);
            if (answer == null || answer.isEmpty()) {
                return Value.of("EMPTY");
            }
            double res = Double.parseDouble(answer);
            if (res <= 0) {
                return Value.of("OFF");
            } else {
                return Value.of(res);
            }
        } catch (SerialPortException ex) {
            return Value.of("COM_ERR");
        } catch (ResponseParseException ex) {
            return Value.of("PARSE");
        }
    }

    private Value readPx() {
        if (pxPort == null) {
            return Value.of("NO_CON");
        }
        try {
            if (!pxPort.isOpened()) {
                pxPort.openPort();
            }
            pxPort.purgePort(SerialPort.PURGE_RXCLEAR | SerialPort.PURGE_TXCLEAR);
            pxPort.writeString(MQuery);
            LoggerFactory.getLogger(pxPort.getPortName()).info("send> " + MQuery.trim());

            String answer = readPort(pxPort, "\r\n", 100);
            LoggerFactory.getLogger(pxPort.getPortName()).info("recieve> " + answer.trim());

            if (answer.isEmpty()) {
                return Value.of("EMPTY");
            }
            Matcher match = Pattern.compile(":010304(\\w{4})(\\w{4})..\r\n").matcher(answer);

            if (match.matches()) {
                double base = (double) (Integer.parseInt(match.group(1), 16)) / 10d;
                int exp = Integer.parseInt(match.group(2), 16);
                if (exp > 32766) {
                    exp = exp - 65536;
                }
                BigDecimal res = BigDecimal.valueOf(base * Math.pow(10, exp));
                res = res.setScale(4, RoundingMode.CEILING);
                return Value.of(res);
            } else {
                return Value.of("PARSE");
            }
        } catch (SerialPortException ex) {
            return Value.of("COM_ERR");
        }
    }

    private Value readP2() {
        if (p2Port == null) {
            return Value.of("NO_CON");
        }
        try {
            p2Lock.lock();
            return readCM(p2Port);
        } finally {
            p2Lock.unlock();
        }
    }

    private Value readP3() {
        if (p3Port == null) {
            return Value.of("NO_CON");
        }
        try {
            p3Lock.lock();
            return readCM(p3Port);
        } finally {
            p3Lock.unlock();
        }
    }

    @Override
    public boolean hasNext() {
        return true;
    }

    @Override
    public DataPoint next() {
        Value p1 = readP1();
        Value p2 = readP2();
        Value p3 = readP3();
        Value px = readPx();
        Value time = Value.of(Instant.now().truncatedTo(ChronoUnit.SECONDS));
        return new MapPoint(VACManager.names, time, p1, p2, p3, px);
    }

}

//P2, P3:
//Q:"MES R PM 1\r\n"
//A:"PM1:mbar  : 2.3  E-03:T--
//"
//
//P1:
//Q:"@253PR4?;FF"
//A:"@253ACK3.891E+1;FF"
//
//Px:
//Q:":010300000002FA\r\n"
//A:":0103040014FFFEE7"
//нужно брать символы с 8 по 12
