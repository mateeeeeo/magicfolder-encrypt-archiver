package com.magicfolder;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.*;
import java.net.URL;
import java.util.*;

public class MainPageController implements Initializable {
    @FXML
    private ChoiceBox<String> chooseFileBtn;

    @FXML
    public void onDragOver(DragEvent event) {
        if (event.getDragboard().hasFiles()) {
            event.acceptTransferModes(TransferMode.ANY);
        }
        event.consume();
    }

    @FXML
    public void onDragDropped(DragEvent event) {
        List<File> files = new ArrayList<>();
        List<File> folders = new ArrayList<>();
        for (File file : event.getDragboard().getFiles()) {
            if (file.getName().endsWith(".mgf")) {
                folders.add(file);
            } else {
                files.add(file);
            }
        }

        if (!files.isEmpty() && !folders.isEmpty()) {
            System.out.println("Please provide either only unencrypted files/folders or encrypted archives");
        } else if (!files.isEmpty()){
            navigateToCreateArchive(files, null);
        } else if (!folders.isEmpty()){
            navigateToCreateArchive(null, folders);
        } else {
            System.out.println("Please select some files!");
        }
    }

    @FXML
    public void chooseFile() {
        List<File> files = new ArrayList<>();
        List<File> folders = new ArrayList<>();
        Window window = chooseFileBtn.getScene().getWindow();

        final FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Any File(s)", "*.*"));
        List<File> chosenFiles = fileChooser.showOpenMultipleDialog(window);

        if (chosenFiles == null) {
            return;
        }

        for (File f : chosenFiles) {
            if (f.getName().endsWith(".mgf")) {
                folders.add(f);
            } else {
                files.add(f);
            }
        }

        if (!files.isEmpty() && !folders.isEmpty()) {
            System.out.println("Please provide either only unencrypted files/folders or encrypted archives");
        } else if (!files.isEmpty()){
            navigateToCreateArchive(files, null);
        } else if (!folders.isEmpty()){
            navigateToCreateArchive(null, folders);
        } else {
            System.out.println("Please select some files!");
        }
    }

    private void chooseArchive() {
        List<File> archives = new ArrayList<>();

        Window window = chooseFileBtn.getScene().getWindow();

        final FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(".mgf archives", "*.mgf"));
        File chosenFile = fileChooser.showOpenDialog(window);

        if (chosenFile == null) {
            return;
        }
        archives.add(chosenFile);

        navigateToCreateArchive(null, archives);
    }

    private void navigateToCreateArchive(List<File> files, List<File> folders) {
        try {
            if(files != null) {
                FXMLLoader createFolderPageLoader = new FXMLLoader(getClass().getResource("fxml/CreateFolderPage.fxml"));
                StackPane createFolderRoot = createFolderPageLoader.load();
                CreateFolderPageController createFolderPageController = createFolderPageLoader.getController();
                createFolderPageController.setFiles(files);
                createFolderPageController.displayFiles();

                Stage currentStage = (Stage) chooseFileBtn.getScene().getWindow();
                currentStage.setTitle("MagicFolder - Create");
                currentStage.setScene(new Scene(createFolderRoot, 840, 600));
            }

            if (folders != null) {
                Stage openFolderWindow = new Stage();
                openFolderWindow.setTitle("MagicFolder - Open");
                FXMLLoader passwordPageLoader = new FXMLLoader(getClass().getResource("fxml/PasswordPage.fxml"));
                VBox openFolderRoot = passwordPageLoader.load();
                PasswordDialogController passwordDialogController = passwordPageLoader.getController();

                for (File folder : folders) {
                    byte[] buffer = new byte["!".length() + 60 + 16];
                    FileInputStream fis = new FileInputStream(folder.getPath());
                    fis.read(buffer);

                    if (new String(buffer).contains("!")) {
                        openFolderWindow.setScene(new Scene(openFolderRoot, 840, 600));
                        passwordDialogController.setBcryptHash(buffer);
                        passwordDialogController.setFolder(folder);
                    }
                    openFolderWindow.show();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        chooseFileBtn.getItems().addAll("Choose File", "Choose Directory", "Choose Archive");
        chooseFileBtn.setValue("Choose...");
        chooseFileBtn.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (Objects.equals(newVal, "Choose File")) {
                chooseFile();
            } else if (Objects.equals(newVal, "Choose Directory")) {
                chooseDirectory();
            } else if(Objects.equals(newVal, "Choose Archive")) {
                chooseArchive();
            }
        });
    }

    private void chooseDirectory() {
        List<File> files = new ArrayList<>();
        Window window = chooseFileBtn.getScene().getWindow();

        final DirectoryChooser directoryChooser = new DirectoryChooser();
        File chosenDir = directoryChooser.showDialog(window);

        if (chosenDir == null) {
            return;
        }

        files.add(chosenDir);
        navigateToCreateArchive(files, null);
    }
}
