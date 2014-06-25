package org.omnetpp.ned.editor.graph.parts.canvas;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PrecisionPoint;
import org.omnetpp.figures.canvas.CanvasGroupFigure;
import static org.omnetpp.common.canvas.CanvasFigureConstants.*;

public class GroupEditPart extends AbstractCanvasFigureEditPart {
    @Override
    protected boolean representsType(String type) {
        return type.equals(FTYPE_GROUP);
    }

    @Override
    protected CanvasGroupFigure createFigure() {
        return new CanvasGroupFigure();
    }

    @Override
    public Point getLocation() {
        return new PrecisionPoint(); // just to not return null
    }

    @Override
    public void setLocation(Point loc) {
        // Nothing.
    }
}
