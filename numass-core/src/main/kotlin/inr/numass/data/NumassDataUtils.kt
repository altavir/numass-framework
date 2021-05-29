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

package inr.numass.data

import hep.dataforge.io.envelopes.Envelope
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder
import inr.numass.data.api.*
import inr.numass.data.storage.ClassicNumassPoint
import org.slf4j.LoggerFactory
import kotlin.streams.asSequence


/**
 * Created by darksnake on 30-Jan-17.
 */
object NumassDataUtils {
    fun join(setName: String, sets: Collection<NumassSet>): NumassSet {
        return object : NumassSet {
            override suspend fun getHvData() = TODO()

            override val points: List<NumassPoint> by lazy {
                val points = sets.flatMap { it.points }.groupBy { it.voltage }
                return@lazy points.entries.map { entry -> SimpleNumassPoint.build(entry.value, entry.key) }
            }

            override val meta: Meta by lazy {
                val metaBuilder = MetaBuilder()
                sets.forEach { set -> metaBuilder.putNode(set.name, set.meta) }
                metaBuilder
            }

            override val name = setName
        }
    }

    fun joinByIndex(setName: String, sets: Collection<NumassSet>): NumassSet {
        return object : NumassSet {
            override suspend fun getHvData() = TODO()

            override val points: List<NumassPoint> by lazy {
                val points = sets.flatMap { it.points }.groupBy { it.index }
                return@lazy points.mapNotNull { (index, points) ->
                    val voltage = points.first().voltage
                    if (!points.all { it.voltage == voltage }) {
                        LoggerFactory.getLogger(javaClass)
                            .warn("Not all points with index $index have voltage $voltage")
                        null
                    } else {
                        SimpleNumassPoint.build(points, voltage, index)
                    }
                }
            }

            override val meta: Meta by lazy {
                val metaBuilder = MetaBuilder()
                sets.forEach { set -> metaBuilder.putNode(set.name, set.meta) }
                metaBuilder
            }

            override val name = setName
        }
    }


    fun adapter(): SpectrumAdapter {
        return SpectrumAdapter("Uset", "CR", "CRerr", "Time")
    }

    fun read(envelope: Envelope): NumassPoint {
        return if (envelope.dataType?.startsWith("numass.point.classic") ?: envelope.meta.hasValue("split")) {
            ClassicNumassPoint(envelope)
        } else {
            ProtoNumassPoint.fromEnvelope(envelope)
        }
    }
}

suspend fun NumassBlock.transformChain(transform: (NumassEvent, NumassEvent) -> Pair<Short, Long>?): NumassBlock {
    return SimpleBlock.produce(this.startTime, this.length) {
        this.events.asSequence()
            .sortedBy { it.timeOffset }
            .zipWithNext(transform)
            .filterNotNull()
            .map { OrphanNumassEvent(it.first, it.second) }.asIterable()
    }
}

suspend fun NumassBlock.filterChain(condition: (NumassEvent, NumassEvent) -> Boolean): NumassBlock {
    return SimpleBlock.produce(this.startTime, this.length) {
        this.events.asSequence()
            .sortedBy { it.timeOffset }
            .zipWithNext().filter { condition.invoke(it.first, it.second) }.map { it.second }.asIterable()
    }
}

suspend fun NumassBlock.filter(condition: (NumassEvent) -> Boolean): NumassBlock {
    return SimpleBlock.produce(this.startTime, this.length) {
        this.events.asSequence().filter(condition).asIterable()
    }
}

suspend fun NumassBlock.transform(transform: (NumassEvent) -> OrphanNumassEvent): NumassBlock {
    return SimpleBlock.produce(this.startTime, this.length) {
        this.events.asSequence()
            .map { transform(it) }
            .asIterable()
    }
}