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
package inr.numass.viewer;

import hep.dataforge.computation.ProgressCallback;
import hep.dataforge.exceptions.StorageException;
import inr.numass.storage.NumassData;
import inr.numass.storage.NumassStorage;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.input.MouseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 *
 * @author darksnake
 */
public class NumassLoaderTreeBuilder {

    public void build(ProgressCallback callback,
                      TreeTableView<TreeItemValue> numassLoaderDataTree,
                      NumassStorage rootStorage,
                      Consumer<NumassData> numassViewBuilder) throws StorageException {

        TreeItem<TreeItemValue> root = buildNode(rootStorage, numassViewBuilder, callback);
        root.setExpanded(true);

        Platform.runLater(() -> {
            numassLoaderDataTree.setRoot(root);

            TreeTableColumn<TreeItemValue, String> numassLoaderNameColumn = new TreeTableColumn<>("name");

            numassLoaderNameColumn.setCellValueFactory(
                    (TreeTableColumn.CellDataFeatures<TreeItemValue, String> param) -> new SimpleStringProperty(param.getValue().getValue().getName()));

            TreeTableColumn<TreeItemValue, String> numassLoaderTimeColumn = new TreeTableColumn<>("time");
            numassLoaderTimeColumn.setCellValueFactory(
                    (TreeTableColumn.CellDataFeatures<TreeItemValue, String> param) -> new SimpleStringProperty(param.getValue().getValue().getTime()));

            TreeTableColumn<TreeItemValue, String> nummassLoaderDescriptionColumn = new TreeTableColumn<>("description");
            nummassLoaderDescriptionColumn.setCellValueFactory(
                    (TreeTableColumn.CellDataFeatures<TreeItemValue, String> param) -> new SimpleStringProperty(param.getValue().getValue().getDescription()));

            numassLoaderDataTree.getColumns().setAll(numassLoaderNameColumn, numassLoaderTimeColumn, nummassLoaderDescriptionColumn);

            numassLoaderNameColumn.setSortType(TreeTableColumn.SortType.ASCENDING);
            numassLoaderDataTree.getSortOrder().addAll(numassLoaderTimeColumn, numassLoaderNameColumn);
            numassLoaderDataTree.addEventHandler(MouseEvent.MOUSE_CLICKED, (MouseEvent e) -> {
                if (e.getClickCount() == 2) {
                    TreeItemValue value = numassLoaderDataTree.getFocusModel().getFocusedCell().getTreeItem().getValue();
                    if (value.isLoader()) {
                        numassViewBuilder.accept(value.getLoader());
                    }
                }
            });
            numassLoaderTimeColumn.setVisible(false);
            nummassLoaderDescriptionColumn.setVisible(false);
        });

    }

    private TreeItem<TreeItemValue> buildNode(NumassStorage storage,
                                              Consumer<NumassData> numassViewBuilder, ProgressCallback callback) throws StorageException {
        TreeItem<TreeItemValue> node = new TreeItem<>(buildValue(storage));
        node.getChildren().setAll(buildChildren(storage, numassViewBuilder, callback));
        return node;
    }

    private Logger getLogger() {
        return LoggerFactory.getLogger(getClass());
    }

    private List<TreeItem<TreeItemValue>> buildChildren(NumassStorage storage,
                                                        Consumer<NumassData> numassViewBuilder, ProgressCallback callback) throws StorageException {
        List<TreeItem<TreeItemValue>> list = new ArrayList<>();

        storage.shelves().values().stream().forEach(subStorage -> {
            if (subStorage instanceof NumassStorage) {
                try {
                    NumassStorage numassSubStorage = (NumassStorage) subStorage;
                    TreeItem<TreeItemValue> childNode = buildNode(numassSubStorage, numassViewBuilder, callback);
                    if (!childNode.isLeaf()) {
                        list.add(buildNode(numassSubStorage, numassViewBuilder, callback));
                    }
                } catch (StorageException ex) {
                    getLogger().error("Error while loading numass storage node", ex);
                }
            }
        });

        callback.updateMessage("Building storage " + storage.getName());
        callback.setProgress(0);
        callback.setMaxProgress(storage.loaders().size());
        storage.loaders().values().stream()
                .forEach(loader -> {
                    if (loader instanceof NumassData) {
                        callback.updateMessage("Building numass data loader " + loader.getName());
                        NumassData numassLoader = (NumassData) loader;
                        TreeItem<TreeItemValue> numassLoaderTreeItem = new TreeItem<>(buildValue(numassLoader));
                        list.add(numassLoaderTreeItem);
                    }
                    callback.increaseProgress(1);
                });

        callback.updateMessage("Loading legacy DAT files");
        callback.setProgress(0);
        List<NumassData> legacyFiles = storage.legacyFiles();
        callback.setMaxProgress(legacyFiles.size());
        //adding legacy data files
        for (NumassData legacyDat : legacyFiles) {
            callback.updateMessage("Loading numass DAT file " + legacyDat.getName());
            TreeItem<TreeItemValue> numassLoaderTreeItem = new TreeItem<>(buildValue(legacyDat));
            callback.increaseProgress(1);
            list.add(numassLoaderTreeItem);
        }

        return list;
    }

    private TreeItemValue buildValue(final NumassStorage storage) {
        return new TreeItemValue() {

            @Override
            public String getDescription() {
                return storage.getDescription();
            }

            @Override
            public NumassData getLoader() {
                return null;
            }

            @Override
            public String getName() {
                return storage.getName();
            }

            @Override
            public NumassStorage getStorage() {
                return storage;
            }

            @Override
            public String getTime() {
                if (getStorage().meta().hasValue("file.timeModified")) {
                    return getStorage().meta().getValue("file.timeModified").stringValue();
                } else {
                    return null;
                }
            }

            @Override
            public boolean isLoader() {
                return false;
            }
        };
    }

    private TreeItemValue buildValue(final NumassData loader) {
        return new TreeItemValue() {

            @Override
            public String getDescription() {
                return loader.getDescription();
            }

            @Override
            public NumassData getLoader() {
                return loader;
            }

            @Override
            public String getName() {
//                if (loader.meta().getBoolean("reverse", false)) {
//                    return loader.getName() + " \u2191";
//                } else {
//                    return loader.getName();
//                }
                 return loader.getName();
            }

            @Override
            public NumassStorage getStorage() {
                return null;
            }

            @Override
            public String getTime() {
                Instant startTime = loader.startTime();
                if (startTime == null || startTime.equals(Instant.EPOCH)) {
                    return "";
                } else {
                    return loader.startTime().toString();
                }
            }

            @Override
            public boolean isLoader() {
                return true;
            }
        };
    }

    public interface TreeItemValue {

        String getName();

        String getTime();

        String getDescription();

        NumassData getLoader();

        NumassStorage getStorage();

        boolean isLoader();
    }
}
