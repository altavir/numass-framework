package inr.numass.scripts.temp

import hep.dataforge.context.Context
import hep.dataforge.context.Global
import hep.dataforge.description.DescriptorUtils
import hep.dataforge.fx.plots.PlotManager
import hep.dataforge.grind.GrindShell
import hep.dataforge.grind.helpers.PlotHelper
import hep.dataforge.meta.Meta
import hep.dataforge.plots.PlotFrame
import hep.dataforge.plots.data.DataPlot
import hep.dataforge.tables.ColumnTable
import hep.dataforge.tables.Table
import hep.dataforge.tables.XYAdapter
import inr.numass.NumassPlugin
import inr.numass.data.NumassDataUtils
import inr.numass.data.analyzers.SmartAnalyzer
import inr.numass.data.api.NumassAnalyzer
import inr.numass.data.api.NumassSet
import inr.numass.data.storage.NumassStorage
import inr.numass.data.storage.NumassStorageFactory

Context ctx = Global.instance()
ctx.getPluginManager().load(PlotManager)
ctx.getPluginManager().load(NumassPlugin.class)


Table.metaClass.dt{double dt = 6.5 ->
    double totalCR = delegate.getColumn(NumassAnalyzer.COUNT_RATE_KEY).stream().mapToDouble { it.doubleValue() }.sum()
//    long totalCount = delegate.getColumn(NumassAnalyzer.COUNT_RATE_KEY).stream().mapToLong() { it.longValue() }.sum()
//    double time = totalCount / totalCR
    double factor = 1d / (1d - dt * 1e-6 * totalCR)
    return ColumnTable.copy(delegate)
                      .replaceColumn(NumassAnalyzer.COUNT_RATE_KEY){it.getDouble(NumassAnalyzer.COUNT_RATE_KEY)*factor}
                      .replaceColumn(NumassAnalyzer.COUNT_RATE_ERROR_KEY){it.getDouble(NumassAnalyzer.COUNT_RATE_ERROR_KEY)*factor}
}


new GrindShell(ctx).eval {
    File rootDir = new File("D:\\Work\\Numass\\data\\2017_11\\Fill_1")

    NumassStorage storage = NumassStorageFactory.buildLocal(rootDir);

    NumassSet joined = NumassDataUtils.join("sum", storage.loaders()
                                                          .findAll { it instanceof NumassSet }
                                                          .collect { it as NumassSet }
    )

    PlotFrame frame = (plots as PlotHelper).getManager().getPlotFrame("test", "spectra")

    NumassAnalyzer analyzer = new SmartAnalyzer();

    frame.plots.setDescriptor(DescriptorUtils.buildDescriptor(DataPlot))
    frame.plots.configure(showErrors: false, showSymbol: false, showLine: true, connection: "step")

    joined.points.filter { it.voltage in [14000d, 15000d, 16000d, 17000d, 18000d] }.forEach {
        Table spectrum = NumassDataUtils.spectrumWithBinning(analyzer.getSpectrum(it, Meta.empty()), 20).dt()
        frame.add(DataPlot.plot(
                it.voltage.toString(),
                new XYAdapter(NumassAnalyzer.CHANNEL_KEY, NumassAnalyzer.COUNT_RATE_KEY, NumassAnalyzer.COUNT_RATE_ERROR_KEY),
                spectrum))
    }

}