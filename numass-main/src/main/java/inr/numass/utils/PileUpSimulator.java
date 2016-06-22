/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.utils;

import inr.numass.generators.NMEventGenerator;
import inr.numass.storage.NMEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 *
 * @author Alexander Nozik <altavir@gmail.com>
 */
public class PileUpSimulator {

    private NMEventGenerator generator;
    private final List<NMEvent> generated = new ArrayList<>();
    private final List<NMEvent> pileup = new ArrayList<>();
    private final List<NMEvent> registred = new ArrayList<>();

    private Function<Double, Double> firstEventSurvivalProb;
    private Function<Double, Double> secondEventSurvivalProb;
    
}
