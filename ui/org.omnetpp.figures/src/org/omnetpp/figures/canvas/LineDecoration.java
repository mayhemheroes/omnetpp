package org.omnetpp.figures.canvas;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PrecisionPoint;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.graphics.Color;
import org.omnetpp.common.color.ColorFactory;
import org.omnetpp.figures.canvas.AbstractCanvasLine.ArrowHead;
import org.omnetpp.figures.canvas.AbstractCanvasLine.CapStyle;
import org.omnetpp.figures.canvas.AbstractCanvasShape.JoinStyle;
import org.omnetpp.figures.misc.PrecisionTransform;

/**
 * A simple helper class to draw arrowheads on the ends of lines. It's not even a Figure.
 */
public class LineDecoration {
    private PrecisionTransform transform = new PrecisionTransform();
    private ArrowHead style = ArrowHead.ARROW_NONE;
    private Color color = ColorFactory.BLACK;
    private double width = 1.0;
    private double opacity = 1.0;

    private AbstractCanvasShape shape;

    private static List<Point> trianglePoints = new ArrayList<Point>();
    private static List<Point> barbedPoints = new ArrayList<Point>();

    static {
        trianglePoints.add(new PrecisionPoint(-3, -3));
        trianglePoints.add(new PrecisionPoint(0, 0));
        trianglePoints.add(new PrecisionPoint(-3, 3));

        barbedPoints.add(new PrecisionPoint(-4, -3));
        barbedPoints.add(new PrecisionPoint(0, 0));
        barbedPoints.add(new PrecisionPoint(-4, 3));
        barbedPoints.add(new PrecisionPoint(-2, 0));
    }

    public boolean hitTest(Point point) {
        return (shape == null) ? false : shape.hitTest(point);
    }

    // TODO optimize this, and only call it when needed
    private void updateShape() {
        List<Point> transformedPoints = new ArrayList<Point>();

        List<Point> points = null;
        boolean closed = false;

        switch (style) {
        case ARROW_NONE:
            shape = null;
            return;
        case ARROW_SIMPLE:
            points = trianglePoints;
            closed = false;
            break;
        case ARROW_TRIANGLE:
            points = trianglePoints;
            closed = true;
            break;
        case ARROW_BARBED:
            points = barbedPoints;
            closed = true;
            break;
        }

        for (Point p : points) {
            transformedPoints.add(transform.getTransformed(p));
        }

        if (closed) {
            CanvasPolygonFigure polygon = new CanvasPolygonFigure();
            polygon.setPoints(transformedPoints);

            shape = polygon;
        } else {
            CanvasPolylineFigure polyline = new CanvasPolylineFigure();
            polyline.setPoints(transformedPoints);

            polyline.setCapStyle(CapStyle.CAP_SQUARE);
            shape = polyline;
        }

        shape.setFill(true);
        shape.setOutline(true);

        // line width compensation on zooming is done by the "pseudo-parent"
        // (the line figure on the end of which this decoration is), the zoom
        // property of shape is kept at 1
        shape.setLineWidth((float)width);
        shape.setJoinStyle(JoinStyle.JOIN_MITER);
        shape.setForegroundColor(color);
        shape.setBackgroundColor(color);
        shape.setFillOpacity(opacity);
        shape.setLineOpacity(opacity);
    }

    public LineDecoration(ArrowHead s) {
        style = s;
        updateShape();
    }

    public void setLocation(Point loc) {
        transform.setTranslation(loc.preciseX(), loc.preciseY());
        updateShape();
    }

    public void setRotation(double angle) {
        transform.setRotation(angle);
        updateShape();
    }

    public void setColor(Color c) {
        if (!color.equals(c)) {
            color = c;
            updateShape();
        }
    }

    public void setOpacity(double opacity) {
        opacity = Math.min(Math.max(opacity, 0), 1); // clamping
        if (this.opacity != opacity) {
            this.opacity = opacity;
            updateShape();
            if (shape != null) {
                shape.repaint();
            }
        }
    }

    public void setScale(double scale) {
        transform.setScale(scale);
        updateShape();
    }

    public void setLineWidth(double w) {
        if (width != w) {
            width = w;
            updateShape();
        }
    }

    public void paint(Graphics g) {
        if (shape != null) {
            // To avoid the "clipping modified in a way that cannot be saved or restored" bug in SWTGraphics
            // also, Graphics expects the clip rect in the transformed coordinate system, so we transform
            g.setClip(shape.getCascadedTransform().getInverse().applyTo(getBounds()));
            shape.paint(g);
        }
    }

    public Rectangle getBounds() {
        return (shape == null) ? null : shape.getBounds();
    }
}
