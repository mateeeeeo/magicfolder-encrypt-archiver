package com.magicfolder.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.awt.*;


public class FileTreeTableViewToolbar extends HBox {

    public enum TOOLBAR_ACTION {
        BACK, DELETE, RENAME, ADD_FILE, ADD_FOLDER, EXTRACT, SAVE, LOCK
    }

    public static interface ToolbarIconClickedCallback {
        void run(TOOLBAR_ACTION action);
    }

    private ToolbarIconClickedCallback toolbarIconClickedHandler;
    private FileTreeTableView.Mode mode = FileTreeTableView.Mode.CREATE_ARCHIVE;

    public FileTreeTableView.Mode getMode() {
        return mode;
    }

    public void setMode(FileTreeTableView.Mode mode) {
        this.mode = mode;
        init();
    }

    public void setOnToolbarIconClicked(ToolbarIconClickedCallback callback) {
        this.toolbarIconClickedHandler = callback;
    }


    public FileTreeTableViewToolbar() {
        init();
    }

    private HBox createSeparator() {
        HBox separator = new HBox();
        separator.setPrefWidth(1);
        separator.setStyle("-fx-border-width:0 0 0 1px; -fx-border-style: solid; -fx-border-color: #364149;");
        return separator;
    }

    private HBox createIconWithAction(String svgResourcePath, TOOLBAR_ACTION action) {
        SVGIconRasterizer rasterizer = new SVGIconRasterizer();
        HBox container = new HBox();

        final Image fxImage = rasterizer.getFxImage(svgResourcePath,
                new Dimension(24, 24));
        ImageView icon = new ImageView();
        icon.setStyle("-fx-background-color: red;");
        icon.setImage(fxImage);
        container.getChildren().add(icon);

        container.setOnMouseClicked(event -> {
            this.toolbarIconClickedHandler.run(action);
        });

        return container;
    }

    private HBox createIconTextPairWithAction(String svgResourcePath, String text, TOOLBAR_ACTION action) {
        HBox icon = createIconWithAction(svgResourcePath, action);
        icon.setSpacing(4);
        icon.setAlignment(Pos.CENTER);
        Label iconLabel = new Label(text);
        iconLabel.setStyle("-fx-text-fill: white");
        icon.getChildren().add(iconLabel);
        return icon;
    }

    private HBox createSpacer() {
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        return spacer;
    }

    private void init() {
        getChildren().clear();

        setPadding(new Insets(12,  16, 12, 16));
        setSpacing(16);

        var backIcon = createIconWithAction("/icons/arrow_back.svg", TOOLBAR_ACTION.BACK);
        getChildren().add(backIcon);

        var separator = createSeparator();
        getChildren().add(separator);

        var addIcon = createIconWithAction("/icons/add_file.svg",
                TOOLBAR_ACTION.ADD_FILE);
        getChildren().add(addIcon);

        var folderAddIcon = createIconWithAction("/icons/new_folder.svg",
                TOOLBAR_ACTION.ADD_FOLDER);
        getChildren().add(folderAddIcon);

        separator = createSeparator();
        getChildren().add(separator);

        var renameIcon = createIconWithAction("/icons/edit.svg",
                TOOLBAR_ACTION.RENAME);
        getChildren().add(renameIcon);

        var trashIcon = createIconWithAction("/icons/trash.svg",
                TOOLBAR_ACTION.DELETE);
        getChildren().add(trashIcon);

        var separator1 = createSeparator();
        getChildren().add(separator1);

        if (mode == FileTreeTableView.Mode.EDIT_ARCHIVE) {
            var extractIcon = createIconWithAction("/icons/unarchive.svg",
                    TOOLBAR_ACTION.EXTRACT);
            getChildren().add(extractIcon);

            var separator2 = createSeparator();
            getChildren().add(separator2);

            var saveIcon = createIconTextPairWithAction("/icons/save.svg", "Save",
                    TOOLBAR_ACTION.SAVE);
            getChildren().add(saveIcon);
        } else if(mode == FileTreeTableView.Mode.CREATE_ARCHIVE) {
            var lockIcon = createIconTextPairWithAction("/icons/lock.svg", "Encrypt",
                    TOOLBAR_ACTION.LOCK);
            getChildren().add(lockIcon);
        }

        requestLayout();
    }
}
