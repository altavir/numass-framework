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

package hep.dataforge.fx.meta

import hep.dataforge.fx.dfIconView
import hep.dataforge.meta.Meta
import hep.dataforge.toList
import hep.dataforge.values.Value
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeSortMode
import javafx.scene.control.TreeTableView
import tornadofx.*
import java.util.stream.Stream

internal sealed class Item {
    abstract val titleProperty: StringProperty
    abstract val valueProperty: StringProperty
}

internal class MetaItem(val meta: Meta) : Item() {
    override val titleProperty = SimpleStringProperty(meta.name)
    override val valueProperty: StringProperty = SimpleStringProperty("")
}

internal class ValueItem(name: String, val value: Value) : Item() {
    override val titleProperty = SimpleStringProperty(name)
    override val valueProperty: StringProperty = SimpleStringProperty(value.string)
}

open class MetaViewer(val meta: Meta, title: String = "Meta viewer: ${meta.name}") : Fragment(title, dfIconView) {
    override val root = borderpane {
        center {
            treetableview<Item> {
                isShowRoot = false
                root = TreeItem(MetaItem(meta))
                populate {
                    val value: Item = it.value
                    when (value) {
                        is MetaItem -> {
                            val meta = value.meta
                            Stream.concat(
                                    meta.nodeNames.flatMap { meta.getMetaList(it).stream() }.map { MetaItem(it) },
                                    meta.valueNames.map { ValueItem(it, meta.getValue(it)) }
                            ).toList()
                        }
                        is ValueItem -> null
                    }
                }
                root.isExpanded = true
                sortMode = TreeSortMode.ALL_DESCENDANTS
                columnResizePolicy = TreeTableView.CONSTRAINED_RESIZE_POLICY
                column("Name", Item::titleProperty)
                column("Value", Item::valueProperty)
            }
        }
    }
}