package org.omnetpp.figures.canvas;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PrecisionPoint;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.widgets.Display;

public class CanvasPolygonFigure extends AbstractCanvasShape {
    private List<Point> points;
    private boolean smooth;
    FillRule fillRule = FillRule.FILL_NONZERO;

    public CanvasPolygonFigure() {
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

    public FillRule getFillRule() {
        return fillRule;
    }

    public void setFillRule(FillRule fillRule) {
        if ((fillRule != null) && !(this.fillRule.equals(fillRule))) {
            erase();
            this.fillRule = fillRule;
            repaint();
        }
    }

    public List<Point> getPoints() {
        return points;
    }

    public void setPoints(List<Point> points) {
        this.points.clear();
        this.points.addAll(points);

        updatePath();
    }

    @Override
    public void paintFigure(Graphics graphics) {
        switch (fillRule) {
        case FILL_NONZERO:
            graphics.setFillRule(SWT.FILL_WINDING);
            break;
        case FILL_EVENODD:
            graphics.setFillRule(SWT.FILL_EVEN_ODD);
            break;
        }
        super.paintFigure(graphics);
    }

    private void updatePath() {
        path = new Path(Display.getDefault());

        if (points.isEmpty()) {
            return;
        }

        if (smooth && (points.size() > 2)) {

              path.moveTo((float)(points.get(0).preciseX() + points.get(1).preciseX()) / 2.0f,
                      (float)(points.get(0).preciseY() + points.get(1).preciseY()) / 2.0f);

              for (int i = 0; i < points.size(); ++i) { // for all the quadratic segments

                  int i1 = (i + 1) % points.size(); // helpers for overflow avoidance
                  int i2 = (i + 2) % points.size();

                  Point m, e; // the control points for this quadratic Bezier path segment, it starts from the current position

                  m = points.get(i1);

                  e = new PrecisionPoint(
                          (points.get(i1).preciseX() + points.get(i2).preciseX()) / 2.0,
                          (points.get(i1).preciseY() + points.get(i2).preciseY()) / 2.0);

                  path.quadTo(m.x, m.y, e.x, e.y);
              }
          } else { // if the polygon is not smoothed, simply adding the points to the path
              path.moveTo((float)points.get(0).preciseX(), (float)points.get(0).preciseY());
              for (Point p : points) {
                  path.lineTo((float)p.preciseX(), (float)p.preciseY());
              }
          }

          path.close();
    }
}
