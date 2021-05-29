package hep.dataforge.grind.demo

import hep.dataforge.context.Global
import hep.dataforge.grind.GrindShell
import hep.dataforge.grind.workspace.WorkspaceSpec
import hep.dataforge.plots.PlotFrame
import hep.dataforge.plots.data.DataPlot
import hep.dataforge.plots.output.PlotOutputKt
import hep.dataforge.tables.ColumnTable
import hep.dataforge.tables.Table
import hep.dataforge.values.ValueType
import javafx.application.Platform

import static hep.dataforge.grind.workspace.DefaultTaskLib.custom
import static hep.dataforge.grind.workspace.DefaultTaskLib.pipe

def workspace = new WorkspaceSpec(Global.instance()).with {

    context {
        name = "TEST"
        properties{
            power = 2d
        }
    }

    data {
        item("xs") {
            meta(axis: "x")
            (1..100).asList() //generate xs
        }
        node("ys") {
            Random rnd = new Random()

            meta(showLine: true)
            item("y1") {
                meta(axis: "y")
                (1..100).collect { it**2 }
            }
            item("y2") {
                meta(axis: "y")
                (1..100).collect { it**2 + rnd.nextDouble() }
            }
            item("y3") {
                meta(thickness: 4, color: "magenta", showSymbol: false, showErrors: false)
                (1..100).collect { (it + rnd.nextDouble() / 2)**2 }
            }
        }
    }

    task custom("table") {
        def xs = input.optData("xs").get()
        def ys = input.getNode("ys")
        ys.dataStream().forEach {
            //yield result
            yield it.name, combine(xs, it, Table.class, it.meta) { x, y ->
                new ColumnTable()
                        .addColumn("x", ValueType.NUMBER, (x as List).stream())
                        .addColumn("y", ValueType.NUMBER, (y as List).stream())
            }
        }
    }

    task pipe("dif", dependsOn: "table") {
        result {input ->
            def power = meta.getDouble("power", context.getDouble("power"))
            return (input as ColumnTable).buildColumn("y", ValueType.NUMBER) {
                it["y"] - it["x"]**power
            }
        }
    }

}.build()

new GrindShell().eval {
    //loading plot feature
    PlotFrame frame = PlotOutputKt.getPlotFrame(context, "demo")

    frame.configure("yAxis.type": "log")

    workspace.run("dif").dataStream().forEach {
        frame.add new DataPlot(it.name, it.meta).fillData(it.get() as Table)
    }
    Platform.implicitExit = true
}
