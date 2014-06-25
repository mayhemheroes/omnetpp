package org.omnetpp.ned.editor.graph.parts.canvas;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PrecisionPoint;
import org.omnetpp.figures.canvas.AbstractCanvasShape.FillRule;
import org.omnetpp.figures.canvas.CanvasPathFigure;
import static org.omnetpp.common.canvas.CanvasFigureConstants.*;
import org.omnetpp.ned.model.ex.PropertyElementEx;

public class PathEditPart extends AbstractCanvasShapeEditPart {

    @Override
    protected boolean representsType(String type) {
        return type.equals(FTYPE_PATH);
    }

    @Override // For convenience.
    public CanvasPathFigure getFigure() {
        return (CanvasPathFigure)super.getFigure();
    }

    @Override
    protected IFigure createFigure() {
        return new CanvasPathFigure();
    }

    @Override
    public Point getLocation() {
        Point loc = parsePoint(PKEY_OFFSET);
        return (loc == null) ? new PrecisionPoint() : loc;
    }

    @Override
    public void setLocation(Point loc) {
        setPoint(PKEY_OFFSET, loc);
    }

    @Override
    protected void refreshVisuals() {
        super.refreshVisuals();

        CanvasPathFigure figure = getFigure();
        PropertyElementEx model = getModel();
        if (model == null) {
            return;
        }

        figure.setPath(model.getValue(PKEY_PATH));
        PrecisionPoint offset = parsePoint(PKEY_OFFSET);
        figure.setOffset((offset == null) ? new PrecisionPoint() : offset);
        FillRule fillRule = parseFillRule(PKEY_FILLRULE);
        figure.setFillRule((fillRule == null) ? FillRule.FILL_NONZERO : fillRule);
    }
}
