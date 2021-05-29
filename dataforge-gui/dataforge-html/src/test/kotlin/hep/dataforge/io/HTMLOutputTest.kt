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

import hep.dataforge.context.Global
import hep.dataforge.description.DescriptorBuilder
import hep.dataforge.meta.buildMeta
import kotlinx.html.stream.appendHTML
import org.junit.Test

class HTMLOutputTest{
    @Test
    fun renderTest(){
        val output = HTMLOutput(Global, System.out.appendHTML(prettyPrint = true)).apply {
            render(
                DescriptorBuilder("test")
                        .value("a", info = "a value")
                        .build()
            )
            render("Some text", buildMeta { "html.class" to "ddd" })
        }

        output.render("another text")
    }
}