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
import hep.dataforge.grind.GrindShell
import hep.dataforge.grind.helpers.PlotHelper
import hep.dataforge.meta.Meta
import hep.dataforge.plots.data.PlottableData
import hep.dataforge.plots.data.PlottableGroup
import hep.dataforge.plots.fx.FXPlotManager
import hep.dataforge.tables.Table
import hep.dataforge.tables.TableTransform
import hep.dataforge.tables.XYAdapter
import inr.numass.NumassPlugin
import inr.numass.data.NumassDataUtils
import inr.numass.data.api.NumassAnalyzer
import javafx.application.Platform

import static hep.dataforge.grind.Grind.buildMeta
import static inr.numass.data.api.NumassAnalyzer.CHANNEL_KEY
import static inr.numass.data.api.NumassAnalyzer.COUNT_RATE_KEY

Context ctx = Global.instance()
ctx.pluginManager().load(FXPlotManager)
ctx.pluginManager().load(NumassPlugin.class)
ctx.pluginManager().load(CachePlugin.class)

Meta meta = buildMeta {
    data(dir: "D:\\Work\\Numass\\data\\2017_05\\Fill_2", mask: "set_.{1,3}")
    generate(t0: 3e4, sort: true)
    subtract(reference: 18600)
    fit(xlow: 450, xHigh: 700, upper: 3100, binning: 20)
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
        PlottableGroup<PlottableData> plotGroup = new PlottableGroup<>();
        def adapter = new XYAdapter(CHANNEL_KEY, COUNT_RATE_KEY)
        points.each {
            plotGroup.add(
                    PlottableData.plot(
                            it.key as String,
                            adapter,
                            NumassAnalyzer.spectrumWithBinning(it.value as Table, binning)
                    )
            )
        }

        //configuring and plotting
        plotGroup.configure(showLine: true, showSymbol: false, showErrors: false, connectionType: "step")
        def frame = (plots as PlotHelper).getManager().getPlotFrame("Spectra")
        frame.configureValue("yAxis.type", "log")
        frame.addAll(plotGroup)
    }

    showPoints(spectraMap.findAll { it.key in [16200d, 16400d, 16800d, 17000d, 17200d, 17700d] })

    [550, 600, 650, 750].each { xHigh ->
        println "Caclculate correctuion for upper linearity bound: ${xHigh}"
        Table correctionTable = TableTransform.filter(
                UnderflowFitter.fitAllPoints(
                        spectraMap,
                        meta["fit.xlow"] as int,
                        xHigh,
                        meta["fit.upper"] as int,
                        meta["fit.binning"] as int
                ),
                "correction",
                0,
                2
        )

//        ColumnedDataWriter.writeTable(System.out, correctionTable, "underflow parameters")

        Platform.runLater {
            (plots as PlotHelper).plot(correctionTable, name: "upper_${xHigh}", frame: "Correction") {
                adapter("x.value": "U", "y.value": "correction")
            }
        }
    }
}