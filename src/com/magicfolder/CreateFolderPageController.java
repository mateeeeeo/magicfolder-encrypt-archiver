package com.magicfolder;

import com.magicfolder.components.FileTreeTableView;
import com.magicfolder.components.FileTreeTableViewToolbar;
import com.magicfolder.helpers.Toast;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Dialog;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class CreateFolderPageController implements Initializable {

    @FXML
    private FileTreeTableView fileTree;

    private List<File> files = new ArrayList<>();

    @FXML
    private FileTreeTableViewToolbar fileTreeToolbar;


    public void setFiles(List<File> files) {
        this.files = files;
        this.fileTree.setFiles(this.files.stream().map(f -> new FileNode(f)).collect(Collectors.toList()));
    }

    public void displayFiles() {
        this.fileTree.setMode(FileTreeTableView.Mode.CREATE_ARCHIVE);
        this.fileTree.displayFiles();

        this.fileTreeToolbar.setMode(FileTreeTableView.Mode.CREATE_ARCHIVE);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        fileTreeToolbar.setOnToolbarIconClicked(action -> {
            switch (action) {
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
                case LOCK:
                    FileChooser saveArchiveChooser = new FileChooser();
                    saveArchiveChooser.setInitialFileName(FilenameUtils.removeExtension(this.files.get(0).getName()) + ".mgf");
                    File destFile = saveArchiveChooser.showSaveDialog(this.fileTree.getScene().getWindow());
                    String password = askForPassword((Stage) this.fileTree.getScene().getWindow());

                    if(destFile == null || password == null) {
                        break;
                    }

                    this.fileTree.createArchive(destFile, password);
                    Toast.showToast((Stage) this.fileTree.getScene().getWindow(), "Locked archive!",
                            "rgba(80, 200, 120,0.8)");
            }
        });
    }

    public String askForPassword(Stage ownerStage) {
        Dialog<List<String>> dialog = new Dialog<>();
        dialog.initOwner(ownerStage);
        dialog.setTitle("Enter Password");

        ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        PasswordField passwordConfirmField = new PasswordField();
        passwordConfirmField.setPromptText("Confirm Password");

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.getChildren().addAll(passwordField, passwordConfirmField);
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(dialogButton -> {
            List<String> results = new ArrayList<>();
            if (dialogButton == okButtonType) {
                results.add(passwordField.getText());
                results.add(passwordConfirmField.getText());
                return results;
            }
            return null;
        });

        Optional<List<String>> result = dialog.showAndWait();
        List<String> passwords = result.orElse(null);
        if (passwords == null) {
            return null;
        }
        if (Objects.equals(passwords.get(0), passwords.get(1))) {
            return passwords.get(0);
        } else {
            Toast.showToast(ownerStage, "Password didn't match.", "rgba(237, 26, 26, 0.8)");
            return askForPassword(ownerStage);
        }
    }

    @FXML
    public void navigateBack(Event event) {
        System.out.println(event);

        try {
            Parent newRoot = FXMLLoader.load(getClass().getResource("fxml/FoldersPage.fxml"));
            Stage currentStage = (Stage) ((Control) event.getSource()).getScene().getWindow();

            currentStage.setTitle("MagicFolder - Terminal");
            currentStage.setScene(new Scene(newRoot, 800, 600));
            currentStage.show();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
