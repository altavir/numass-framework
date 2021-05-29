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

package hep.dataforge.fx.output

import hep.dataforge.context.Context
import hep.dataforge.context.Global
import hep.dataforge.io.HTMLOutput
import hep.dataforge.io.output.TextAttribute
import hep.dataforge.io.output.TextOutput
import hep.dataforge.meta.Meta
import javafx.scene.Parent
import javafx.scene.web.WebView
import kotlinx.html.body
import kotlinx.html.consumers.onFinalize
import kotlinx.html.dom.append
import kotlinx.html.dom.createHTMLDocument
import kotlinx.html.dom.serialize
import kotlinx.html.head
import kotlinx.html.html
import tornadofx.*

/**
 * An output wrapping html view
 */
class FXWebOutput(context: Context) : FXOutput(context), TextOutput {

    private val webView: WebView  by lazy { WebView() }


    override val view: Fragment by lazy {
        object : Fragment() {
            override val root: Parent = borderpane {
                center = webView
            }
        }
    }


    private val document = createHTMLDocument().html {
        head { }
        body { }
    }

    private val node = document.getElementsByTagName("body").item(0)

    private val appender = node.append.onFinalize { from, _ ->
        runLater {
            webView.engine.loadContent(from.ownerDocument.serialize(true))
        }
    }

    val out = HTMLOutput(Global, appender)

    override fun render(obj: Any, meta: Meta) = out.render(obj, meta)

    override fun renderText(text: String, vararg attributes: TextAttribute) = out.renderText(text, *attributes)
}

