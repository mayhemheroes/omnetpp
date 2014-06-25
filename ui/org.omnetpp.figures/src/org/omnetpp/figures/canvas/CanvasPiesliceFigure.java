package org.omnetpp.figures.canvas;

import org.eclipse.draw2d.geometry.PrecisionRectangle;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.widgets.Display;
import org.omnetpp.figures.misc.AnchoredRectangle;

public class CanvasPiesliceFigure extends AbstractCanvasShape {

    private AnchoredRectangle definingRectangle;

    private double startAngle;
    private double endAngle;

    public CanvasPiesliceFigure() {
        definingRectangle = new AnchoredRectangle();

        startAngle = 0;
        endAngle = 360;

        updatePath();
    }

    public AnchoredRectangle getDefiningRectangle() {
        return definingRectangle;
    }

    public void setDefiningRectangle(AnchoredRectangle rectangle) {
        if ((rectangle != null) && (!definingRectangle.equals(rectangle))) {
            erase();
            definingRectangle = rectangle;
            updatePath();
            repaint();
        }
    }

    private void updatePath() {
        PrecisionRectangle boundingRectangle = definingRectangle.getRectangle();

        path = new Path(Display.getDefault());

        path.addArc((float)boundingRectangle.preciseX(), (float)boundingRectangle.preciseY(),
                (float)boundingRectangle.preciseWidth() - 1, (float)boundingRectangle.preciseHeight() - 1,
                (float)startAngle, (float)(endAngle - startAngle));

        path.lineTo(
                (float)(boundingRectangle.preciseX() + (boundingRectangle.preciseWidth()) / 2.0),
                (float)(boundingRectangle.preciseY() + (boundingRectangle.preciseHeight()) / 2.0));

        path.close();
    }

    /**
     * Gets the angle at which the pieslice is beginning.
     *
     * In degrees, with 0° pointing right, and the positive direction is CCW.
     */
    public double getStartAngle() {
        return startAngle;
    }

    /**
     * Sets the angle at which the pieslice is beginning.
     *
     * In degrees, with 0° pointing right, and the positive direction is CCW.
     */
    public void setStartAngle(double startAngle) {
        if (startAngle != this.startAngle) {
            this.startAngle = startAngle;
            updatePath();
        }
    }

    /**
     * Gets the angle at which the pieslice is ending.
     *
     * In degrees, with 0° pointing right, and the positive direction is CCW.
     */
    public double getEndAngle() {
        return endAngle;
    }

    /**
     * Sets the angle at which the pieslice is ending.
     *
     * In degrees, with 0° pointing right, and the positive direction is CCW.
     */
    public void setEndAngle(double endAngle) {
        if (endAngle != this.endAngle) {
            this.endAngle = endAngle;
            updatePath();
        }
    }
}
