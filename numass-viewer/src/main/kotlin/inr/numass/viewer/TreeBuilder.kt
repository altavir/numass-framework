/* 
 * Copyright 2015 Alexander Nozik.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package inr.numass.viewer

/**

 * @author darksnake
 */
class TreeBuilder {
//    @Throws(StorageException::class)
//    fun build(callback: Work,
//              numassLoaderDataTree: TreeTableView<Item>,
//              rootStorage: NumassStorage,
//              numassViewBuilder: (NumassData) -> Unit) {
//
//        val root = buildNode(rootStorage, numassViewBuilder, callback)
//        root.isExpanded = true
//
//        Platform.runLater {
//            numassLoaderDataTree.root = root
//
//            val numassLoaderNameColumn = TreeTableColumn<Item, String>("name")
//
//            numassLoaderNameColumn.setCellValueFactory { param: TreeTableColumn.CellDataFeatures<Item, String> -> SimpleStringProperty(param.value.value.name) }
//
//            val numassLoaderTimeColumn = TreeTableColumn<Item, String>("time")
//            numassLoaderTimeColumn.setCellValueFactory { param: TreeTableColumn.CellDataFeatures<Item, String> -> SimpleStringProperty(param.value.value.time) }
//
//            val nummassLoaderDescriptionColumn = TreeTableColumn<Item, String>("description")
//            nummassLoaderDescriptionColumn.setCellValueFactory { param: TreeTableColumn.CellDataFeatures<Item, String> -> SimpleStringProperty(param.value.value.description) }
//
//            numassLoaderDataTree.columns.setAll(numassLoaderNameColumn, numassLoaderTimeColumn, nummassLoaderDescriptionColumn)
//
//            numassLoaderNameColumn.sortType = TreeTableColumn.SortType.ASCENDING
//            numassLoaderDataTree.sortOrder.addAll(numassLoaderTimeColumn, numassLoaderNameColumn)
//            numassLoaderDataTree.addEventHandler(MouseEvent.MOUSE_CLICKED) { e: MouseEvent ->
//                if (e.clickCount == 2) {
//                    val value = numassLoaderDataTree.focusModel.focusedCell.treeItem.value
//                    if (value.isLoader) {
//                        numassViewBuilder(value.loader)
//                    }
//                }
//            }
//            numassLoaderTimeColumn.isVisible = false
//            nummassLoaderDescriptionColumn.setVisible(false)
//        }
//
//    }
//
//    @Throws(StorageException::class)
//    private fun buildNode(storage: NumassStorage, numassViewBuilder: (NumassData) -> Unit, callback: Work): TreeItem<Item> {
//        val node = TreeItem(NumassDataItem(storage))
//        node.children.setAll(buildChildren(storage, numassViewBuilder, callback))
//        return node
//    }
//
//    private val logger: Logger
//        get() = LoggerFactory.getLogger(javaClass)
//
//    @Throws(StorageException::class)
//    private fun buildChildren(storage: NumassStorage, numassViewBuilder: (NumassData) -> Unit, callback: Work): List<TreeItem<Item>> {
//        val list = ArrayList<TreeItem<Item>>()
//
//        storage.shelves().stream().forEach { subStorage ->
//            if (subStorage is NumassStorage) {
//                try {
//                    val numassSubStorage = subStorage
//                    val childNode = buildNode(numassSubStorage, numassViewBuilder, callback)
//                    if (!childNode.isLeaf) {
//                        list.add(buildNode(numassSubStorage, numassViewBuilder, callback))
//                    }
//                } catch (ex: StorageException) {
//                    logger.error("Error while loading numass storage node", ex)
//                }
//
//            }
//        }
//
//        callback.status = "Building storage " + storage.name
//        callback.progress = 0.0
//        callback.maxProgress = storage.loaders().size.toDouble()
//        storage.loaders().stream()
//                .forEach { loader ->
//                    if (loader is NumassData) {
//                        callback.status = "Building numass data loader " + loader.getName()
//                        val numassLoaderTreeItem = TreeItem(NumassDataItem(loader))
//                        list.add(numassLoaderTreeItem)
//                    }
//                    callback.increaseProgress(1.0)
//                }
//
//        callback.status = "Loading legacy DAT files"
//        callback.progress = 0.0
//        val legacyFiles = storage.legacyFiles()
//        callback.maxProgress = legacyFiles.size.toDouble()
//        //adding legacy data files
//        for (legacyDat in legacyFiles) {
//            callback.status = "Loading numass DAT file " + legacyDat.name
//            val numassLoaderTreeItem = TreeItem(NumassDataItem(legacyDat))
//            callback.increaseProgress(1.0)
//            list.add(numassLoaderTreeItem)
//        }
//
//        return list
//    }
//
}
