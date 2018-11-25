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
import java.util.stream.Collectors
import kotlin.streams.asSequence
import kotlin.streams.toList


/**
 * Created by darksnake on 30-Jan-17.
 */
object NumassDataUtils {
    fun join(setName: String, sets: Collection<NumassSet>): NumassSet {
        return object : NumassSet {
            override suspend fun getHvData() = TODO()

            override val points: List<NumassPoint> by lazy {
                val points = sets.stream().flatMap<NumassPoint> { it.points.stream() }
                        .collect(Collectors.groupingBy<NumassPoint, Double> { it.voltage })
                points.entries.stream().map { entry -> SimpleNumassPoint(entry.value, entry.key) }
                        .toList()
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