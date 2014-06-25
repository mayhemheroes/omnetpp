package org.omnetpp.ned.editor.graph.parts.canvas;

import java.util.List;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PrecisionPoint;
import org.omnetpp.figures.canvas.CanvasPolylineFigure;
import org.omnetpp.ned.editor.graph.misc.CanvasFigureUtils;

import static org.omnetpp.common.canvas.CanvasFigureConstants.*;

public class PolylineEditPart extends AbstractCanvasLineEditPart {

    @Override
    protected boolean representsType(String type) {
        return type.equals(FTYPE_LINE) || type.equals(FTYPE_POLYLINE);
    }

    @Override // For convenience.
    public CanvasPolylineFigure getFigure() {
        return (CanvasPolylineFigure)super.getFigure();
    }

    @Override
    protected CanvasPolylineFigure createFigure() {
        return new CanvasPolylineFigure();
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
        CanvasPolylineFigure figure = getFigure();

        List<Point> points = parsePointList(PKEY_POINTS);
        figure.setPoints(((points == null) || (points.size() < 2)) ? CanvasFigureUtils.getDefaultPointList() : points);

        Boolean smooth = parseBoolean(PKEY_SMOOTH);
        figure.setSmooth((smooth == null) ? false : smooth);
    }
}
