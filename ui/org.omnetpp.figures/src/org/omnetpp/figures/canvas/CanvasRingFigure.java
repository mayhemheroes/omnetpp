package org.omnetpp.figures.canvas;

import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PrecisionDimension;
import org.eclipse.draw2d.geometry.PrecisionRectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.widgets.Display;
import org.omnetpp.figures.misc.AnchoredRectangle;


public class CanvasRingFigure extends AbstractCanvasShape {
    private AnchoredRectangle definingRectangle;
    private PrecisionDimension innerSize;

    public CanvasRingFigure() {
        definingRectangle = new AnchoredRectangle();
        innerSize = new PrecisionDimension();
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

    public PrecisionDimension getInnerSize() {
        return innerSize;
    }

    public void setInnerSize(Dimension dim) {
        if ((dim != null) && (!innerSize.equals(dim))) {
            erase();
            innerSize.setSize(dim);
            updatePath();
            repaint();
        }
    }

    private void updatePath() {
        PrecisionRectangle boundingRectangle = definingRectangle.getRectangle();
        path = new Path(Display.getDefault());
        path.addArc((float)boundingRectangle.preciseX(), (float)boundingRectangle.preciseY(),
                (float)boundingRectangle.preciseWidth(), (float)boundingRectangle.preciseHeight(), 0, 360);
        path.close();

        PrecisionRectangle innerRectangle = new PrecisionRectangle();
        Point center = boundingRectangle.getCenter();
        innerRectangle.setPreciseLocation(center.preciseX() - innerSize.preciseWidth() / 2.0,
                center.preciseY() - innerSize.preciseHeight() / 2.0);
        innerRectangle.setSize(innerSize);
        path.addArc((float)innerRectangle.preciseX(), (float)innerRectangle.preciseY(),
                (float)innerRectangle.preciseWidth(), (float)innerRectangle.preciseHeight(), 0, 360);
        path.close();
    }

    @Override
    public void paintFigure(Graphics graphics) {
        graphics.setFillRule(SWT.FILL_EVEN_ODD); // to make the inner circle not filled
        super.paintFigure(graphics);
    }
}
