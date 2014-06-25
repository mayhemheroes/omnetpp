package org.omnetpp.figures.canvas;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PrecisionPoint;
import org.eclipse.draw2d.geometry.PrecisionRectangle;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.widgets.Display;
import org.omnetpp.figures.misc.AnchoredRectangle;

public class CanvasArcFigure extends AbstractCanvasLine {

    AnchoredRectangle definingRectangle;

    private double startAngle;
    private double endAngle;

    public CanvasArcFigure() {
        definingRectangle = new AnchoredRectangle();

        startAngle = 0;
        endAngle = 360;

        updatePath();
    }

    protected void updatePath() {
        path = new Path(Display.getDefault());

        PrecisionRectangle rectangle = definingRectangle.getRectangle();

        path.addArc((float)rectangle.preciseX(), (float)rectangle.preciseY(),
                (float)rectangle.preciseWidth(), (float)rectangle.preciseHeight(),
                (float)startAngle, (float)(endAngle - startAngle));

        repaint();
    }

    public AnchoredRectangle getDefiningRectangle() {
        return definingRectangle;
    }

    public void setDefiningRectangle(AnchoredRectangle rectangle) {
        if ((rectangle != null) && (!definingRectangle.equals(rectangle))) {
            erase();
            definingRectangle = rectangle;
            updateArrowHeads();
            updatePath();
            repaint();
        }
    }

    /**
     * Gets the angle at which the arc is beginning.
     *
     * In degrees, with 0° pointing right, and the positive direction is CCW.
     */
    public double getStartAngle() {
        return startAngle;
    }

    /**
     * Sets the angle at which the arc is beginning.
     *
     * In degrees, with 0° pointing right, and the positive direction is CCW.
     */
    public void setStartAngle(double startAngle) {
        if (startAngle != this.startAngle) {
            this.startAngle = startAngle;

            updateArrowHeads();
            updatePath();
        }
    }

    /**
     * Gets the angle at which the arc is ending.
     *
     * In degrees, with 0° pointing right, and the positive direction is CCW.
     */
    public double getEndAngle() {
        return endAngle;
    }

    /**
     * Sets the angle at which the arc is ending.
     *
     * In degrees, with 0° pointing right, and the positive direction is CCW.
     */
    public void setEndAngle(double endAngle) {
        if (endAngle != this.endAngle) {
            this.endAngle = endAngle;
            updateArrowHeads();
            updatePath();
        }
    }

    private PrecisionPoint getPositionFromAngle(double angle) {
        PrecisionRectangle boundingRectangle = definingRectangle.getRectangle();

        Point center = boundingRectangle.getCenter();

        // The sine part is negative because the y axis is pointing downwards in draw2d
        return new PrecisionPoint(center.preciseX() + Math.cos(Math.toRadians(angle)) * boundingRectangle.preciseWidth() / 2.0,
                center.preciseY() - Math.sin(Math.toRadians(angle)) * boundingRectangle.preciseHeight() / 2.0);
    }

    private double getTangentAngleFromAngle(double angle) {
        PrecisionRectangle boundingRectangle = definingRectangle.getRectangle();

        double x = Math.cos(Math.toRadians(angle));
        double y = Math.sin(Math.toRadians(angle));

        // yes, multiply x with height and y with width
        // (should divide x with width and y with height, but this way the numbers
        // won't be so small to lose accuracy, and the result is the same)
        x *= boundingRectangle.preciseHeight();
        y *= boundingRectangle.preciseWidth();

        // 90 to be tangent, and negative because positive y is downwards in draw2d
        return 90 - Math.toDegrees(Math.atan2(y, x));
    }

    @Override
    protected PrecisionPoint getStartArrowheadPosition() {
        return getPositionFromAngle(startAngle);
    }

    @Override
    protected double getStartArrowheadAngle() {
        return getTangentAngleFromAngle(startAngle);
    }

    @Override
    protected PrecisionPoint getEndArrowheadPosition() {
        return getPositionFromAngle(endAngle);
    }

    @Override
    protected double getEndArrowheadAngle() {
       return getTangentAngleFromAngle(endAngle) + 180;
    }
}
