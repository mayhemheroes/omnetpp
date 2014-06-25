package org.omnetpp.figures.canvas;

import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PrecisionRectangle;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;
import org.omnetpp.figures.misc.Transform;

public class CanvasIconFigure extends CanvasImageFigure {

    @Override
    public Rectangle getGraphicalBounds() {
        Rectangle bounds = definingRectangle.getRectangle();
        bounds.translate(-definingRectangle.getLocation().preciseX(), -definingRectangle.getLocation().preciseY());
        return bounds;
    }

    @Override
    public Transform getCascadedTransform() {
        Point offset = super.getCascadedTransform().applyTo(definingRectangle.getLocation());
        Transform t = new Transform();
        t.translate((float)offset.preciseX(), (float)offset.preciseY());
        return t;
    }

    @Override
    protected boolean hitTest(Point point) {
        Rectangle rect = getGraphicalBounds();

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
    protected void paintFigure(Graphics graphics) {
        if (image != null) {
            if (needsUpdate) {
                updateImage();
                needsUpdate = false;
            }
            graphics.setAntialias(SWT.ON);
            switch (interpolation) {
                case INTERPOLATION_NONE: graphics.setInterpolation(SWT.NONE); break;
                case INTERPOLATION_FAST:
                case INTERPOLATION_BEST: graphics.setInterpolation(SWT.HIGH); break;
            }
            graphics.setAlpha((int) Math.round(opacity * 255.0));
            Point offset = definingRectangle.getAnchoringOffset();
            Rectangle dest = new PrecisionRectangle(offset.preciseX(), offset.preciseY(),
                    definingRectangle.getSize().preciseWidth(), definingRectangle.getSize().preciseHeight());
            graphics.drawImage(tintedImage, new Rectangle(tintedImage.getBounds()), dest);
        }
    }
}
