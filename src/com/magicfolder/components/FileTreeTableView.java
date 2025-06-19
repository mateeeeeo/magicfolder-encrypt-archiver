package com.magicfolder.components;

import com.magicfolder.DictFile;
import com.magicfolder.FileNode;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTreeTableCell;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.util.converter.DefaultStringConverter;
import com.magicfolder.helpers.SecureArchive;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class FileTreeTableView extends TreeTableView<FileNode> {

    private static final DataFormat ARCHIVE_FORMAT = new DataFormat("application/x-archive-file");
    private Mode mode = Mode.CREATE_ARCHIVE;
    private SecureArchive folder;
    private List<FileNode> files;
    private TreeTableColumn<FileNode, String> fileNameCol;
    private int lastFileId = 0;

    public enum Mode {
        CREATE_ARCHIVE, EDIT_ARCHIVE
    }

    public FileTreeTableView() {
        super();
        this.files = new ArrayList<>();
        init();
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public SecureArchive getFolder() {
        return folder;
    }

    public void setFolder(SecureArchive folder) {
        this.folder = folder;
    }

    public void setFiles(List<FileNode> files) {
        this.files = files;
    }

    private TreeItem<FileNode> removeNodeById(TreeItem<FileNode> root, String id) {
        for (var c : root.getChildren()) {
            if (Objects.equals(c.getValue().getId(), id)) {
                root.getChildren().remove(c);
                System.out.println("Removing " + id);
                return c;
            }
            var removed = removeNodeById(c, id);
            if (removed != null) {
                return removed;
            }
        }
        return null;
    }


    private TreeItem<FileNode> osDirToTree(String path) {
        Path start = Paths.get(path);

        try {
            var visitor = new SimpleFileVisitor<Path>() {
                public TreeItem<FileNode> root = null;
                public TreeItem<FileNode> current = null;
                public String currPath = "";

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    System.out.println("Visited file: " + file);

                    current.getChildren().add(new TreeItem<>(new FileNode(new File(String.valueOf(file)))));
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    System.out.println("Visited directory: " + dir);
                    if (root == null) {
                        root = new TreeItem<>(new FileNode(new File(String.valueOf(dir)), true));
                        current = root;
                        currPath = String.valueOf(dir);
                        return FileVisitResult.CONTINUE;
                    }

                    var child = new TreeItem<>(new FileNode(new File(String.valueOf(dir)), true));
                    current.getChildren().add(child);

                    current = child;
                    currPath = String.valueOf(dir);

                    return FileVisitResult.CONTINUE;
                }
            };
            Files.walkFileTree(start, visitor);
            System.out.println(visitor.root);
            return visitor.root;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void addFile(File file, TreeItem<FileNode> parent) {
        if (parent == null) {
            parent = super.getRoot();
        } else if (!parent.getValue().isDir()) {
            parent = parent.getParent();
        }

        if (file.isDirectory()) {
            var newTreeRoot = osDirToTree(file.getPath());
            // connect tree to existing
            parent.getChildren().add(newTreeRoot);
        } else {
            FileNode newFile = new FileNode(file);
            TreeItem<FileNode> fileNode = new TreeItem<>(newFile);
            parent.getChildren().add(fileNode);
        }
    }

    public void init() {
        TreeTableColumn<FileNode, Long> sizeCol = new TreeTableColumn<>("Size");
        TreeTableColumn<FileNode, String> lastModifiedCol = new TreeTableColumn<>("Last Modified");

        setEditable(true);

        this.fileNameCol = new TreeTableColumn<>("Filename");
        fileNameCol.setCellFactory(col -> {
            TreeTableCell<FileNode, String> cell = new TextFieldTreeTableCell<>(new DefaultStringConverter());
            cell.setPickOnBounds(true);
            cell.setMouseTransparent(false);
            cell.setFocusTraversable(true);

            cell.setOnMouseClicked(event -> {
                TreeTableCell<FileNode, ?> clickedCell = getCell(event.getPickResult().getIntersectedNode());
                if (clickedCell.getTableRow().getTreeItem() == null)
                    getSelectionModel().clearSelection();
                event.consume();
            });

            cell.setOnDragOver(event -> {
                super.getSelectionModel().clearSelection();

                TreeItem<FileNode> item = cell.getTreeTableRow().getTreeItem();
                if (item != null && item.getValue().isDir()) {
                    super.getSelectionModel().select(item);
                }

                if (event.getDragboard().hasFiles() || event.getDragboard().hasContent(ARCHIVE_FORMAT)) {
                    event.acceptTransferModes(TransferMode.COPY);
                }
                event.consume();
            });

            cell.setOnDragDetected(event -> {
                TreeItem<FileNode> item = cell.getTreeTableRow().getTreeItem();
                if (item == null) return;
                FileNode file = item.getValue();

                Dragboard db = cell.startDragAndDrop(TransferMode.COPY);
                ClipboardContent content = new ClipboardContent();

                if (!item.getValue().isDir() && this.mode == Mode.EDIT_ARCHIVE) {
                    File extractedFile = this.folder.extractToTemp(file);
                    if (extractedFile != null && extractedFile.exists()) {
                        content.putFiles(List.of(extractedFile));
                    }
                }
                content.put(ARCHIVE_FORMAT, file.getId());
                db.setContent(content);
            });

            return cell;
        });

        fileNameCol.setCellValueFactory(new TreeItemPropertyValueFactory("name"));
        fileNameCol.setOnEditCancel(event -> onEditCancel(event));
        fileNameCol.setOnEditCommit(event -> onEditCommit(event));
        sizeCol.setCellValueFactory(new TreeItemPropertyValueFactory("size"));
        lastModifiedCol.setCellValueFactory(new TreeItemPropertyValueFactory("lastModified"));

        super.setShowRoot(false);
        super.getColumns().addAll(fileNameCol, sizeCol, lastModifiedCol);

        super.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY);
        super.getColumns().get(super.getColumns().size() - 1)
                .setStyle("-fx-border-width: 1px 0 1px 0");

        super.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles() || event.getDragboard().hasContent(ARCHIVE_FORMAT)) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        super.setOnDragDropped(event -> {
            System.out.println("Table wide dropped");
            TreeTableCell<FileNode, ?> cell = getCell(event.getPickResult().getIntersectedNode());
            if (cell == null) {
                event.setDropCompleted(false);
                event.consume();
                return;
            }

            TreeItem<FileNode> targetItem = cell.getTreeTableRow().getTreeItem();

            // Fallback: drop into root if no row
            if (targetItem == null || !targetItem.getValue().isDir()) {
                targetItem = super.getRoot();
            }

            Dragboard db = event.getDragboard();

            if (event.getDragboard().hasContent(ARCHIVE_FORMAT)) {
                var dbContent = db.getContent(ARCHIVE_FORMAT);
                TreeItem<FileNode> removedItem = removeNodeById(super.getRoot(), (String) dbContent);
                targetItem.getChildren().add(removedItem);
            } else if (db.hasFiles()) {
                for (File file : db.getFiles()) {
                    this.addFile(file, targetItem);
                }
                event.setDropCompleted(true);
            } else {
                event.setDropCompleted(false);
            }

            event.consume();
            super.refresh();
        });
    }

    private static TreeTableCell<FileNode, ?> getCell(Node node) {
        while (node != null && !(node instanceof TreeTableCell)) {
            node = node.getParent();
        }
        return (TreeTableCell<FileNode, ?>) node;
    }

    private void onEditCancel(TreeTableColumn.CellEditEvent<FileNode, String> event) {
        FileNode item = event.getRowValue().getValue();

        String newVal = event.getNewValue() != null ? event.getNewValue() : "";
        String oldVal = event.getOldValue() != null ? event.getOldValue() : "";

        if (newVal.trim().isEmpty()) {
            if (!oldVal.trim().isEmpty()) {
                item.setName(oldVal);
            } else {
                item.setName("Untitled Folder");
            }
        }
    }

    private void onEditCommit(TreeTableColumn.CellEditEvent<FileNode, String> event) {
        FileNode item = event.getRowValue().getValue();

        String newVal = event.getNewValue() != null ? event.getNewValue() : "";

        if (!newVal.trim().isEmpty()) {
            item.setName(newVal);
            item.nameProperty().set(newVal);
        } else {
            item.setName("Untitled Folder");
            item.nameProperty().set("Untitled Folder");

        }
    }

    public void renameSelected() {
        System.out.println("Rename");
        setEditable(true);
        System.out.println(isEditable());
        TreeTablePosition<FileNode, ?> pos = getFocusModel().getFocusedCell();
        edit(pos.getRow(), pos.getTableColumn());
    }

    public void addDir() {
        TreeItem<FileNode> parent = getSelectionModel().getSelectedItem();
         if(parent == null) {
            parent = super.getRoot();
        } else if (!parent.getValue().isDir()) {
            parent = parent.getParent();
        }

        FileNode item = new FileNode();
        TreeItem<FileNode> treeItem = new TreeItem<>(item);
        parent.getChildren().add(treeItem);

        parent.setExpanded(true);
        treeItem.setExpanded(true);

        int row = getRow(treeItem);

        super.edit(row, fileNameCol);
        lastFileId++;
    }

    public void deleteDir() {
        TreeItem<FileNode> selected = super.getSelectionModel().getSelectedItem();
        if (selected != null && selected.getParent() != null) {
            selected.getParent().getChildren().remove(selected);
        }

        lastFileId--;
    }

    public void displayFiles() {
        if (!files.isEmpty()) {
            TreeItem<FileNode> root = new TreeItem<>(new FileNode());
            for (FileNode f : files) {
                if(f.isDir()) {
                    root.getChildren().add(osDirToTree(f.getPath()));
                } else {
                    root.getChildren().add(new TreeItem<>(f));
                }
            }

            root.setExpanded(true);
            super.setRoot(root);

            expandAllRecursively(super.getRoot());
        }
    }

    private void expandAllRecursively(TreeItem<FileNode> item) {
        if(item != null && !item.isLeaf()) {
            item.setExpanded(true);
            for (TreeItem<FileNode> c : item.getChildren()) {
                expandAllRecursively(c);
            }
        }
    }

    private void fileTreeToFileDictTree(TreeItem<FileNode> treeItem, DictFile node) {
        for (int i = 0; i < treeItem.getChildren().size(); i++) {
            TreeItem<FileNode> c1 = treeItem.getChildren().get(i);
            var c2 = c1.getValue();
            fileTreeToFileDictTree(c1, c2);
            node.getChildren().add(c2);
        }
    }

    public void createArchive(File destFile, String password) {
        System.out.println("Create archive");

        var treeRoot = super.getRoot();
        var dictFileRoot = super.getRoot().getValue();
        fileTreeToFileDictTree(treeRoot, dictFileRoot);
        if(!files.isEmpty()) {
            SecureArchive folder = new SecureArchive(destFile.getName(), destFile.getPath(),
                    password, super.getRoot(), files.stream().map(f -> f.getFile()).collect(Collectors.toList()));
            folder.createArchiveWithRandomKeys();
        }
    }

    public void setRootAndDictRoot(TreeItem<FileNode> root) {
        super.setRoot(root);
        this.folder.setDictRoot(root);
    }

    public void saveArchive() {
        this.folder.commitArchive();
    }

    public void extractAll() {
        this.folder.extractAllRecursively(this.folder.getOriginalDict());
    }
}
