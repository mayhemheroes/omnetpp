package org.omnetpp.figures.canvas;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PrecisionPoint;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.widgets.Display;

public class CanvasPolylineFigure extends AbstractCanvasLine {
    private List<Point> points;
    private boolean smooth;

    public CanvasPolylineFigure() {
        points = new ArrayList<Point>();
        smooth = false;

        updatePath();
    }

    public boolean getSmooth() {
        return smooth;
    }

    public void setSmooth(boolean smooth) {
        if (smooth != this.smooth) {
            this.smooth = smooth;
            updatePath();
        }
    }

    public List<Point> getPoints() {
        return points;
    }

    public void setPoints(List<Point> points) {
        this.points.clear();
        this.points.addAll(points);

        updateArrowHeads();
        updatePath();
    }

    @Override
    protected Point getStartArrowheadPosition() {
        return points.isEmpty() ? null : points.get(0);
    }

    @Override
    protected double getStartArrowheadAngle() {
        if (points.size() < 2) {
            return 0;
        }

        Point to = points.get(0);
        Point from = points.get(1);

        return Math.toDegrees(Math.atan2(to.preciseY() - from.preciseY(), to.preciseX() - from.preciseX()));
    }

    @Override
    protected Point getEndArrowheadPosition() {
        return points.isEmpty() ? null : points.get(points.size() - 1);
    }

    @Override
    protected double getEndArrowheadAngle() {
        if (points.size() < 2) {
            return 0;
        }

        Point to = points.get(points.size() - 1);
        Point from = points.get(points.size() - 2);

        return Math.toDegrees(Math.atan2(to.preciseY() - from.preciseY(), to.preciseX() - from.preciseX()));
    }

    private void updatePath() {
        path = new Path(Display.getDefault());
        if (points.isEmpty()) {
            recomputeBounds();
            return;
        }

        path.moveTo((float)points.get(0).preciseX(), (float)points.get(0).preciseY());

        if (smooth && (points.size() > 2)) {
            for (int i = 0; i < points.size() - 2; ++i) { // for all the quadratic segments
                Point m, e; // the control points for this quadratic Bezier path segment, it starts from the current position

                m = points.get(i + 1);

                // if this is the last segment, we need it to end on the last point
                e = (i == (points.size() - 3)) ? points.get(i + 2) :
                    new PrecisionPoint(
                            (points.get(i + 1).preciseX() + points.get(i + 2).preciseX()) / 2.0,
                            (points.get(i + 1).preciseY() + points.get(i + 2).preciseY()) / 2.0);

                path.quadTo((float)m.preciseX(), (float)m.preciseY(), (float)e.preciseX(), (float)e.preciseY());
            }
        } else {  // if the polyline is not smoothed, simply adding the points to the path
            for (Point p : points) {
                path.lineTo((float)p.preciseX(), (float)p.preciseY());
            }
        }

        recomputeBounds();
    }

    @Override
    protected void outlineShape(Graphics graphics) {
        graphics.drawPath(path);
    }
}
