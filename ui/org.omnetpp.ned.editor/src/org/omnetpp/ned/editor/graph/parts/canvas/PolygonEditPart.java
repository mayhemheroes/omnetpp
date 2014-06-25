package org.omnetpp.ned.editor.graph.parts.canvas;

import java.util.List;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PrecisionPoint;
import org.omnetpp.figures.canvas.AbstractCanvasShape.FillRule;
import org.omnetpp.figures.canvas.CanvasPolygonFigure;
import org.omnetpp.ned.editor.graph.misc.CanvasFigureUtils;

import static org.omnetpp.common.canvas.CanvasFigureConstants.*;

public class PolygonEditPart extends AbstractCanvasShapeEditPart {

    @Override
    protected boolean representsType(String type) {
        return type.equals(FTYPE_POLYGON);
    }

    @Override // For convenience.
    public CanvasPolygonFigure getFigure() {
        return (CanvasPolygonFigure)super.getFigure();
    }

    @Override
    protected CanvasPolygonFigure createFigure() {
        return new CanvasPolygonFigure();
    }

    @Override
    public Point getLocation() {
        return parsePointList(PKEY_POINTS).get(0);
    }

    @Override
    public void setLocation(Point loc) {
        List<Point> points = parsePointList(PKEY_POINTS);

        PrecisionPoint delta = new PrecisionPoint(loc.preciseX() - points.get(0).preciseX(),
                loc.preciseY() - points.get(0).preciseY());

        double scale = getScale();
        for (int i = 0; i < points.size(); i++) {
            Point point = points.get(i);
            PrecisionPoint pp = new PrecisionPoint(point.preciseX() + delta.preciseX(), point.preciseY() + delta.preciseY());
            CanvasFigureUtils.roundForZoom(pp, scale);
            points.set(i, pp);
        }

        setPointList(PKEY_POINTS, points);
    }

    @Override
    protected void refreshVisuals() {
        super.refreshVisuals();
        CanvasPolygonFigure figure = getFigure();

        List<Point> points = parsePointList(PKEY_POINTS);
        figure.setPoints(((points == null) || (points.size() < 2)) ? CanvasFigureUtils.getDefaultPointList() : points);

        Boolean smooth = parseBoolean(PKEY_SMOOTH);
        figure.setSmooth((smooth == null) ? false : smooth);

        FillRule fillRule = parseFillRule(PKEY_FILLRULE);
        figure.setFillRule((fillRule == null) ? FillRule.FILL_NONZERO : fillRule);
    }
}
