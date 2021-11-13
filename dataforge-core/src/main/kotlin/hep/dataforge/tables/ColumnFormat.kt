package hep.dataforge.tables

import hep.dataforge.Named
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder
import hep.dataforge.meta.SimpleMetaMorph
import hep.dataforge.values.Value
import hep.dataforge.values.ValueType
import java.util.*
import java.util.stream.Stream

/**
 * Created by darksnake on 29-Dec-16.
 */
class ColumnFormat(meta: Meta) : SimpleMetaMorph(meta), Named {

    override val name: String
        get() = getString("name")

    /**
     * Return primary type. By default primary type is `STRING`
     *
     * @return
     */
    val primaryType: ValueType
        get() = ValueType.valueOf(getString("type", ValueType.STRING.name))

    /**
     * Get displayed title for this column. By default returns column name
     *
     * @return
     */
    val title: String
        get() = getString("title") { this.name }

    /**
     * @return
     */
    val tags: List<String>
        get() = Arrays.asList(*getStringArray(TAG_KEY))

    /**
     * Check if value is allowed by the format. It 'type' field of meta is empty then any type is allowed.
     *
     * @param value
     * @return
     */
    fun isAllowed(value: Value): Boolean {
        //TODO add complex analysis here including enum-values
        return !hasValue("type") || Arrays.asList(*getStringArray("type")).contains(value.type.name)
    }

    companion object {

        const val TAG_KEY = "tag"

        //    /**
        //     * mark column as optional so its value is replaced by {@code null} in table builder if it is not present
        //     */
        //    public static final String OPTIONAL_TAG = "optional";

        /**
         * Construct simple column format
         *
         * @param name
         * @param type
         * @return
         */
        fun build(name: String, type: ValueType, vararg tags: String): ColumnFormat {
            return ColumnFormat(MetaBuilder("column")
                .putValue("name", name)
                .putValue("type", type)
                .putValue(TAG_KEY, Stream.of(*tags).toList())
            )
        }

        /**
         * Create a new format instance with changed name. Returns argument if name is not changed
         *
         * @param name
         * @param columnFormat
         * @return
         */
        fun rename(name: String, columnFormat: ColumnFormat): ColumnFormat {
            return if (name == columnFormat.name) {
                columnFormat
            } else {
                ColumnFormat(columnFormat.toMeta().builder.setValue("name", name).build())
            }
        }
    }

}
