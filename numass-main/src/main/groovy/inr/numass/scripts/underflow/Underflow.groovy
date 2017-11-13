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
import hep.dataforge.fx.plots.PlotManager
import hep.dataforge.grind.GrindShell
import hep.dataforge.grind.helpers.PlotHelper
import hep.dataforge.io.ColumnedDataWriter
import hep.dataforge.meta.Meta
import hep.dataforge.plots.PlotGroup
import hep.dataforge.plots.data.DataPlot
import hep.dataforge.tables.Table
import hep.dataforge.tables.TableTransform
import hep.dataforge.tables.XYAdapter
import inr.numass.NumassPlugin
import inr.numass.data.NumassDataUtils
import javafx.application.Platform

import static hep.dataforge.grind.Grind.buildMeta
import static inr.numass.data.api.NumassAnalyzer.CHANNEL_KEY
import static inr.numass.data.api.NumassAnalyzer.COUNT_RATE_KEY

Context ctx = Global.instance()
ctx.getPluginManager().load(PlotManager)
ctx.getPluginManager().load(NumassPlugin)
ctx.getPluginManager().load(CachePlugin)

Meta meta = buildMeta {
    data(dir: "D:\\Work\\Numass\\data\\2017_05\\Fill_2", mask: "set_.{1,3}")
    generate(t0: 3e4)
    subtract(reference: 18500)
    fit(xLow: 450, xHigh: 700, upper: 3100, binning: 20)
}


def shell = new GrindShell(ctx);

DataNode<Table> spectra = UnderflowUtils.getSpectraMap(shell, meta);

shell.eval {

    //subtracting reference point
    Map<Double, Table> spectraMap
    if (meta.hasValue("subtract.reference")) {
        String referenceVoltage = meta["subtract.reference"]
        println "subtracting reference point ${referenceVoltage}"
        def referencePoint = spectra.compute(referenceVoltage)
        spectraMap = spectra
                .findAll { it.name != referenceVoltage }
                .collectEntries {
            [(it.meta["voltage"]): NumassDataUtils.subtractSpectrum(it.get(), referencePoint)]
        }
    } else {
        spectraMap = spectra.collectEntries { return [(it.meta["voltage"]): it.get()] }
    }

    //Showing selected points
    def showPoints = { Map points, int binning = 20, int loChannel = 300, int upChannel = 2000 ->
        def plotGroup = new PlotGroup("points");
        def adapter = new XYAdapter(CHANNEL_KEY, COUNT_RATE_KEY)
        points.each {
            plotGroup.add(
                    DataPlot.plot(
                            it.key as String,
                            adapter,
                            NumassDataUtils.spectrumWithBinning(it.value as Table, binning)
                    )
            )
        }

        //configuring and plotting
        plotGroup.configure(showLine: true, showSymbol: false, showErrors: false, connectionType: "step")
        def frame = (plots as PlotHelper).getManager().getPlotFrame("Spectra")
        frame.configureValue("yAxis.type", "log")
        frame.add(plotGroup)
    }

    showPoints(spectraMap.findAll { it.key in [16200d, 16400d, 16800d, 17000d, 17200d, 17700d] })

    [550, 600, 650, 700, 750].each { xHigh ->
        println "Caclculate correctuion for upper linearity bound: ${xHigh}"
        Table correctionTable = TableTransform.filter(
                UnderflowFitter.fitAllPoints(
                        spectraMap,
                        meta["fit.xLow"] as int,
                        xHigh,
                        meta["fit.upper"] as int,
                        meta["fit.binning"] as int
                ),
                "correction",
                0,
                2
        )

        if (xHigh == 700) {
            ColumnedDataWriter.writeTable(System.out, correctionTable, "underflow parameters")
        }

        Platform.runLater {
            (plots as PlotHelper).plot(correctionTable, new XYAdapter("U", "correction"), "upper_${xHigh}", "upper")
        }
    }


    [400, 450, 500].each { xLow ->
        println "Caclculate correctuion for lower linearity bound: ${xLow}"
        Table correctionTable = TableTransform.filter(
                UnderflowFitter.fitAllPoints(
                        spectraMap,
                        xLow,
                        meta["fit.xHigh"] as int,
                        meta["fit.upper"] as int,
                        meta["fit.binning"] as int
                ),
                "correction",
                0,
                2
        )

        Platform.runLater {
            (plots as PlotHelper).plot(correctionTable, new XYAdapter("U", "correction"), "lower_${xLow}", "lower")
        }
    }
}