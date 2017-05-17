/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.readvac;

import hep.dataforge.context.Context;
import hep.dataforge.control.devices.PortSensor;
import hep.dataforge.control.measurements.Measurement;
import hep.dataforge.control.measurements.SimpleMeasurement;
import hep.dataforge.control.ports.PortHandler;
import hep.dataforge.description.ValueDef;
import hep.dataforge.exceptions.ControlException;
import hep.dataforge.meta.Meta;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Alexander Nozik
 */
@ValueDef(name = "address", type = "NUMBER", def = "1", info = "A modbus address")
public class MeradatVacDevice extends PortSensor<Double> {

    public MeradatVacDevice() {
    }

    public MeradatVacDevice(Context context, Meta meta) {
        setContext(context);
        setMeta(meta);
    }

    @Override
    protected PortHandler buildHandler(String portName) throws ControlException {
        PortHandler newHandler = super.buildHandler(portName);
        newHandler.setDelimeter("\r\n");
        return newHandler;
    }

    @Override
    protected Measurement<Double> createMeasurement() {
        return new MeradatMeasurement(meta().getInt("adress", 1));
    }

    @Override
    public String type() {
        return meta().getString("type", "Vit vacuumeter");
    }


    private class MeradatMeasurement extends SimpleMeasurement<Double> {

        private final String query; // ":010300000002FA\r\n";
        private final Pattern response;
        private final String base;

        public MeradatMeasurement(int address) {
            base = String.format(":%02d", address);
            query = base + "0300000002FA\r\n";
            response = Pattern.compile(base + "0304(\\w{4})(\\w{4})..\r\n");
        }

        @Override
        protected synchronized Double doMeasure() throws Exception {

            String answer = getHandler().sendAndWait(query, timeout(), phrase -> phrase.startsWith(base));

            if (answer.isEmpty()) {
                this.progressUpdate("No signal");
                updateState("connection", false);
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
                    this.progressUpdate("OK");
                    updateState("connection", true);
                    return res.doubleValue();
                }
                else {
                    this.progressUpdate("Wrong answer: " + answer);
                    updateState("connection", false);
                    return null;
                }
            }
        }
    }

}
