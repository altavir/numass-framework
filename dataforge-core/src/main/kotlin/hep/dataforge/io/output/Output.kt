package hep.dataforge.io.output

import hep.dataforge.context.Context
import hep.dataforge.context.ContextAware
import hep.dataforge.context.FileReference
import hep.dataforge.meta.Meta
import java.io.OutputStream

/**
 * An interface for generic display and ouput capabilities.
 */
interface Output : ContextAware {
    /**
     * Display an object with given configuration. Throw an exception if object type not supported
     */
    fun render(obj: Any, meta: Meta = Meta.empty())

    companion object {
        const val TEXT_TYPE = "text"
        const val BINARY_TYPE = "binary"


        fun splitOutput(vararg outputs: Output): Output {
            val context = outputs.first().context
            return object : Output {
                override val context: Context
                    get() = context

                override fun render(obj: Any, meta: Meta) {
                    outputs.forEach { it.render(obj, meta) }
                }

            }
        }

        fun fileOutput(ref: FileReference): Output {
            return FileOutput(ref)
        }

        fun streamOutput(context: Context, stream: OutputStream): Output {
            return StreamOutput(context, stream)
        }
    }
}

/**
 * The object that knows best how it should be rendered
 */
interface SelfRendered {
    fun render(output: Output, meta: Meta)
}

/**
 * Custom renderer for specific type of object
 */
interface OutputRenderer {
    val type: String
    fun render(output: Output, obj: Any, meta: Meta)
}


val Output.stream: OutputStream
    get() = if(this is StreamOutput){
        this.stream
    } else{
        StreamConsumer(this)
    }