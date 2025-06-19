package com.magicfolder;

import com.magicfolder.components.FileTreeTableView;
import com.magicfolder.components.FileTreeTableViewToolbar;
import com.magicfolder.helpers.Toast;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.VBox;
import javafx.stage.*;
import javafx.stage.Popup;
import org.json.JSONObject;
import com.magicfolder.helpers.SecureArchive;

import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class OpenArchivePageController implements Initializable {
    @FXML
    private FileTreeTableView fileTree;
    @FXML
    private FileTreeTableViewToolbar fileTreeToolbar;

    private SecureArchive folder;

    private String passwordPlain;
    private byte[] salt;

    @FXML
    public void saveArchive() {
        try {
            this.fileTree.extractAll();
            this.fileTree.saveArchive();
            SecureArchive newFolder = new SecureArchive(folder.getName(), folder.getPath(), passwordPlain, salt);
            this.setFolder(newFolder, this.passwordPlain, this.salt);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setFolder(final SecureArchive folder, String passwordPlain, byte[] salt) {
        this.folder = folder;
        this.passwordPlain = passwordPlain;
        this.salt = salt;

        try {
            this.folder.getDecryptKeyIv2PairAndIvDict(passwordPlain);
            this.folder.getKeyIv1Pair();

            String filesDict = this.folder.readDict();
            var jsonRoot = new JSONObject(filesDict);
            var root = new TreeItem<>(new FileNode());
            this.folder.parseDict(root, jsonRoot);
            this.folder.setOriginalDict(root);
            List<DictFile> flattenedFilesList = new ArrayList<>();
            this.folder.flattenDict(jsonRoot, flattenedFilesList);

            this.fileTree.setFolder(this.folder);
            this.fileTree.setRootAndDictRoot(root);
            this.fileTree.setFiles(flattenedFilesList.stream().map(df -> new FileNode(df)).collect(Collectors.toList()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Popup createExtractPopup() {
        var popup = new Popup();
        var container = new VBox();
        var label = new Label("Extract to...");
        container.setStyle("-fx-background-color: white;");
        popup.getContent().add(container);
        container.setMinWidth(80);
        container.setMinHeight(50);
        return popup;
    }

    public void extractSelected() {
        ObservableList<TreeItem<FileNode>> items = fileTree.getSelectionModel().getSelectedItems();
        for (TreeItem<FileNode> item : items) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Extract to...");
            fileChooser.setInitialFileName(item.getValue().getName());
            File selectedFile = fileChooser.showSaveDialog(this.fileTree.getScene().getWindow());
            if (selectedFile == null) {
                continue;
            }
            if (item.getValue().isDir() && !item.getValue().isNew()) {
                folder.extractDir(selectedFile.getPath(), item);
            } else if (item.getValue().isNew()) {
                // if the item is new, it is copied from its original path, as it is not present in the encrypted archive
                // if the target folder already contains the item, the item is saved to a temp path, then it overrides the old one
                folder.moveFileOrDir(selectedFile.getPath(), item);
            } else {
                folder.extract(item.getValue().getId(), selectedFile.getPath());
            }
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.fileTree.setMode(FileTreeTableView.Mode.EDIT_ARCHIVE);
        this.fileTreeToolbar.setMode(FileTreeTableView.Mode.EDIT_ARCHIVE);

        fileTreeToolbar.setOnToolbarIconClicked(action -> {
            switch (action) {
                case BACK:
                    Stage stage = (Stage) this.fileTree.getScene().getWindow();
                    stage.close();
                    break;
                case DELETE:
                    this.fileTree.deleteDir();
                    break;
                case RENAME:
                    this.fileTree.renameSelected();
                    break;
                case ADD_FILE:
                    final DirectoryChooser fileChooser = new DirectoryChooser();
                    fileChooser.setTitle("Select a Folder");
                    Window window = this.fileTree.getScene().getWindow();
                    File selectedDirectory = fileChooser.showDialog(window);

                    if (selectedDirectory == null) {
                        break;
                    }

                    this.fileTree.addFile(selectedDirectory, this.fileTree.getSelectionModel().getSelectedItem());
                    break;
                case ADD_FOLDER:
                    this.fileTree.addDir();
                    break;
                case EXTRACT:
                    this.extractSelected();
                    break;
                case SAVE:
                    this.saveArchive();
                    Toast.showToast((Stage) this.fileTree.getScene().getWindow(), "Archive saved successfully",
                            "rgba(80, 200, 120,0.8)");
                    break;
            }
        });
    }
}
