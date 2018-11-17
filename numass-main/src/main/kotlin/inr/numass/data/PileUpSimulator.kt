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

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.data

import hep.dataforge.maths.chain.Chain
import inr.numass.data.api.NumassBlock
import inr.numass.data.api.OrphanNumassEvent
import inr.numass.data.api.SimpleBlock
import org.apache.commons.math3.random.RandomGenerator
import java.lang.Math.max
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * @author [Alexander Nozik](mailto:altavir@gmail.com)
 */
class PileUpSimulator {
    private val pointLength: Long
    private val rnd: RandomGenerator
    private val generated = ArrayList<OrphanNumassEvent>()
    private val pileup = ArrayList<OrphanNumassEvent>()
    private val registered = ArrayList<OrphanNumassEvent>()
    private var generator: Chain<OrphanNumassEvent>
    //private double uSet = 0;
    private val doublePileup = AtomicInteger(0)


    constructor(length: Long, rnd: RandomGenerator, countRate: Double) {
        this.rnd = rnd
        generator = NumassGenerator.generateEvents(countRate, rnd)
        this.pointLength = length
    }

    constructor(pointLength: Long, rnd: RandomGenerator, generator: Chain<OrphanNumassEvent>) {
        this.rnd = rnd
        this.pointLength = pointLength
        this.generator = generator
    }

//    fun withUset(uset: Double): PileUpSimulator {
//        this.uSet = uset
//        return this
//    }

    fun generated(): NumassBlock {
        return SimpleBlock(Instant.EPOCH, Duration.ofNanos(pointLength), generated)
    }

    fun registered(): NumassBlock {
        return SimpleBlock(Instant.EPOCH, Duration.ofNanos(pointLength), registered)
    }

    fun pileup(): NumassBlock {
        return SimpleBlock(Instant.EPOCH, Duration.ofNanos(pointLength), pileup)
    }

    /**
     * The amplitude for pileup event
     *
     * @param x
     * @return
     */
    private fun pileupChannel(x: Double, prevChanel: Short, nextChanel: Short): Short {
        assert(x > 0)
        //эмпирическая формула для канала
        val coef = max(0.0, 0.99078 + 0.05098 * x - 0.45775 * x * x + 0.10962 * x * x * x)
        if (coef < 0 || coef > 1) {
            throw Error()
        }

        return (prevChanel + coef * nextChanel).toShort()
    }

    /**
     * pileup probability
     *
     * @param delay
     * @return
     */
    private fun pileup(delay: Double): Boolean {
        val prob = 1.0 / (1.0 + Math.pow(delay / (2.5 + 0.2), 42.96))
        return random(prob)
    }

    /**
     * Probability for next event to register
     *
     * @param delay
     * @return
     */
    private fun nextEventRegistered(prevChanel: Short, delay: Double): Boolean {
        val average = 6.76102 - 4.31897E-4 * prevChanel + 7.88429E-8 * prevChanel.toDouble() * prevChanel.toDouble() + 0.2
        val prob = 1.0 - 1.0 / (1.0 + Math.pow(delay / average, 75.91))
        return random(prob)
    }

    private fun random(prob: Double): Boolean {
        return rnd.nextDouble() <= prob
    }

    @Synchronized
    fun generate() {
        var next: OrphanNumassEvent
        //var lastRegisteredTime = 0.0 // Time of DAQ closing
        val last = AtomicReference<OrphanNumassEvent>(OrphanNumassEvent(0, 0))

        //flag that shows that previous event was pileup
        var pileupFlag = false
        runBlocking {
            next = generator.next()
            while (next.timeOffset <= pointLength) {
                generated.add(next)
                //not counting double pileups
                if (generated.size > 1) {
                    val delay = (next.timeOffset - last.get().timeOffset) / us //time between events in microseconds
                    if (nextEventRegistered(next.amplitude, delay)) {
                        //just register new event
                        registered.add(next)
                        last.set(next)
                        pileupFlag = false
                    } else if (pileup(delay)) {
                        if (pileupFlag) {
                            //increase double pileup stack
                            doublePileup.incrementAndGet()
                        } else {
                            //pileup event
                            val newChannel = pileupChannel(delay, last.get().amplitude, next.amplitude)
                            val newEvent = OrphanNumassEvent(newChannel, next.timeOffset)
                            //replace already registered event by event with new channel
                            registered.removeAt(registered.size - 1)
                            registered.add(newEvent)
                            pileup.add(newEvent)
                            //do not change DAQ close time
                            pileupFlag = true // up the flag to avoid secondary pileup
                        }
                    } else {
                        // second event not registered, DAQ closed
                        pileupFlag = false
                    }
                } else {
                    //register first event
                    registered.add(next)
                    last.set(next)
                }
                next = generator.next()
            }
        }
    }

    companion object {
        private const val us = 1e-6//microsecond
    }

}
