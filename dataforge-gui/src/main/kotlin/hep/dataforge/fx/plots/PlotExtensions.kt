package hep.dataforge.fx.plots

import hep.dataforge.plots.PlotFrame
import hep.dataforge.plots.PlotGroup
import hep.dataforge.plots.Plottable
import hep.dataforge.plots.data.DataPlot
import hep.dataforge.values.Values

fun PlotFrame.plot(plot: Plottable): Plottable {
    this.add(plot);
    return plot;
}

operator fun PlotFrame.plusAssign(plot: Plottable) {
    this.add(plot)
}

fun PlotGroup.plot(plot: Plottable): Plottable {
    this.add(plot);
    return plot;
}

operator fun PlotGroup.plusAssign(plot: Plottable) {
    this.add(plot)
}

fun PlotFrame.group(name: String, action: PlotGroup.() -> Unit) {
    this += PlotGroup(name).apply(action);
}

operator fun DataPlot.plusAssign(point: Values) {
    this.append(point)
}
