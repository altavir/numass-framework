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

package inr.numass.data.api

import hep.dataforge.meta.Metoid
import hep.dataforge.providers.Provider
import hep.dataforge.providers.Provides
import java.time.Duration
import java.time.Instant
import java.util.stream.Stream

/**
 * Created by darksnake on 06-Jul-17.
 */
interface NumassPoint : Metoid, ParentBlock, Provider {


    override val blocks: List<NumassBlock>

    /**
     * Provides block with given number (starting with 0)
     */
    @Provides(NUMASS_BLOCK_TARGET)
    operator fun get(index: Int): NumassBlock? {
        return blocks[index]
    }

    /**
     * Provides all blocks in given channel
     */
    @Provides(NUMASS_CHANNEL_TARGET)
    fun channel(index: Int): NumassBlock? {
        return channels[index]
    }

    /**
     * Distinct map of channel number to corresponding grouping block
     */
    val channels: Map<Int, NumassBlock>
        get() = blocks.toList().groupBy { it.channel }.mapValues { entry ->
            if (entry.value.size == 1) {
                entry.value.first()
            } else {
                MetaBlock(entry.value)
            }
        }


    /**
     * Get the voltage setting for the point
     *
     * @return
     */
    val voltage: Double
        get() = meta.getDouble(HV_KEY, 0.0)

    /**
     * Get the index for this point in the set
     * @return
     */
    val index: Int
        get() = meta.getInt(INDEX_KEY, -1)

    /**
     * Get the first block if it exists. Throw runtime exception otherwise.
     *
     * @return
     */
    val firstBlock: NumassBlock
        get() = blocks.firstOrNull() ?: throw RuntimeException("The point is empty")

    /**
     * Get the starting time from meta or from first block
     *
     * @return
     */
    override val startTime: Instant
        get() = meta.optValue(START_TIME_KEY).map<Instant> { it.time }.orElseGet { firstBlock.startTime }

    /**
     * Get the length key of meta or calculate length as a sum of block lengths. The latter could be a bit slow
     *
     * @return
     */
    override val length: Duration
        get() = Duration.ofNanos(blocks.stream().filter { it.channel == 0 }.mapToLong { it -> it.length.toNanos() }.sum())

    /**
     * Get all events it all blocks as a single sequence
     *
     *
     * Some performance analysis of different stream concatenation approaches is given here: https://www.techempower.com/blog/2016/10/19/efficient-multiple-stream-concatenation-in-java/
     *
     *
     * @return
     */
    override val events: Stream<NumassEvent>
        get() = blocks.stream().flatMap { it.events }

    /**
     * Get all frames in all blocks as a single sequence
     *
     * @return
     */
    override val frames: Stream<NumassFrame>
        get() = blocks.stream().flatMap { it.frames }


    override val isSequential: Boolean
        get() = channels.size == 1

    companion object {
        const val NUMASS_BLOCK_TARGET = "block"
        const val NUMASS_CHANNEL_TARGET = "channel"

        const val START_TIME_KEY = "start"
        const val LENGTH_KEY = "length"
        const val HV_KEY = "voltage"
        const val INDEX_KEY = "index"
    }
}
