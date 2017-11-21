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
package inr.numass.control.magnet;

import hep.dataforge.control.ports.VirtualPort;
import hep.dataforge.exceptions.PortException;
import hep.dataforge.meta.Meta;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Alexander Nozik
 */
public class VirtualLambdaPort extends VirtualPort {

    private static final Duration latency = Duration.ofMillis(50);

    private volatile int currentAddress = -1;
    private Map<Integer, VirtualMagnetStatus> magnets = new HashMap<>();
    private final String virtualPortName;

    public VirtualLambdaPort(String portName, Map<Integer, Double> magnets) {
        this.virtualPortName = portName;
        magnets.entrySet().stream().forEach((entry) -> {
            this.magnets.put(entry.getKey(), new VirtualMagnetStatus(entry.getValue()));
        });
    }

    public VirtualLambdaPort(String portName, int... magnets) {
        this.virtualPortName = portName;
        for (int magnet : magnets) {
            this.magnets.put(magnet, new VirtualMagnetStatus(0.01));
        }
    }

    @Override
    public String toString() {
        return virtualPortName;
    }

    @Override
    protected void evaluateRequest(String request) {
        String comand;
        String value = "";
        String[] split = request.split(" ");
        if (split.length == 1) {
            comand = request;
        } else {
            comand = split[0];
            value = split[1];
        }
        try {
            evaluateRequest(comand.trim(), value.trim());
        } catch (RuntimeException ex) {

            receivePhrase("FAIL");//TODO какая команда правильная?
            LoggerFactory.getLogger(getClass()).error("Request evaluation failure", ex);
        }

    }

    private void sendOK() {
        planResponse("OK", latency);
    }

    private void evaluateRequest(String comand, String value) {
        switch (comand) {
            case "ADR":
                int address = Integer.parseInt(value);
                if (magnets.containsKey(address)) {
                    currentAddress = address;
                    sendOK();
                }
                return;
            case "ADR?":
                planResponse(Integer.toString(currentAddress), latency);
                return;
            case "OUT":
                int state = Integer.parseInt(value);
                currentMagnet().out = (state == 1);
                sendOK();
                return;
            case "OUT?":
                boolean out = currentMagnet().out;
                if (out) {
                    planResponse("ON", latency);
                } else {
                    planResponse("OFF", latency);
                }
                return;
            case "PC":
                double current = Double.parseDouble(value);
                if (current < 0.5) {
                    current = 0;
                }
                currentMagnet().current = current;
                sendOK();
                return;
            case "PC?":
                planResponse(Double.toString(currentMagnet().current), latency);
                return;
            case "MC?":
                planResponse(Double.toString(currentMagnet().current), latency);
                return;
            case "PV?":
                planResponse(Double.toString(currentMagnet().voltage()), latency);
                return;
            case "MV?":
                planResponse(Double.toString(currentMagnet().voltage()), latency);
                return;
            default:
                LoggerFactory.getLogger(getClass()).warn("Unknown comand {}", comand);
        }
    }

    private VirtualMagnetStatus currentMagnet() {
        if (currentAddress < 0) {
            throw new RuntimeException();
        }
        return magnets.get(currentAddress);
    }

    @Override
    public void close() throws Exception {
        
    }

    @Override
    public void open() throws PortException {
        
    }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public Meta getMeta() {
        return Meta.buildEmpty("virtualPort");
                
    }

    private class VirtualMagnetStatus {

        public VirtualMagnetStatus(double resistance) {
            this.resistance = resistance;
            this.on = true;
            this.out = false;
            this.current = 0;
        }

        public VirtualMagnetStatus(double resistance, boolean on, boolean out, double current) {
            this.resistance = resistance;
            this.on = on;
            this.out = out;
            this.current = current;
        }

        private final double resistance;
        private boolean on;
        private boolean out;
        private double current;

        public double voltage() {
            return current * resistance;
        }

    }
}
