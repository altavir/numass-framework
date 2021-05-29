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
package hep.dataforge.context

import hep.dataforge.description.ValueDef
import hep.dataforge.description.ValueDefs
import hep.dataforge.meta.*

/**
 * The tag which contains information about name, group and version of some
 * object. It also could contain any complex rule to define version ranges
 *
 * @author Alexander Nozik
 */
//@ValueDef(name = "role", multiple = true,info = "The list of roles this plugin implements")
//@ValueDef(name = "priority", type = "NUMBER", info = "Plugin load priority. Used for plugins with the same role")
@ValueDefs(
        ValueDef(key = "group", def = ""),
        ValueDef(key = "name", required = true)
)
class PluginTag(meta: Meta) : SimpleMetaMorph(meta) {

    val name by meta.stringValue()

    val group by meta.stringValue(def = "")

    constructor(name: String, group: String = "hep.dataforge", description: String? = null, version: String? = null, vararg dependsOn: String) : this(
            buildMeta("plugin") {
                "name" to name
                "group" to group
                description?.let { "description" to it }
                version?.let { "version" to it }
                if (dependsOn.isNotEmpty()) {
                    dependsOn.let { "dependsOn" to it }
                }
            }
    )

    /**
     * Check if given tag is compatible (in range) of this tag
     *
     * @param otherTag
     * @return
     */
    fun matches(otherTag: PluginTag): Boolean {
        return matchesName(otherTag) && matchesGroup(otherTag)
    }

    private fun matchesGroup(otherTag: PluginTag): Boolean {
        return this.group.isEmpty() || this.group == otherTag.group
    }

    private fun matchesName(otherTag: PluginTag): Boolean {
        return this.name == otherTag.name
    }


    /**
     * Build standard string representation of plugin tag
     * `group.name[version]`. Both group and version could be empty.
     *
     * @return
     */
    override fun toString(): String {
        var theGroup = group
        if (!theGroup.isEmpty()) {
            theGroup += ":"
        }

        return theGroup + name
    }

    companion object {

        /**
         * Build new PluginTag from standard string representation
         *
         * @param tag
         * @return
         */
        fun fromString(tag: String): PluginTag {
            val sepIndex = tag.indexOf(":")
            return if (sepIndex >= 0) {
                PluginTag(group = tag.substring(0, sepIndex), name = tag.substring(sepIndex + 1))
            } else {
                PluginTag(tag)
            }
        }

        /**
         * Resolve plugin tag either from [PluginDef] annotation or Plugin instance.
         *
         * @param type
         * @return
         */
        fun resolve(type: Class<out Plugin>): PluginTag {
            //if definition is present
            return if (type.isAnnotationPresent(PluginDef::class.java)) {
                val builder = MetaBuilder("tag")
                val def = type.getAnnotation(PluginDef::class.java)
                builder.putValue("group", def.group)
                builder.putValue("name", def.name)
                builder.putValue("description", def.info)
                builder.putValue("version", def.version)
                for (dep in def.dependsOn) {
                    builder.putValue("dependsOn", dep)
                }
                PluginTag(builder)
            } else { //getting plugin instance to find tag
                PluginTag.fromString(type.simpleName ?: "undefined")
            }
        }

    }
}
