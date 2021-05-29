package inr.numass.scripts.analysis

import hep.dataforge.context.Global
import hep.dataforge.fx.output.FXOutputManager
import hep.dataforge.workspace.FileBasedWorkspace
import java.io.File

fun main() {
    FXOutputManager().startGlobal()

    val configPath = File("D:\\Work\\Numass\\sterile2017_05_frames\\workspace.groovy").toPath()
    val workspace = FileBasedWorkspace.build(Global, configPath)
    workspace.context.setValue("cache.enabled", false)

    //val meta = workspace.getTarget("group_3")

    val result = workspace.runTask("fit", "group_5").first().get()
    println("Complete!")

}