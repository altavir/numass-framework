package inr.numass.data.api

import java.util.stream.Stream

/**
 * An ancestor to numass frame analyzers
 * Created by darksnake on 07.07.2017.
 */
interface SignalProcessor {
    fun analyze(frame: NumassFrame): Stream<NumassEvent>
}
