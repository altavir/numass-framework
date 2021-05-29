package hep.dataforge.grind.terminal

import hep.dataforge.context.Global

/**
 * Created by darksnake on 27-Oct-16.
 */

println "DataForge grind shell"
try {
    GrindTerminal.system().launch()
} catch (Exception ex) {
    ex.printStackTrace();
} finally {
    Global.terminate();
}
println "grind shell closed"