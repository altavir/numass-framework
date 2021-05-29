package hep.dataforge.tables

import hep.dataforge.exceptions.NameNotFoundException
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder
import hep.dataforge.meta.MetaMorph
import hep.dataforge.values.Value
import hep.dataforge.values.ValueMap
import hep.dataforge.values.Values
import java.util.*

/**
 * Created by darksnake on 18-Apr-17.
 */
open class ListOfPoints(private val data: List<Values>) : MetaMorph, NavigableValuesSource {

    constructor(points: Iterable<Values>): this(points.toList())

    constructor(meta: Meta): this(buildFromMeta(meta))

    /**
     * {@inheritDoc}
     *
     * @param i
     * @return
     */
    override fun getRow(i: Int): Values {
        return data[i]
    }

    /**
     * {@inheritDoc}
     */
    @Throws(NameNotFoundException::class)
    override fun get(name: String, index: Int): Value {
        return this.data[index].getValue(name)
    }

    /**
     * {@inheritDoc}
     */
    override fun iterator(): MutableIterator<Values> {
        return data.toMutableList().iterator()
    }

    /**
     * {@inheritDoc}
     */
    override fun size(): Int {
        return data.size
    }


    override fun toMeta(): Meta {
        val dataNode = MetaBuilder("data")
        forEach { dp -> dataNode.putNode("point", dp.toMeta()) }
        return dataNode
    }

    override fun equals(other: Any?): Boolean {
        return other != null && javaClass == other.javaClass && (other as MetaMorph).toMeta() == this.toMeta()
    }

    override fun hashCode(): Int {
        var result = data.hashCode()
        result = 31 * result + data.hashCode()
        return result
    }

    companion object {

        fun buildFromMeta(annotation: Meta): List<Values> {
            val res = ArrayList<Values>()
            for (pointMeta in annotation.getMetaList("point")) {
                val map = HashMap<String, Value>()
                pointMeta.valueNames.forEach { key -> map[key] = pointMeta.getValue(key) }
                res.add(ValueMap(map))
            }
            return res
        }
    }
}
