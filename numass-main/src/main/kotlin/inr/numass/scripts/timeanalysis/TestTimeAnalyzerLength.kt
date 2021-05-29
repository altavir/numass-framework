package inr.numass.scripts.timeanalysis

import hep.dataforge.buildContext
import hep.dataforge.fx.output.FXOutputManager
import hep.dataforge.meta.buildMeta
import hep.dataforge.plots.jfreechart.JFreeChartPlugin
import inr.numass.NumassPlugin
import inr.numass.data.analyzers.TimeAnalyzer
import inr.numass.data.api.NumassSet
import inr.numass.data.storage.NumassDirectory


fun main() {
    val context = buildContext("NUMASS", NumassPlugin::class.java, JFreeChartPlugin::class.java) {
        output = FXOutputManager()
        rootDir = "D:\\Work\\Numass\\sterile2017_05"
        dataDir = "D:\\Work\\Numass\\data\\2017_05"
    }

    val storage = NumassDirectory.read(context, "Fill_3")!!

    val loader = storage.provide("set_10", NumassSet::class.java).get()

    val point = loader.getPoints(18050.00).first()

    val analyzer = TimeAnalyzer()

    val meta = buildMeta("analyzer") {
        "t0" to 3000
        "inverted" to false
        //"chunkSize" to 5000
        //"mean" to TimeAnalyzer.AveragingMethod.ARITHMETIC
    }

    println(analyzer.analyze(point, meta))

    println(analyzer.getEventsWithDelay(point.firstBlock, meta ).count())
    println(point.events.count())
    println(point.firstBlock.events.count())

//    val time = point.events.asSequence().zipWithNext().map { (p, n) ->
//        n.timeOffset - p.timeOffset
//    }.filter { it > 0 }.sum()

    val time = analyzer.getEventsWithDelay(point.firstBlock, meta ).map { it.second }.filter { it > 0 }.sum()



//    val totalN = AtomicLong(0)
//    val totalT = AtomicLong(0)
//
//    analyzer.getEventsWithDelay(point.firstBlock, meta ).filter { pair -> pair.second >= 3000 }
//        .forEach { pair ->
//            totalN.incrementAndGet()
//            //TODO add progress listener here
//            totalT.addAndGet(pair.second)
//        }
//
//    val time = totalT.get()

    println(time / 1e9)

//
//    val cr = 80.0
//    val length = 5e9.toLong()
//    val num = 6
//    val dt = 6.5
//
//    val start = Instant.now()
//
//    val generator = SynchronizedRandomGenerator(JDKRandomGenerator(2223))
//
//    repeat(100) {
//
//        val point = (1..num).map {
//            Global.generate {
//                NumassGenerator
//                    .generateEvents(cr , rnd = generator)
////                    .withDeadTime { (dt * 1000).toLong() }
//                    .generateBlock(start.plusNanos(it * length), length)
//            }
//        }.join(Global) { blocks ->
//            SimpleNumassPoint.build(blocks, 12000.0)
//        }.get()
//
//
//        println(analyzer.analyze(point, meta))
//
//    }
}