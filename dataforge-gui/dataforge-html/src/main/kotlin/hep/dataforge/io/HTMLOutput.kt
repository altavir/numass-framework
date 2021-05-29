/*
 * Copyright  2018 Alexander Nozik.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package hep.dataforge.io

import ch.qos.logback.classic.spi.ILoggingEvent
import hep.dataforge.childNodes
import hep.dataforge.context.Context
import hep.dataforge.description.NodeDescriptor
import hep.dataforge.description.ValueDescriptor
import hep.dataforge.io.envelopes.Envelope
import hep.dataforge.io.history.Record
import hep.dataforge.io.output.*
import hep.dataforge.meta.Meta
import hep.dataforge.meta.Metoid
import hep.dataforge.tables.Table
import hep.dataforge.values
import hep.dataforge.values.ValueType
import kotlinx.html.*
import org.w3c.dom.Node

open class HTMLOutput(override val context: Context, private val consumer: TagConsumer<*>) : Output, TextOutput {

    fun appendHTML(meta: Meta = Meta.empty(), action: TagConsumer<*>.() -> Unit) {
        if (meta.hasMeta("html")) {
            consumer.div(classes = meta.getString("html.class")) {
                consumer.action()
            }
        } else {
            consumer.action()
        }
    }

    override fun renderText(text: String, vararg attributes: TextAttribute) {
        appendHTML{
            span {
                attributes.forEach {
                    classes += when(it){
                        is TextColor -> "color: ${it.color}"
                        is TextStrong-> "font-weight: bold"
                        is TextEmphasis -> "font-style: italic"
                    }
                }
                +text
            }
        }
    }

    private fun TagConsumer<*>.meta(meta: Meta) {
        div(classes = "meta-node") {
            p {
                +meta.name
            }
            ul(classes = "meta-node-list") {
                meta.childNodes.forEach {
                    li {
                        meta(it)
                    }
                }
            }

            ul(classes = "meta-values-list") {
                meta.values.forEach {
                    li {
                        span {
                            style = "color:green"
                            +it.key
                        }
                        +": ${it.value}"
                    }
                }
            }
        }
    }

    private fun TagConsumer<*>.descriptor(descriptor: ValueDescriptor) {
        div(classes = "descriptor-value") {
            p(classes = "descripor-head") {
                span {
                    style = "color:red"
                    +descriptor.name
                }
                if (descriptor.required) {
                    span {
                        style = "color:cyan"
                        +"(*) "
                    }
                }
                descriptor.type.firstOrNull()?.let {
                    +"[it]"
                }

                if (descriptor.hasDefault()) {
                    val def = descriptor.default
                    if (def.type == ValueType.STRING) {
                        +" = "
                        span {
                            style = "color:green"
                            +"\"${def.string}\""
                        }
                    } else {
                        +" = "
                        span {
                            style = "color:green"
                            +def.string
                        }
                    }
                }
                p(classes = "descriptor-info") {
                    +descriptor.info
                }
            }
        }
    }

    private fun TagConsumer<*>.descriptor(descriptor: NodeDescriptor) {
        div(classes = "descriptor-node") {
            p(classes = "descriptor-header") {
                +descriptor.name
                if (descriptor.required) {
                    span {
                        style = "color:cyan"
                        +"(*) "
                    }
                }
            }
            p(classes = "descriptor-info") {
                descriptor.info
            }
            div(classes = "descriptor-body") {
                descriptor.childrenDescriptors().also {
                    if (it.isNotEmpty()) {
                        ul {
                            it.forEach { _, value ->
                                li { descriptor(value) }
                            }
                        }
                    }
                }

                descriptor.valueDescriptors().also {
                    if (it.isNotEmpty()) {
                        ul {
                            it.forEach { _, value ->
                                li { descriptor(value) }
                            }
                        }
                    }
                }
            }
        }
    }


    override fun render(obj: Any, meta: Meta) {
        when (obj) {
            is SelfRendered -> {
                obj.render(this, meta)
            }
            is Node -> render(obj)
            is Meta -> {
                appendHTML(meta) {
                    div(classes = "meta") {
                        meta(obj)
                    }
                }
            }
            is Table -> {
                appendHTML(meta) {
                    table {
                        //table header
                        tr {
                            obj.format.names.forEach {
                                th { +it }
                            }
                        }
                        obj.rows.forEach {values->
                            tr {
                                obj.format.names.forEach {
                                    td {
                                        +values[it].string
                                    }
                                }
                            }
                        }

                    }
                }
            }
            is Envelope -> {
                appendHTML(meta) {
                    div(classes = "envelope-meta") {
                        meta(obj.meta)
                    }
                    div(classes = "envelope-data") {
                        +obj.data.toString()
                    }
                }
            }
            is ILoggingEvent -> {
                appendHTML(meta) {
                    p {
                        //TODO fix logger
                        obj.message
                    }
                }
            }
            is CharSequence, is Record -> {
                appendHTML(meta) {
                    pre {
                        +obj.toString()
                    }
                }
            }
            is ValueDescriptor -> {
                appendHTML(meta) {
                    descriptor(obj)
                }
            }
            is NodeDescriptor -> {
                appendHTML(meta) {
                    descriptor(obj)
                }
            }
            is Metoid -> { // render custom metoid
                val renderType = obj.meta.getString("@output.type", "@default")
                context.findService(OutputRenderer::class.java) { it.type == renderType }
                        ?.render(this@HTMLOutput, obj, meta)
                        ?: appendHTML(meta) { meta(obj.meta) }
            }
        }
    }

    companion object {
        const val HTML_MODE = "html"
    }
}

