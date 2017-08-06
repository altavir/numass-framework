package inr.numass.scripts.underflow

import hep.dataforge.cache.CachePlugin
import hep.dataforge.context.Context
import hep.dataforge.context.Global
import hep.dataforge.data.DataNode
import hep.dataforge.grind.GrindShell
import hep.dataforge.io.ColumnedDataWriter
import hep.dataforge.meta.Meta
import hep.dataforge.plots.fx.FXPlotManager
import hep.dataforge.tables.ColumnTable
import hep.dataforge.tables.Table
import inr.numass.NumassPlugin
import inr.numass.data.NumassDataUtils

import static hep.dataforge.grind.Grind.buildMeta

Context ctx = Global.instance()
ctx.pluginManager().load(FXPlotManager)
ctx.pluginManager().load(NumassPlugin.class)
ctx.pluginManager().load(CachePlugin.class)

Meta meta = buildMeta {
    data(dir: "D:\\Work\\Numass\\data\\2017_05\\Fill_2", mask: "set_.{1,3}")
    generate(t0: 3e4, sort: false)
}

def shell = new GrindShell(ctx);

DataNode<Table> spectra = UnderflowUtils.getSpectraMap(shell, meta);

shell.eval {
    def columns = [:];

    Map<Integer, Table> binned = [:]


    (14500..17500).step(500).each {
        Table up = binned.computeIfAbsent(it) { key ->
            NumassDataUtils.spectrumWithBinning(spectra.optData(key as String).get().get(), 20, 400, 3100);
        }

        Table lo = binned.computeIfAbsent(it - 500) { key ->
            NumassDataUtils.spectrumWithBinning(spectra.optData(key as String).get().get(), 20, 400, 3100);
        }

        columns << [channel: up.channel]

        columns << [(it as String): NumassDataUtils.subtractSpectrum(lo, up).getColumn("cr")]
    }

    ColumnedDataWriter.writeTable(System.out, ColumnTable.of(columns), "Response function")

//    println()
//    println()
//
//    columns.clear()
//
//    binned.each { key, table ->
//        columns << [channel: table.channel]
//        columns << [(key as String): table.cr]
//    }
//
//    ColumnedDataWriter.writeTable(System.out,
//            ColumnTable.of(columns),
//            "Spectra")
}