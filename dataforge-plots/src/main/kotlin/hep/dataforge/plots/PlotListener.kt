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
package hep.dataforge.plots

import hep.dataforge.names.Name

/**
 * Listener for plot state changes
 * @author darksnake
 */
interface PlotListener {
    /**
     * Data changed for a specific plot. Data for group could not be changed
     * @param caller the plottable that sent the message
     * @param path the path of added plot relative to caller. If empty, caller is the source of the event
     * @param before the entry before change
     * @param after the entry after change
     */
    fun dataChanged(caller: Plottable, path: Name, before: Plottable?, after: Plottable?)

    /**
     * Configuration changed for node or plot
     * @param caller the plottable that sent the message
     * @param path full path of  plottable with changed meta relative to caller
     */
    fun metaChanged(caller: Plottable, path: Name, plot: Plottable)
}
