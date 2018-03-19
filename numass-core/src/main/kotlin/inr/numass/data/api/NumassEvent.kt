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
package inr.numass.data.api

import java.io.Serializable
import java.time.Instant

/**
 * A single numass event with given amplitude and time.
 *
 * @author Darksnake
 * @property amp the amplitude of the event
 * @property blockTime
 * @property timeOffset time in nanoseconds relative to block start
 *
 */
class NumassEvent(val amp: Short, val timeOffset: Long, val block: NumassBlock? = null) : Serializable {

    val channel: NumassChannel?
        get() = block?.channel

    val time: Instant
        get() = (block?.startTime ?: Instant.EPOCH).plusNanos(timeOffset)

}
