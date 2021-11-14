package inr.numass.scripts.utils

import hep.dataforge.context.Global
import hep.dataforge.io.XMLMetaWriter
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder
import hep.dataforge.meta.MetaUtils
import hep.dataforge.meta.buildMeta
import hep.dataforge.storage.Storage
import hep.dataforge.useValue
import inr.numass.data.storage.NumassDataLoader
import inr.numass.data.storage.NumassDirectory
import kotlinx.coroutines.runBlocking
import java.io.File

private suspend fun createSummaryNode(storage: Storage): MetaBuilder {
    Global.logger.info("Reading content of shelf {}", storage.fullName)

    val builder = MetaBuilder("shelf")
            .setValue("name", storage.name)
            .setValue("path", storage.fullName)

    storage.getChildren().forEach { element ->
        if(element is Storage && element.name.startsWith("Fill")){
            builder.putNode(createSummaryNode(element))
        } else if(element is NumassDataLoader){
            Global.logger.info("Reading content of set {}", element.fullName)

            val setBuilder = MetaBuilder("set")
                    .setValue("name", element.name)
                    .setValue("path", element.fullName)

            if (element.name.endsWith("bad")) {
                setBuilder.setValue("bad", true)
            }

            element.points.forEach { point ->
                val pointBuilder = MetaBuilder("point")
                        .setValue("index", point.index)
                        .setValue("hv", point.voltage)
                        .setValue("startTime", point.startTime)
//                    .setNode("meta", point.meta)

                point.meta.useValue("acquisition_time") {
                    pointBuilder.setValue("length", it.double)
                }

                point.meta.useValue("events") { value ->
                    pointBuilder.setValue("count", value.list.stream().mapToInt { it.int }.sum())
                }

                setBuilder.putNode(pointBuilder)
            }
            builder.putNode(setBuilder)
        }
    }
    return builder
}

fun calculateStatistics(summary: Meta, hv: Double): Meta {
    var totalLength = 0.0
    var totalCount = 0L
    MetaUtils.nodeStream(summary).map { it.second }.filter { it.name == "point" && it.getDouble("hv") == hv }.forEach {
        totalCount += it.getInt("count")
        totalLength += it.getDouble("length")
    }
    return buildMeta("point") {
        "hv" to hv
        "time" to totalLength
        "count" to totalCount
    }
}

fun main(args: Array<String>) {
    val directory = if (args.isNotEmpty()) {
        args.first()
    } else {
        ""
    }


    val output = File(directory, "summary.xml")
    output.createNewFile()


    val storage = NumassDirectory.read(Global, directory) as Storage
    val summary = runBlocking { createSummaryNode(storage)}

    Global.logger.info("Writing output meta")
    output.outputStream().use {
        XMLMetaWriter().write(it, summary)
    }
    Global.logger.info("Calculating statistics")
    val statistics = MetaBuilder("statistics")
    (14000..18600).step(100).map { it.toDouble() }.forEach {
        statistics.putNode(calculateStatistics(summary, it))
    }

    File(directory, "statistics.xml").outputStream().use {
        XMLMetaWriter().write(it, statistics)
    }

}