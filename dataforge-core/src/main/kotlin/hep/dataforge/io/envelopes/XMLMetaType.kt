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

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.io.envelopes

import hep.dataforge.io.MetaStreamReader
import hep.dataforge.io.MetaStreamWriter
import hep.dataforge.io.XMLMetaReader
import hep.dataforge.io.XMLMetaWriter

val xmlMetaType = XMLMetaType()

class XMLMetaType : MetaType {

    override val codes: List<Short> = listOf(0x584d, 0)//XM

    override val name: String = XML_META_TYPE

    override val reader: MetaStreamReader = XMLMetaReader()

    override val writer: MetaStreamWriter = XMLMetaWriter()

    override val fileNameFilter: (String) -> Boolean = { str -> str.toLowerCase().endsWith(".xml") }

    companion object {
        const val XML_META_TYPE = "XML"
    }

}
