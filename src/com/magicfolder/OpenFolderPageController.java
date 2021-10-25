package com.magicfolder;

import com.magicfolder.helpers.Folder;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

public class OpenFolderPageController implements Initializable {
    @FXML
    private ListView<FileListItem> fileList;
    @FXML
    private VBox extractBtn;

    private Folder folder;
    private byte[] extractKey;
    private byte[] extractIv;

    private List<String> selectedFiles;

    public void setFolder(final Folder folder, final byte[] extractKey, final byte[] extractIv) {
        this.folder = folder;
        this.extractKey = extractKey;
        this.extractIv = extractIv;

        List<String> fileNames = folder.getFileNames(extractKey, extractIv);
        List<Long> fileSizes = folder.getFileSizes(extractKey, extractIv);

        List<FileListItem> fileListItems = new ArrayList<>();

        for(int i = 0; i < fileNames.size(); i++) {
            fileListItems.add(new FileListItem(i, fileNames.get(i), fileSizes.get(i), folder, extractKey, extractIv));
        }

        fileList.getItems().addAll(fileListItems);
    }

    public void extractSelected() {
        for (String selectedFileName : selectedFiles) {
            folder.extract(selectedFileName, extractKey, extractIv);
        }
    }

    public void deselectAll() {
        fileList.getSelectionModel().clearSelection();
        selectedFiles.clear();
    }

    public void deleteTempFiles() {
        for (FileListItem item : fileList.getItems()) {
            item.deleteTempFile();
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        selectedFiles = new ArrayList<>();
        fileList.setCellFactory(stringListView -> new FileListCell());
        fileList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        fileList.getSelectionModel().selectedItemProperty().addListener((observableValue, s, t1) -> {
            FileListItem[] fileListItems = fileList.getSelectionModel().getSelectedItems().toArray(new FileListItem[0]);

            for (FileListItem item : fileListItems) {
                selectedFiles.add(item.getFileName());
            }
        });
    }
}
