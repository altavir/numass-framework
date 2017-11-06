/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.control.readvac;

import hep.dataforge.context.Context;
import hep.dataforge.control.devices.Device;
import hep.dataforge.control.devices.PortSensor;
import hep.dataforge.control.measurements.Measurement;
import hep.dataforge.control.measurements.SimpleMeasurement;
import hep.dataforge.control.ports.PortHandler;
import hep.dataforge.description.ValueDef;
import hep.dataforge.exceptions.ControlException;
import hep.dataforge.meta.Meta;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static hep.dataforge.values.ValueType.NUMBER;

/**
 * @author Alexander Nozik
 */
@ValueDef(name = "address", type = {NUMBER}, def = "1", info = "A modbus address")
public class MeradatVacDevice extends PortSensor<Double> {
    private static final String REQUEST = "0300000002";

    public MeradatVacDevice() {
    }

    public MeradatVacDevice(Context context, Meta meta) {
        setContext(context);
        setMeta(meta);
    }

    @Override
    protected PortHandler buildHandler(String portName) throws ControlException {
        PortHandler newHandler = super.buildHandler(portName);
        newHandler.setDelimiter("\r\n");
        return newHandler;
    }

    @Override
    protected Measurement<Double> createMeasurement() {
        return new MeradatMeasurement();
    }

    @Override
    public String getType() {
        return meta().getString("type", "Vit vacuumeter");
    }

    public static String calculateLRC(String inputString) {
        /*
         * String is Hex String, need to convert in ASCII.
         */
        byte[] bytes = new BigInteger(inputString, 16).toByteArray();
        int checksum = 0;
        for (byte aByte : bytes) {
            checksum += aByte;
        }
        String val = Integer.toHexString(-checksum);
        val = val.substring(val.length() - 2).toUpperCase();
        if (val.length() < 2) {
            val = "0" + val;
        }

        return val;
    }


    private class MeradatMeasurement extends SimpleMeasurement<Double> {

        private final String query; // ":010300000002FA\r\n";
        private final Pattern response;
        private final String base;

        public MeradatMeasurement() {
            base = String.format(":%02d", meta().getInt("address", 1));
            String dataStr = base.substring(1) + REQUEST;
            query = base + REQUEST + calculateLRC(dataStr) + "\r\n";
            response = Pattern.compile(base + "0304(\\w{4})(\\w{4})..\r\n");
        }

        @Override
        protected synchronized Double doMeasure() throws Exception {

            String answer = sendAndWait(query, timeout(), phrase -> phrase.startsWith(base));

            if (answer.isEmpty()) {
                this.updateMessage("No signal");
                updateState(CONNECTED_STATE, false);
                return null;
            } else {
                Matcher match = response.matcher(answer);

                if (match.matches()) {
                    double base = (double) (Integer.parseInt(match.group(1), 16)) / 10d;
                    int exp = Integer.parseInt(match.group(2), 16);
                    if (exp > 32766) {
                        exp = exp - 65536;
                    }
                    BigDecimal res = BigDecimal.valueOf(base * Math.pow(10, exp));
                    res = res.setScale(4, RoundingMode.CEILING);
                    this.updateMessage("OK");
                    updateState(CONNECTED_STATE, true);
                    return res.doubleValue();
                } else {
                    this.updateMessage("Wrong answer: " + answer);
                    updateState(CONNECTED_STATE, false);
                    return null;
                }
            }
        }

        @Override
        public Device getDevice() {
            return MeradatVacDevice.this;
        }
    }

}
