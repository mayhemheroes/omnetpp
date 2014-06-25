package org.omnetpp.figures.canvas;

import java.util.Arrays;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;

public class CanvasPixmapFigure extends AbstractCanvasImage {

    public void setPixmapData(int width, int height, Color fillColor) {
        PaletteData palette = new PaletteData(0xFF0000, 0x00FF00, 0x0000FF);
        ImageData data = new ImageData(width, height, 24, palette);
        int pixel = palette.getPixel(fillColor.getRGB());
        int[] scanLine = new int[width];
        Arrays.fill(scanLine, pixel);
        for (int y = 0; y < height; y++) {
            data.setPixels(0, y, width, scanLine, 0);
        }
        byte[] alphaLine = new byte[width];
        Arrays.fill(alphaLine, (byte) 255);
        for (int y = 0; y < height; y++) {
            data.setAlphas(0, y, width, alphaLine, 0);
        }
        this.image = data;
        needsUpdate = true;
        repaint();
    }

}
