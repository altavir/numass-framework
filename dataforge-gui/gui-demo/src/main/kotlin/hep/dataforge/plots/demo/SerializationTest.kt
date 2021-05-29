package hep.dataforge.plots.demo

import hep.dataforge.buildContext
import hep.dataforge.fx.output.FXOutputManager
import hep.dataforge.plots.data.DataPlot
import hep.dataforge.plots.data.XYFunctionPlot
import hep.dataforge.plots.jfreechart.JFreeChartPlugin
import hep.dataforge.plots.output.plotFrame
import hep.dataforge.tables.Adapters
import hep.dataforge.tables.Tables
import hep.dataforge.values.ValueMap
import hep.dataforge.values.Values
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.*

/**
 * @param args the command line arguments
 */

fun main() {

    val context = buildContext("TEST", JFreeChartPlugin::class.java) {
        output = FXOutputManager()
    }

    val func = { x: Double -> Math.pow(x, 2.0) }

    val funcPlot = XYFunctionPlot.plot("func", 0.1, 4.0, 200, function = func)


    val names = arrayOf("myX", "myY", "myXErr", "myYErr")

    val data = ArrayList<Values>()
    data.add(ValueMap.of(names, 0.5, 0.2, 0.1, 0.1))
    data.add(ValueMap.of(names, 1.0, 1.0, 0.2, 0.5))
    data.add(ValueMap.of(names, 3.0, 7.0, 0, 0.5))
    val ds = Tables.infer(data)

    val dataPlot = DataPlot.plot("dataPlot", ds, Adapters.buildXYAdapter("myX", "myXErr", "myY", "myYErr"))


    context.plotFrame("before"){
        +dataPlot
    }

    val baos = ByteArrayOutputStream();

    ObjectOutputStream(baos).use {
        it.writeObject(dataPlot)
    }

    val bais = ByteArrayInputStream(baos.toByteArray());
    val restored: DataPlot = ObjectInputStream(bais).readObject() as DataPlot

    context.plotFrame("after"){
        +restored
    }

}