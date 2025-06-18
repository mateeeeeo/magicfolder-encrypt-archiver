package com.magicfolder.components;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

import java.awt.*;


public class FileTreeTableViewToolbar extends HBox {

    public enum TOOLBAR_ACTION {
        DELETE, RENAME, ADD_FILE, ADD_FOLDER, EXTRACT, SAVE, LOCK
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

    private ImageView createIconWithAction(String svgResourcePath, TOOLBAR_ACTION action) {
        SVGIconRasterizer rasterizer = new SVGIconRasterizer();
        final Image fxImage = rasterizer.getFxImage(svgResourcePath,
                new Dimension(24, 24));
        ImageView icon = new ImageView();
        icon.setImage(fxImage);
        icon.setOnMouseClicked(event -> {
            this.toolbarIconClickedHandler.run(action);
        });

        return icon;
    }

    private void init() {
        getChildren().clear();

        setSpacing(16);

        ImageView addIcon = createIconWithAction("/icons/add_file.svg",
                TOOLBAR_ACTION.ADD_FILE);
        getChildren().add(addIcon);

        ImageView folderAddIcon = createIconWithAction("/icons/new_folder.svg",
                TOOLBAR_ACTION.ADD_FOLDER);
        getChildren().add(folderAddIcon);

        ImageView renameIcon = createIconWithAction("/icons/edit.svg",
                TOOLBAR_ACTION.RENAME);
        getChildren().add(renameIcon);

        ImageView trashIcon = createIconWithAction("/icons/trash.svg",
                TOOLBAR_ACTION.DELETE);
        getChildren().add(trashIcon);

        var separator1 = createSeparator();
        getChildren().add(separator1);

        if (mode == FileTreeTableView.Mode.EDIT_ARCHIVE) {
            ImageView extractIcon = createIconWithAction("/icons/unarchive.svg",
                    TOOLBAR_ACTION.EXTRACT);
            getChildren().add(extractIcon);

            var separator2 = createSeparator();
            getChildren().add(separator2);

            ImageView saveIcon = createIconWithAction("/icons/save.svg",
                    TOOLBAR_ACTION.SAVE);
            getChildren().add(saveIcon);
        } else if(mode == FileTreeTableView.Mode.CREATE_ARCHIVE) {
            ImageView lockIcon = createIconWithAction("/icons/lock.svg",
                    TOOLBAR_ACTION.LOCK);
            getChildren().add(lockIcon);
        }

        requestLayout();
    }
}
