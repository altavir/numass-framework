package inr.numass.scripts.underflow

import hep.dataforge.cache.CachePlugin
import hep.dataforge.context.Context
import hep.dataforge.context.Global
import hep.dataforge.data.DataNode
import hep.dataforge.grind.GrindShell
import hep.dataforge.io.ColumnedDataWriter
import hep.dataforge.meta.Meta
import hep.dataforge.plots.fx.FXPlotManager
import hep.dataforge.tables.Table
import inr.numass.NumassPlugin
import inr.numass.data.NumassDataUtils
import inr.numass.data.api.NumassAnalyzer

import static hep.dataforge.grind.Grind.buildMeta

Context ctx = Global.instance()
ctx.pluginManager().load(FXPlotManager)
ctx.pluginManager().load(NumassPlugin.class)
ctx.pluginManager().load(CachePlugin.class)

Meta meta = buildMeta {
    data(dir: "D:\\Work\\Numass\\data\\2017_05\\Fill_2", mask: "set_.{1,3}")
    generate(t0: 3e4, sort: true)
}

def shell = new GrindShell(ctx);

DataNode<Table> spectra = UnderflowUtils.getSpectraMap(shell, meta);

shell.eval {
    Table p17100 = NumassAnalyzer.spectrumWithBinning(spectra.optData("17100").get().get(),20);
    Table p17000 = NumassAnalyzer.spectrumWithBinning(spectra.optData("17000").get().get(),20);

    Table subtract =NumassDataUtils.subtractSpectrum(p17100,p17000);

    ColumnedDataWriter.writeTable(System.out, subtract, "Response function")
}