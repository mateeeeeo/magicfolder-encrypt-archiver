package com.magicfolder.components;

import javafx.application.Platform;
import javafx.embed.swing.SwingNode;
import javafx.scene.Node;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class FXSVGIcon extends SwingNode {
    public FXSVGIcon(String svgPath, int width, int height) {
        File svgFile = new File(svgPath);

        SwingUtilities.invokeLater(() -> {
            try {
                var svgPanel = new SvgPanel(svgFile, new Dimension(width, height));
                setContent(svgPanel);

                // swing node black background bug workaround
                Platform.runLater(() -> {
                    autosize();
                    setVisible(false);
                    requestFocus();
                    setVisible(true);
                });


            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public Node getStyleableNode() {
        return super.getStyleableNode();
    }
}
