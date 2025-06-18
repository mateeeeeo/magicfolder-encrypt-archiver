package com.magicfolder.components;

import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGException;
import com.kitfox.svg.SVGUniverse;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.Map;

import static java.awt.RenderingHints.*;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;

public class SVGIconRasterizer {
    private final static SVGUniverse renderer = new SVGUniverse(); // only one single renderer should be used for all icons
    public final static Map<Object, Object> RENDERING_HINTS = Map.of(
            KEY_ANTIALIASING,
            VALUE_ANTIALIAS_ON,
            KEY_ALPHA_INTERPOLATION,
            VALUE_ALPHA_INTERPOLATION_QUALITY,
            KEY_COLOR_RENDERING,
            VALUE_COLOR_RENDER_QUALITY,
            KEY_DITHERING,
            VALUE_DITHER_DISABLE,
            KEY_FRACTIONALMETRICS,
            VALUE_FRACTIONALMETRICS_ON,
            KEY_INTERPOLATION,
            VALUE_INTERPOLATION_BICUBIC,
            KEY_RENDERING,
            VALUE_RENDER_QUALITY,
            KEY_STROKE_CONTROL,
            VALUE_STROKE_PURE,
            KEY_TEXT_ANTIALIASING,
            VALUE_TEXT_ANTIALIAS_ON
    );

    public Image getFxImage(final String path, final Dimension dstDim) {
        Image fxImage;

        try {
            BufferedImage bufferedImage;
            bufferedImage = rasterize(path, dstDim);
            fxImage = SwingFXUtils.toFXImage(bufferedImage, null);
        } catch (SVGException e) {
            e.printStackTrace();
            // fallback to PNG
            String pngPath = path.replace(".svg", ".png");
            fxImage = new Image(getResourceUrl(pngPath).toExternalForm());
        }
        return fxImage;
    }
    private BufferedImage rasterize(final String path, final Dimension dstDim)
            throws SVGException {
        final var diagram = loadDiagram(path);
        final var wDiagram = diagram.getWidth();
        final var hDiagram = diagram.getHeight();
        final var srcDim = new Dimension((int) wDiagram, (int) hDiagram);

        final var scaled = fit(srcDim, dstDim);
        final var wScaled = (int) scaled.getWidth();
        final var hScaled = (int) scaled.getHeight();

        final var image = new BufferedImage(wScaled, hScaled, TYPE_INT_ARGB);

        final var g = image.createGraphics();
        g.setRenderingHints(RENDERING_HINTS);

        final var transform = g.getTransform();
        transform.setToScale(wScaled / wDiagram, hScaled / hDiagram);

        g.setTransform(transform);
        diagram.render(g);
        g.dispose();

        return image;
    }

    private Dimension fit(final Dimension src, final Dimension dst) {
        final var srcWidth = src.getWidth();
        final var srcHeight = src.getHeight();

        // Determine the ratio that will have the best fit.
        final var ratio = Math.min(
                dst.getWidth() / srcWidth, dst.getHeight() / srcHeight
        );

        // Scale both dimensions with respect to the best fit ratio.
        return new Dimension((int) (srcWidth * ratio), (int) (srcHeight * ratio));
    }

    private URL getResourceUrl(final String path) {
        return getClass().getResource(path);
    }

    /**
     * Loads the resource specified by the given path into an instance of
     * {@link SVGDiagram} that can be rasterized into a bitmap format. The
     * {@link SVGUniverse} class will
     *
     * @param path The full path (starting at the root), relative to the
     *             application or JAR file's resources directory.
     * @return An {@link SVGDiagram} that can be rasterized onto a
     * {@link BufferedImage}.
     */
    private SVGDiagram loadDiagram(final String path) {
        final var url = getResourceUrl(path);
        final var uri = renderer.loadSVG(url);
        final var diagram = renderer.getDiagram(uri);
        diagram.setIgnoringClipHeuristic(true);
        return diagram;

    }
}
