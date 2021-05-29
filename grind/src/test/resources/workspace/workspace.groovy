
// define context
context{
    name = "TEST"
    plugin "fx"
    properties{
        a = 4
        b = false
    }
}

task(hep.dataforge.grind.TestTask)