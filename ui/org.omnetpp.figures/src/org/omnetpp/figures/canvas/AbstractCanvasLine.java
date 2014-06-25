package org.omnetpp.figures.canvas;

import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;

/**
 * Extends {@link AbstractCanvasShape} with arrowhead handling and cap styles.
 *
 * @author attila
 */
public abstract class AbstractCanvasLine extends AbstractCanvasShape {
    public enum CapStyle { CAP_BUTT, CAP_SQUARE, CAP_ROUND };
    public enum ArrowHead { ARROW_NONE, ARROW_SIMPLE, ARROW_TRIANGLE, ARROW_BARBED };

    protected CapStyle cap = CapStyle.CAP_BUTT;

    protected ArrowHead startArrowHead = ArrowHead.ARROW_NONE;
    protected ArrowHead endArrowHead = ArrowHead.ARROW_NONE;

    protected LineDecoration startDecor;
    protected LineDecoration endDecor;

    @Override
    protected boolean hitTest(Point point) {
        boolean result = super.hitTest(point);

        boolean startDecorHit = (startDecor == null) ? false : startDecor.hitTest(point);
        boolean endDecorHit = (endDecor == null) ? false : endDecor.hitTest(point);

        return result || startDecorHit || endDecorHit;
    }

    @Override
    public Rectangle getGraphicalBounds() {
        Rectangle bounds = super.getGraphicalBounds();

        if (startDecor != null) bounds.union(startDecor.getBounds());
        if (endDecor != null) bounds.union(endDecor.getBounds());

        return bounds;
    }

    /**
     * Should return the position to which the startArrowHead decoration should
     * be placed, i.e. where the line begins.
     */
    protected abstract Point getStartArrowheadPosition();

    /**
     * Should return the angle to which the startArrowHead decoration should
     * be rotated, i.e. at which angle the line's beginning points.
     *
     * In degrees, with 0° pointing right, and the positive direction is CCW.
     */
    protected abstract double getStartArrowheadAngle();

    /**
     * Should return the position to which the endArrowHead decoration should
     * be placed, i.e. where the line ends.
     */
    protected abstract Point getEndArrowheadPosition();

    /**
     * Should return the angle to which the endArrowHead decoration should
     * be rotated, i.e. at which angle the line's end points.
     *
     * In degrees, with 0° pointing right, and the positive direction is CCW.
     */
    protected abstract double getEndArrowheadAngle();


    private LineDecoration setupArrowHead(ArrowHead style, Point position, double angle) {
        if (position == null) {
            return null;
        }

        LineDecoration decor = new LineDecoration(style);

        decor.setScale(getZoomedLineWidth());
        decor.setLineWidth(getZoomedLineWidth());
        decor.setLocation(position);
        decor.setRotation(angle);
        decor.setColor(getLocalForegroundColor());
        decor.setOpacity(getLineOpacity());

        return decor;
    }

    /**
     * Places the end decorations in the correct position and orientation.
     * Should be called every time any of their parameters changed.
     */
    protected void updateArrowHeads() {
        erase();

        startDecor = setupArrowHead(startArrowHead, getStartArrowheadPosition(), getStartArrowheadAngle());
        endDecor = setupArrowHead(endArrowHead, getEndArrowheadPosition(), getEndArrowheadAngle());

        recomputeBounds();
        repaint();
        revalidate();
    }

    protected void paintArrowHeads(Graphics graphics) {
        if (startDecor != null) {
            startDecor.paint(graphics);
        }

        if (endDecor != null) {
            endDecor.paint(graphics);
        }
    }

    public ArrowHead getStartArrowHead() {
        return startArrowHead;
    }

    public void setStartArrowHead(ArrowHead arrowHead) {
        if (startArrowHead != arrowHead) {
            startArrowHead = arrowHead;

            updateArrowHeads();
        }
    }

    public ArrowHead getEndArrowHead() {
        return endArrowHead;
    }

    public void setEndArrowHead(ArrowHead arrowHead) {
        if (endArrowHead != arrowHead) {
            endArrowHead = arrowHead;
            updateArrowHeads();
        }
    }

    public CapStyle getCapStyle() {
        return cap;
    }

    public void setCapStyle(CapStyle c) {
        if (cap != c) {
            cap = c;
            repaint();
        }
    }

    @Override
    public void setLineWidth(float w) {
        super.setLineWidth(w);
        updateArrowHeads();
    }

    /**
     * Adds the cap style setup and arrowhead painting to the super implementation.
     */
    @Override
    public void paintFigure(Graphics graphics) {
        switch (cap) {
        case CAP_BUTT:
            graphics.setLineCap(SWT.CAP_FLAT);
            break;
        case CAP_SQUARE:
            graphics.setLineCap(SWT.CAP_SQUARE);
            break;
        case CAP_ROUND:
            graphics.setLineCap(SWT.CAP_ROUND);
            break;
        }

        super.paintFigure(graphics);

        paintArrowHeads(graphics);
        graphics.restoreState();
    }

    // Dummy implementations, as filling lines generally doesn't make sense.

    @Override
    public void setFill(boolean fill) {
        // Nothing
    }

    @Override
    public boolean getFill() {
        return false;
    }

    @Override
    protected void fillShape(Graphics graphics) {
    }
}
