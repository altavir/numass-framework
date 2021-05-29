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
import hep.dataforge.meta.Meta
import javafx.scene.Parent
import tornadofx.*

class FXDumbOutput(context:Context): FXOutput(context) {

    override val view: Fragment by lazy{
        object: Fragment() {
            override val root: Parent
                get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
        }
    }

    override fun render(obj: Any, meta: Meta) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}