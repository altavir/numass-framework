/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package inr.numass.scripts.underflow

import hep.dataforge.cache.CachePlugin
import hep.dataforge.context.Context
import hep.dataforge.context.Global
import hep.dataforge.data.DataNode
import hep.dataforge.fx.plots.FXPlotManager
import hep.dataforge.grind.GrindShell
import hep.dataforge.grind.helpers.PlotHelper
import hep.dataforge.io.ColumnedDataWriter
import hep.dataforge.meta.Meta
import hep.dataforge.plots.PlotGroup
import hep.dataforge.plots.data.DataPlot
import hep.dataforge.tables.Adapters
import hep.dataforge.tables.Table
import hep.dataforge.tables.TableTransform
import inr.numass.NumassPlugin
import inr.numass.data.analyzers.NumassAnalyzerKt
import inr.numass.subthreshold.Threshold
import javafx.application.Platform

import static hep.dataforge.grind.Grind.buildMeta
import static inr.numass.data.analyzers.NumassAnalyzer.CHANNEL_KEY
import static inr.numass.data.analyzers.NumassAnalyzer.COUNT_RATE_KEY

Context ctx = Global.instance()
ctx.getPluginManager().load(FXPlotManager)
ctx.getPluginManager().load(NumassPlugin)
ctx.getPluginManager().load(CachePlugin)

Meta meta = buildMeta(t0: 3e4) {
    data(dir: "D:\\Work\\Numass\\data\\2017_11\\Fill_2", mask: "set_3.")
    subtract(reference: 18500)
    fit(xLow: 400, xHigh: 600, upper: 3000, binning: 20, method: "pow")
    scan(
            hi: [500, 600, 700, 800, 900],
            lo: [350, 400, 450],
            def: 700
    )
    window(lo: 300, up: 3000)
}


def shell = new GrindShell(ctx);

DataNode<Table> spectra = Threshold.INSTANCE.getSpectraMap(ctx, meta).computeAll();

shell.eval {

    //subtracting reference point
    Map<Double, Table> spectraMap
    if (meta.hasValue("subtract.reference")) {
        String referenceVoltage = meta["subtract.reference"]
        println "subtracting reference point ${referenceVoltage}"
        def referencePoint = spectra.get(referenceVoltage)
        spectraMap = spectra
                .findAll { it.name != referenceVoltage }
                .collectEntries {
            [(it.meta["voltage"]): NumassAnalyzerKt.subtractAmplitudeSpectrum(it.get(), referencePoint)]
        }
    } else {
        spectraMap = spectra.collectEntries { return [(it.meta["voltage"]): it.get()] }
    }

    //Showing selected points
    def showPoints = { Map points, int binning = 20, int loChannel = 300, int upChannel = 2000 ->
        def plotGroup = new PlotGroup("points");
        def adapter = Adapters.buildXYAdapter(CHANNEL_KEY, COUNT_RATE_KEY)
        points.each {
            plotGroup.add(
                    DataPlot.plot(
                            it.key as String,
                            adapter,
                            NumassAnalyzerKt.withBinning(it.value as Table, binning)
                    )
            )
        }

        //configuring and plotting
        plotGroup.configure(showLine: true, showSymbol: false, showErrors: false, connectionType: "step")
        def frame = (plots as PlotHelper).getManager().getPlotFrame("Spectra")
        frame.configureValue("yAxis.type", "log")
        frame.add(plotGroup)
    }

    showPoints(spectraMap.findAll { it.key in [14100d, 14200d, 14300d, 14400d, 14800d, 15000d, 15200d, 15700d] })

    meta["scan.hi"].each { xHigh ->
        println "Caclculate correctuion for upper linearity bound: ${xHigh}"
        Table correctionTable = TableTransform.filter(
                Threshold.INSTANCE.calculateSubThreshold(
                        spectraMap,
                        meta.getMeta("fit").builder.setValue("xHigh", xHigh)
                ),
                "correction",
                0,
                2
        )

        if (xHigh == meta["scan.def"]) {
            ColumnedDataWriter.writeTable(System.out, correctionTable, "underflow parameters")
        }

        Platform.runLater {
            (plots as PlotHelper).plot(
                    data: correctionTable,
                    adapter: Adapters.buildXYAdapter("U", "correction"),
                    name: "upper_${xHigh}",
                    frame: "upper"
            )
        }
    }


    meta["scan.lo"].each { xLow ->
        println "Caclculate correctuion for lower linearity bound: ${xLow}"
        Table correctionTable = TableTransform.filter(
                Threshold.INSTANCE.calculateSubThreshold(
                        spectraMap,
                        meta.getMeta("fit").builder.setValue("xLow", xLow)
                ),
                "correction",
                0,
                2
        )

        Platform.runLater {
            (plots as PlotHelper).plot(
                    data: correctionTable,
                    adapter: Adapters.buildXYAdapter("U", "correction"),
                    name: "lower_${xLow}",
                    frame: "lower"
            )
        }
    }
}