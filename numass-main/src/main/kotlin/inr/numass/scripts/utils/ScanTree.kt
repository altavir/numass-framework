package inr.numass.scripts.utils

import hep.dataforge.context.Global
import hep.dataforge.io.XMLMetaWriter
import hep.dataforge.kodex.useValue
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder
import hep.dataforge.meta.MetaUtils
import hep.dataforge.meta.buildMeta
import hep.dataforge.storage.api.Storage
import inr.numass.data.storage.NumassDataLoader
import inr.numass.data.storage.NumassStorageFactory
import java.io.File
import java.nio.file.Paths

private fun createSummaryNode(storage: Storage): MetaBuilder {
    Global.logger.info("Reading content of shelf {}", storage.fullName)

    val builder = MetaBuilder("shelf")
            .setValue("name", storage.name)
            .setValue("path", storage.fullName)
    storage.shelves().filter { it.name.startsWith("Fill") }.forEach {
        builder.putNode(createSummaryNode(it))
    }
    storage.loaders().filterIsInstance(NumassDataLoader::class.java).forEach { set ->

        Global.logger.info("Reading content of set {}", set.fullName)

        val setBuilder = MetaBuilder("set")
                .setValue("name", set.name)
                .setValue("path", set.fullName)

        if (set.name.endsWith("bad")) {
            setBuilder.setValue("bad", true)
        }

        set.points.forEach { point ->
            val pointBuilder = MetaBuilder("point")
                    .setValue("index", point.index)
                    .setValue("hv", point.voltage)
                    .setValue("startTime", point.startTime)
//                    .setNode("meta", point.meta)

            point.meta.useValue("acquisition_time") {
                pointBuilder.setValue("length", it.double)
            }

            point.meta.useValue("events") {
                pointBuilder.setValue("count", it.list.stream().mapToInt { it.int }.sum())
            }

            setBuilder.putNode(pointBuilder)
        }
        builder.putNode(setBuilder)
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

    val path = Paths.get(directory)

    val output = File(directory, "summary.xml")
    output.createNewFile()


    val storage = NumassStorageFactory.buildLocal(Global, path, true, false)
    val summary = createSummaryNode(storage)

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