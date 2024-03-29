rootProject.name = "numass"

include(":dataforge-core") // core classes and data structures
include(":dataforge-core:dataforge-json") // json meta converter
//include(":dataforge-core:osgi") // osgi framework for easy plugin delivery

include(":dataforge-plots") // plot library
include(":dataforge-plots:plots-viewer") // Viewer application for exported plots
include(":dataforge-plots:plots-jfc")

include(":dataforge-maths") // mathematical tools

include(":grind") // GRoovy INteractive Dataforge
include(":grind:groovymath") // groovy mathematical extension
include(":grind:grind-terminal")

//include("kodex-server") // ktor based http server
//include(":kodex:dataforge-server") // server base

include(":dataforge-gui") // javafx based gui
include(":dataforge-gui:dataforge-html") // HTML tools
include(":dataforge-gui:gui-demo") // javafx based gui
include(":dataforge-gui:gui-workspace") // javafx based gui

include(":dataforge-stat") // fitting tools
include(":dataforge-stat:dataforge-minuit") // MINUIT plugin for fitting

include(":dataforge-control") // tools for measurement and control applications

include(":dataforge-storage") // storage2 module

//include(":site")//the site

include(":numass-control")
include(":numass-control:cryotemp")
include(":numass-control:magnet")
include(":numass-control:msp")
include(":numass-control:vac")
//include(":numass-control:control-room")
include(":numass-control:dante")
include(":numass-control:gun")
//
include(":numass-main")
//
include(":numass-core")

include("numass-core:numass-data-api")
include("numass-core:numass-data-proto")
include("numass-core:numass-signal-processing")

include(":numass-viewer")

