package org.omnetpp.figures.canvas;

import java.util.List;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PrecisionRectangle;
import org.eclipse.draw2d.geometry.Rectangle;

public class CanvasGroupFigure extends AbstractCanvasFigure {

    @Override
    protected boolean hitTest(Point point) {
        return false;
    }

    @Override
    public Rectangle getBounds() {
        recomputeBounds();
        return bounds;
    }

    @Override
    protected Rectangle getGraphicalBounds() {
        return getBounds(); // not needed, but not returning null, just in case...
    }

    @Override
    public void recomputeBounds() {
        Rectangle bounds = new PrecisionRectangle();

        @SuppressWarnings("unchecked")
        List<? extends IFigure> children = getChildren();

        if (!children.isEmpty()) {
            bounds.setBounds(children.get(0).getBounds());
        }

        for (IFigure child : children) {
            ((AbstractCanvasFigure)child).recomputeBounds();
            Rectangle childBounds = child.getBounds();
            bounds.union(childBounds);
        }

        this.bounds = bounds;
    }
}
