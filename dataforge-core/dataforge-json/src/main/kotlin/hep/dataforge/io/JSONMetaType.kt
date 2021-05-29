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
package hep.dataforge.io

import hep.dataforge.io.envelopes.MetaType

class JSONMetaType : MetaType {
    override val codes: List<Short> = listOf(0x4a53, 1)//JS

    override val name: String = "JSON"

    override val reader: MetaStreamReader = JSONMetaReader

    override val writer: MetaStreamWriter = JSONMetaWriter

    override val fileNameFilter: (String) -> Boolean = { it.toLowerCase().endsWith(".json") }
}

val jsonMetaType = JSONMetaType()
