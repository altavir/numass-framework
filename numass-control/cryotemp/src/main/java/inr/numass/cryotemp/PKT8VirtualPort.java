/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.cryotemp;

import hep.dataforge.control.ports.VirtualPort;
import hep.dataforge.meta.Annotated;
import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaUtils;
import hep.dataforge.values.Value;

import java.time.Duration;
import java.util.Random;


/**
 * @author Alexander Nozik
 */
public class PKT8VirtualPort extends VirtualPort implements Annotated {

    private final Random generator = new Random();

    public PKT8VirtualPort(String portName, Meta meta) {
        super.configure(meta).configureValue("id", portName);
    }

    @Override
    protected void evaluateRequest(String request) {
        switch (request) {
            case "s":
                String[] letters = {"a", "b", "c", "d", "e", "f", "g", "h"};
                for (String letter : letters) {
                    Meta channelMeta = MetaUtils.findNodeByValue(meta(), "channel", "letter", Value.of(letter));

                    double average;
                    double sigma;
                    if (channelMeta != null) {
                        average = channelMeta.getDouble("av", 1200);
                        sigma = channelMeta.getDouble("sigma", 50);
                    } else {
                        average = 1200d;
                        sigma = 50d;
                    }

                    this.planRegularResponse(
                            () -> {
                                double res = average + generator.nextGaussian() * sigma;
                                //TODO convert double value to formatted string
                                return letter + "000120000\n";
                            },
                            Duration.ZERO, Duration.ofSeconds(5), letter, "measurement"
                    );
                }
                return;
            case "p":
                cancelByTag("measurement");
                this.recievePhrase("stopped\n\r");
                return;
        }
    }

    @Override
    public void close() throws Exception {
        cancelByTag("measurement");
        super.close();
    }

}
