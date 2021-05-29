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

import hep.dataforge.context.Global
import hep.dataforge.io.HTMLOutput
import kotlinx.html.body
import kotlinx.html.consumers.onFinalize
import kotlinx.html.dom.append
import kotlinx.html.dom.createHTMLDocument
import kotlinx.html.dom.serialize
import kotlinx.html.head
import kotlinx.html.html
import org.junit.Test

class FXWebOutputTest {
    @Test
    fun testRendering() {

        val document = createHTMLDocument().html {
            head {  }
            body {  }
        }

        val node = document.getElementsByTagName("body").item(0)

        val appender = node.append.onFinalize { from, _ ->
            println(from.ownerDocument.serialize(true))
        }

        val out = HTMLOutput(Global,appender)

        out.render("this is my text")
        out.render("this is another text")
    }
}