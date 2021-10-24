package com.magicfolder;
import com.magicfolder.helpers.Folder;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;

public class FoldersPageController {
    private List<File> files = new ArrayList<>();
    private List<File> folders = new ArrayList<>();

    @FXML
    public void onDragOver(DragEvent event) {
        if (event.getDragboard().hasFiles()) {
            event.acceptTransferModes(TransferMode.ANY);
        }
        event.consume();
    }

    @FXML
    public void onDragEntered() {
//        JFXFillTransition fillTransition = new JFXFillTransition();
//        dragMenu.getChildren().add(0, plusIcon);
//        fillTransition.setDuration(Duration.millis(500));
//        fillTransition.setRegion(dragMenu);
//        fillTransition.setFromValue(Color.web("#1d57a3"));
//        fillTransition.setToValue(Color.web("#5b9aff"));
//        fillTransition.play();
    }

    @FXML
    public void onDragExited() {
//        JFXFillTransition fillTransition = new JFXFillTransition();
//        dragMenu.getChildren().remove(plusIcon);
//        fillTransition.setDuration(Duration.millis(500));
//        fillTransition.setRegion(dragMenu);
//        fillTransition.setFromValue(Color.web("#5b9aff"));
//        fillTransition.setToValue(Color.web("#1d57a3"));
//        fillTransition.play();
    }

    @FXML
    public void onDragDropped(DragEvent event) {

        files.clear();
        folders.clear();

        try {
            Font.loadFont("../fonts/Berlin.ttf", 24);

            for (File file : event.getDragboard().getFiles()) {
                if (file.getName().endsWith(".mgf")) {
                    folders.add(file);
                } else {
                    files.add(file);
                }
            }

            if(!files.isEmpty()) {
                FXMLLoader createFolderPageLoader = new FXMLLoader(getClass().getResource("fxml/CreateFolderPage.fxml"));
                VBox createFolderRoot = createFolderPageLoader.load();
                CreateFolderPageController createFolderPageController = createFolderPageLoader.getController();
                createFolderPageController.setFiles(files);
                createFolderPageController.displayFiles();

                Stage createFolderWindow = new Stage();
                createFolderWindow.setTitle("MagicFolder - Create");
                createFolderWindow.setScene(new Scene(createFolderRoot, 840, 600));
                createFolderWindow.show();
            }

            if (!folders.isEmpty()) {
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


//    @FXML
//    public void onDragDropped(DragEvent event) {
//
//        files.clear();
//        folders.clear();
//
//        VBox createFilesContainer = new VBox();
//
//        try {
//            Font.loadFont("../fonts/Berlin.ttf", 24);
//
//            for (File file : event.getDragboard().getFiles()) {
//                if (file.getName().endsWith(".mgf")) {
//                    folders.add(file);
//                } else {
//                    files.add(file);
//                }
//            }
//
//            Stage createFolderWindow = new Stage();
//            createFolderWindow.setTitle("MagicFolder - Create");
//            FXMLLoader createFolderPageLoader = new FXMLLoader(getClass().getResource("fxml/CreateFolderPage.fxml"));
//            VBox createFolderRoot = createFolderPageLoader.load();
//            CreateFolderPageController createFolderPageController = createFolderPageLoader.getController();
//            createFolderPageController.setFiles(files);
//
//            Stage openFolderWindow = new Stage();
//            openFolderWindow.setTitle("MagicFolder - Open");
//            FXMLLoader passwordPageLoader = new FXMLLoader(getClass().getResource("fxml/PasswordPage.fxml"));
//            VBox openFolderRoot = passwordPageLoader.load();
//            PasswordDialogController passwordDialogController = passwordPageLoader.getController();
//
//            HBox fileContainer;
//
//            HBox space;
//
//            DecimalFormat decimalFormat = new DecimalFormat("#,###");
//
//            if (!files.isEmpty()) {
//                Label fileSize;
//                Label fileName;
//                int i = 1;
//                for (File file : files) {
//                    fileSize = new Label(decimalFormat.format(file.length()));
//                    fileSize.setTextFill(Color.WHITE);
//                    fileName = new Label(i + ". " + file.getName());
//                    fileName.setTextFill(Color.WHITE);
//                    space = new HBox();
//                    HBox.setHgrow(space, Priority.ALWAYS);
//                    fileContainer = new HBox();
//                    fileContainer.getChildren().addAll(fileName, space, fileSize);
//                    HBox.setHgrow(fileContainer, Priority.ALWAYS);
//                    createFilesContainer.getChildren().add(fileContainer);
//                    i++;
//                }
//
//                createFilesContainer.setPadding(new Insets(20));
//                VBox createFolderContentContainer = (VBox) ((HBox) createFolderRoot.getChildren().get(1)).getChildren().get(1);
//                HBox.setHgrow(createFolderContentContainer, Priority.ALWAYS);
//                createFolderContentContainer.getChildren().add(createFilesContainer);
//                createFolderWindow.setScene(new Scene(createFolderRoot, 840, 600));
//                createFolderWindow.show();
//            }
//
//            if (!folders.isEmpty()) {
//
//                int i = 1;
//
//                for (File folder : folders) {
//                    byte[] buffer = new byte["!".length() + 60 + 16];
//                    FileInputStream fis = new FileInputStream(folder.getPath());
//                    fis.read(buffer);
//
//                    if (new String(buffer).contains("!")) {
//                        openFolderWindow.setScene(new Scene(openFolderRoot, 840, 600));
//                        passwordDialogController.setBcryptHash(buffer);
//                        passwordDialogController.setFolder(folder);
//                    }
//                    openFolderWindow.show();
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
}
