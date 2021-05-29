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

package hep.dataforge.names

import hep.dataforge.exceptions.NamingException

/**
 * Created by darksnake on 26-Aug-16.
 */
internal class EmptyName : Name {

    override val query: String
        get() = ""

    override val length: Int
        get() = 0

    override val first: Name
        get() = this

    override val last: Name
        get() = this

    override val tokens: List<Name>
        get() = emptyList()

    override fun isEmpty(): Boolean = true

    override fun hasQuery(): Boolean {
        return false
    }

    override fun ignoreQuery(): Name {
        return this
    }

    override fun cutFirst(): Name {
        throw NamingException("Can not cut name token")
    }

    override fun cutLast(): Name {
        throw NamingException("Can not cut name token")
    }

    override fun entry(): String {
        return ""
    }


    override fun asArray(): Array<String> {
        return arrayOf()
    }

    override fun toString(): String {
        return ""
    }

    override val unescaped: String
        get() {
            return ""
        }
}
