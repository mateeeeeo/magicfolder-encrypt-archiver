package com.magicfolder.components;

import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGUniverse;

import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.net.URL;

public class SvgPanel extends JPanel {
    private final SVGDiagram diagram;

    public SvgPanel(URL svgPath, Dimension dimensions) throws Exception {
        setBackground(new Color(0,0,0,0));
        SVGUniverse universe = new SVGUniverse();
        URI svgUri = universe.loadSVG(svgPath);
        diagram = universe.getDiagram(svgUri);
        setPreferredSize(dimensions); // optional
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        try {
            Graphics2D g2 = (Graphics2D) g;
            int panelWidth = getWidth();
            int panelHeight = getHeight();

            float svgWidth = diagram.getWidth();
            float svgHeight = diagram.getHeight();

            g2.scale(panelWidth / svgWidth, panelHeight / svgHeight);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            diagram.render(g2); // renders vector SVG
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}