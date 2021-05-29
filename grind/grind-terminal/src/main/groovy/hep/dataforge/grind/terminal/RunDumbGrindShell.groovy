package hep.dataforge.grind.terminal

import hep.dataforge.context.Global

/**
 * Created by darksnake on 05-Nov-16.
 */
println "DataForge grind shell"
try {
    GrindTerminal.dumb().launch()
} catch (Exception ex) {
    ex.printStackTrace();
} finally {
    Global.instance().close();
}
println "grind shell closed"