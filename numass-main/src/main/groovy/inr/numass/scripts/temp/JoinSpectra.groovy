package inr.numass.scripts.temp

import hep.dataforge.context.Context
import hep.dataforge.context.Global
import hep.dataforge.description.DescriptorUtils
import hep.dataforge.fx.plots.PlotManager
import hep.dataforge.grind.Grind
import hep.dataforge.grind.GrindShell
import hep.dataforge.grind.helpers.PlotHelper
import hep.dataforge.plots.PlotFrame
import hep.dataforge.plots.data.DataPlot
import hep.dataforge.tables.Adapters
import hep.dataforge.tables.ColumnTable
import hep.dataforge.tables.Table
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


Table.metaClass.withBinning { int binning ->
    return NumassDataUtils.spectrumWithBinning(delegate, binning)
}

Table.metaClass.withDeadTime { double dt = 6.5 ->
    double totalCR = delegate.getColumn(NumassAnalyzer.COUNT_RATE_KEY).stream().mapToDouble { it.doubleValue() }.sum()
//    long totalCount = delegate.getColumn(NumassAnalyzer.COUNT_RATE_KEY).stream().mapToLong() { it.longValue() }.sum()
//    double time = totalCount / totalCR
    double factor = 1d / (1d - dt * 1e-6 * totalCR)
    return ColumnTable.copy(delegate)
                      .replaceColumn(NumassAnalyzer.COUNT_RATE_KEY) {
        it.getDouble(NumassAnalyzer.COUNT_RATE_KEY) * factor
    }
                      .replaceColumn(NumassAnalyzer.COUNT_RATE_ERROR_KEY) {
        it.getDouble(NumassAnalyzer.COUNT_RATE_ERROR_KEY) * factor
    }
}


new GrindShell(ctx).eval {
    File rootDir = new File("D:\\Work\\Numass\\data\\2017_11\\Fill_1")

    NumassStorage storage = NumassStorageFactory.buildLocal(rootDir);

    NumassSet joined = NumassDataUtils.join("sum", storage.loaders()
                                                          .findAll { it instanceof NumassSet }
                                                          .collect { it as NumassSet }
    )


    NumassAnalyzer analyzer = new SmartAnalyzer();

    def adapter = Adapters.buildXYAdapter(NumassAnalyzer.CHANNEL_KEY, NumassAnalyzer.COUNT_RATE_KEY, NumassAnalyzer.COUNT_RATE_ERROR_KEY)

    def t0 = 15

    PlotFrame frame = (plots as PlotHelper).getManager().getPlotFrame("test", "spectra")

    frame.plots.setDescriptor(DescriptorUtils.buildDescriptor(DataPlot))
    frame.plots.configure(showErrors: false, showSymbol: false, showLine: true, connection: "step")

    joined.points.filter { it.voltage in [14000d, 15000d, 16000d, 17000d, 18000d] }.forEach {
        //Table spectrum = analyzer.getSpectrum(it, Meta.empty()).withBinning(20).withDeadTime()
        Table spectrum = analyzer.getSpectrum(it, Grind.buildMeta(t0: t0*1000)).withBinning(20).withDeadTime(t0)
        frame.add(DataPlot.plot(it.voltage.toString(), adapter, spectrum))
    }

//    def point = joined.points.find { it.voltage == 14000d } as NumassPoint
//    PlotFrame pointFrame = (plots as PlotHelper).getManager().getPlotFrame("test", "14000")
//
//    pointFrame.plots.setDescriptor(DescriptorUtils.buildDescriptor(DataPlot))
//    pointFrame.plots.configure(showErrors: false, showSymbol: false, showLine: true, connection: "step")
//
//    [0, 5, 10,15,20].forEach{
//        Table spectrum = analyzer.getSpectrum(point, Grind.buildMeta(t0: it*1000)).withBinning(20).withDeadTime(it)
//        pointFrame.add(DataPlot.plot(it.toString(), adapter,  spectrum))
//    }


}