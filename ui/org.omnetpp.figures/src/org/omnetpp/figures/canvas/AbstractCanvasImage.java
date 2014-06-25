package org.omnetpp.figures.canvas;

import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PrecisionRectangle;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;
import org.omnetpp.figures.misc.AnchoredRectangle;
import org.omnetpp.figures.misc.AnchoredRectangle.Anchor;

public abstract class AbstractCanvasImage extends AbstractCanvasFigure {

    // NONE is nearest neighbor, FAST is bilinear, BEST can be something fancier (like Lanczos) if available.
    public enum Interpolation { INTERPOLATION_NONE, INTERPOLATION_FAST, INTERPOLATION_BEST };

    protected ImageData image = null;
    protected Image tintedImage = null; // always drawing this
    protected AnchoredRectangle definingRectangle = new AnchoredRectangle();
    protected Interpolation interpolation = Interpolation.INTERPOLATION_FAST;
    protected double opacity = 1; // 0 - invisible, 1 - fully opaque
    protected Color tintColor = new Color(Display.getDefault(), 0, 0, 1);
    protected double tintAmount = 0; // 0 -not at all, 1 - fully

    protected boolean needsUpdate = true;

    protected void updateImage() {
        tintedImage = new Image(Display.getDefault(), image);

        ImageData tintedData = (ImageData)image.clone();

        double rdest = tintColor.getRed() / 255.0;
        double gdest = tintColor.getGreen() / 255.0;
        double bdest = tintColor.getBlue() / 255.0;

        int[] scanLine = new int[tintedData.width];
        for (int y = 0; y < tintedData.height; y++) {
            tintedData.getPixels(0, y, tintedData.width, scanLine, 0);
            for (int x = 0; x < tintedData.width; x++) {
                int r = (scanLine[x] / 65536) % 256;
                int g = (scanLine[x] / 256) % 256;
                int b = scanLine[x] % 256;


                // transform - code taken from qtenv/qtutil.cc (ColorizeEffect)
                int lum = (int)(0.2126*r + 0.7152*g + 0.0722*b);
                r = (int)((1-tintAmount)*r + tintAmount*lum*rdest);
                g = (int)((1-tintAmount)*g + tintAmount*lum*gdest);
                b = (int)((1-tintAmount)*b + tintAmount*lum*bdest);

                scanLine[x] = 65536*r + 256*g + b;
            }
            tintedData.setPixels(0, y, tintedData.width, scanLine, 0);
        }

        tintedImage = new Image(Display.getDefault(), tintedData);
    }

    @Override
    protected boolean hitTest(Point point) {
        PrecisionRectangle rect = definingRectangle.getRectangle();

        if (!rect.contains(point) || (image == null)) {
            return false;
        }

        int x = (int) ((point.preciseX() - rect.preciseX())
                / rect.preciseWidth() * image.width);
        int y = (int) ((point.preciseY() - rect.preciseY())
                / rect.preciseHeight() * image.height);

        return (image.getAlpha(x, y) != 0);
    }

    @Override
    protected Rectangle getGraphicalBounds() {
        return definingRectangle.getRectangle();
    }

    public void setImage(Image image) {
        if ((image != null) && !image.getImageData().equals(this.image)) {
            this.image = image.getImageData();
            repaint();
        }
    }

    public void setLocation(Point loc) {
        definingRectangle.setLocation(loc);
        repaint();
    }

    public void setDimension(Dimension size) {
        definingRectangle.setSize(size);
        repaint();
    }

    public void setAnchor(Anchor anchor) {
        definingRectangle.setAnchor(anchor);
        repaint();
    }

    public Interpolation getInterpolation() {
        return interpolation;
    }

    public void setInterpolation(Interpolation interp) {
        if (interp != interpolation) {
            interpolation = interp;
            repaint();
        }
    }

    public double getOpacity() {
        return opacity;
    }

    public void setOpacity(double opacity) {
        opacity = Math.min(Math.max(opacity, 0), 1); // clamping
        if (this.opacity != opacity) {
            this.opacity = opacity;
            repaint();
        }
    }

    public Color getTintColor() {
        return tintColor;
    }

    public void setTintColor(Color tintColor) {
        if (this.tintColor != tintColor) {
            this.tintColor = new Color(Display.getDefault(), tintColor.getRGB());
            needsUpdate = true;
            repaint();
        }
    }

    public double getTintAmount() {
        return tintAmount;
    }

    public void setTintAmount(double tintAmount) {
        tintAmount = Math.min(Math.max(tintAmount, 0), 1); // clamping
        if (this.tintAmount != tintAmount) {
            this.tintAmount = tintAmount;
            needsUpdate = true;
            repaint();
        }
    }

    @Override
    protected void paintFigure(Graphics graphics) {
        if (image != null) {
            if (needsUpdate) {
                updateImage();
                needsUpdate = false;
            }
            graphics.setAntialias(SWT.ON);
            switch (interpolation) {
                case INTERPOLATION_NONE: graphics.setInterpolation(SWT.NONE); break;
                case INTERPOLATION_FAST: // graphics.setInterpolation(SWT.LOW);  break; // in SWT this is still ugly
                case INTERPOLATION_BEST: graphics.setInterpolation(SWT.HIGH); break;
            }
            graphics.setAlpha((int) Math.round(opacity * 255.0));
            graphics.drawImage(tintedImage, new Rectangle(tintedImage.getBounds()),
                    definingRectangle.getRectangle());
        }
    }
}
