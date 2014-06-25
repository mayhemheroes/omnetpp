package org.omnetpp.figures.canvas;

import org.eclipse.draw2d.geometry.PrecisionRectangle;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.widgets.Display;
import org.omnetpp.figures.misc.AnchoredRectangle;

public class CanvasRectangleFigure extends AbstractCanvasShape {
    private AnchoredRectangle definingRectangle;
    private double cornerRx = 0, cornerRy = 0;

    public CanvasRectangleFigure() {
        definingRectangle = new AnchoredRectangle();
        updatePath();
    }

    public AnchoredRectangle getDefiningRectangle() {
        return definingRectangle;
    }

    public void setDefiningRectangle(AnchoredRectangle rectangle) {
        if ((rectangle != null) && (!definingRectangle.equals(rectangle))) {
            erase();
            definingRectangle = new AnchoredRectangle(rectangle);
            updatePath();
            repaint();
        }
    }

    public double getCornerRadiusX() {
        return cornerRx;
    }

    public double getCornerRadiusY() {
        return cornerRy;
    }

    public void setCornerRadius(double r) {
        setCornerRadius(r, r);
    }

    public void setCornerRadius(double rx, double ry) {
        if ((cornerRx != rx) || (cornerRy != ry)) {
            cornerRx = rx;
            cornerRy = ry;

            updatePath();
            repaint();
        }
    }

    private void updatePath() {
        path = new Path(Display.getDefault());

        PrecisionRectangle boundingRectangle = definingRectangle.getRectangle();
        float x1 = (float)boundingRectangle.preciseX();
        float y1 = (float)boundingRectangle.preciseY();
        float w = (float)boundingRectangle.preciseWidth();
        float h = (float)boundingRectangle.preciseHeight();
        float x2 = x1 + w;
        float y2 = y1 + h;
        float rx = (float)Math.max(0, Math.min(cornerRx, w / 2));
        float ry = (float)Math.max(0, Math.min(cornerRy, h / 2));
        float dx = 2 * rx;
        float dy = 2 * ry;

        path.moveTo(x1 + rx, y1);
        path.lineTo(x2 - rx, y1); // top line
        path.addArc(x2 - dx, y1, dx, dy, 90, -90); // top right corner
        path.lineTo(x2, y2 - ry); // right line
        path.addArc(x2 - dx, y2 - dy, dx, dy, 0, -90); // bottom right corner
        path.lineTo(x1 + rx, y2); // bottom line
        path.addArc(x1, y2 - dy, dx, dy, -90, -90); // bottom left corner
        path.lineTo(x1, y1 + ry); // left line
        path.addArc(x1, y1, dx, dy, -180, -90); // top left corner
        path.close();
    }
}
