/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package inr.numass.scripts.underflow

import hep.dataforge.context.Context
import hep.dataforge.context.Global
import hep.dataforge.grind.Grind
import hep.dataforge.grind.GrindShell
import hep.dataforge.grind.helpers.PlotHelper
import hep.dataforge.io.ColumnedDataWriter
import hep.dataforge.meta.Meta
import hep.dataforge.plots.data.PlottableData
import hep.dataforge.plots.data.PlottableGroup
import hep.dataforge.plots.fx.FXPlotManager
import hep.dataforge.storage.commons.StorageUtils
import hep.dataforge.tables.Table
import hep.dataforge.tables.TableTransform
import hep.dataforge.tables.XYAdapter
import inr.numass.NumassPlugin
import inr.numass.data.NumassDataUtils
import inr.numass.data.analyzers.TimeAnalyzer
import inr.numass.data.api.NumassAnalyzer
import inr.numass.data.api.NumassPoint
import inr.numass.data.api.SimpleNumassPoint
import inr.numass.data.storage.NumassStorage
import inr.numass.data.storage.NumassStorageFactory

import java.util.stream.Collectors

import static inr.numass.data.api.NumassAnalyzer.CHANNEL_KEY
import static inr.numass.data.api.NumassAnalyzer.COUNT_RATE_KEY

Context ctx = Global.instance()
ctx.pluginManager().load(FXPlotManager)
ctx.pluginManager().load(NumassPlugin.class)

new GrindShell(ctx).eval {

    //Defining root directory
    File dataDirectory = new File("D:\\Work\\Numass\\data\\2017_05\\Fill_2")

    //creating storage instance

    NumassStorage storage = NumassStorageFactory.buildLocal(dataDirectory);

    //Reading points
    Map<Double, List<NumassPoint>> allPoints = StorageUtils
            .loaderStream(storage)
            .filter { it.key.matches("set_.{1,3}") }
            .map {
        println "loading ${it.key}"
        it.value
    }.flatMap { it.points }
            .collect(Collectors.groupingBy { it.voltage })

    Meta analyzerMeta = Grind.buildMeta(t0: 3e4)
    NumassAnalyzer analyzer = new TimeAnalyzer()

    //creating spectra
    Map spectra = allPoints.collectEntries {
        def point = new SimpleNumassPoint(it.key, it.value)
        println "generating spectrum for ${point.voltage}"
        return [(point.voltage): analyzer.getSpectrum(point, analyzerMeta)]
    }



    def refereceVoltage = 18600d

    //subtracting reference point
    if (refereceVoltage) {
        println "subtracting reference point ${refereceVoltage}"
        def referencePoint = spectra[refereceVoltage]
        spectra = spectra.findAll { it.key != refereceVoltage }.collectEntries {
            return [(it.key): NumassDataUtils.subtractSpectrum(it.getValue() as Table, referencePoint as Table)]
        }
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
        frame.configure("yAxis.type": "log")
        frame.addAll(plotGroup)
    }

    showPoints(spectra.findAll { it.key in [16200d, 16400d, 16800d, 17000d, 17200d, 17700d] })

    println()

    Table correctionTable = TableTransform.filter(
            UnderflowFitter.fitAllPoints(spectra, 450, 700, 3100, 20),
            "correction",
            0,
            2)
    ColumnedDataWriter.writeTable(System.out, correctionTable, "underflow parameters")

    (plots as PlotHelper).plot(correctionTable, name: "correction", frame: "Correction") {
        adapter("x.value":"U", "y.value":"correction")
    }
}