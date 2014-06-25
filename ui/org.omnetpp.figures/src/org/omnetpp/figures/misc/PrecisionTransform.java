package org.omnetpp.figures.misc;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PrecisionPoint;

/**
 * A more precise version of {@link org.eclipse.draw2d.geometry.Transform}.
 */
public class PrecisionTransform {
    private double scaleX = 1.0, scaleY = 1.0, dx, dy, cos = 1.0, sin;

    /**
     * Sets the value for the amount of scaling to be done along both axes.
     *
     * @param scale
     *            Scale factor
     */
    public void setScale(double scale) {
        scaleX = scaleY = scale;
    }

    /**
     * Sets the value for the amount of scaling to be done along X and Y axes
     * individually.
     *
     * @param x
     *            Amount of scaling on X axis
     * @param y
     *            Amount of scaling on Y axis
     */
    public void setScale(double x, double y) {
        scaleX = x;
        scaleY = y;
    }

    /**
     * Sets the rotation angle.
     *
     * @param angle
     *            Angle of rotation in degrees.
     */
    public void setRotation(double angle) {
        cos = Math.cos(Math.toRadians(angle));
        sin = Math.sin(Math.toRadians(angle));
    }

    /**
     * Sets the translation amounts for both axes.
     *
     * @param x
     *            Amount of shift on X axis
     * @param y
     *            Amount of shift on Y axis
     */
    public void setTranslation(double x, double y) {
        dx = x;
        dy = y;
    }

    /**
     * Returns a new transformed Point of the input Point based on the
     * transformation values set.
     *
     * @param p
     *            Point being transformed
     * @return The transformed Point
     */
    public PrecisionPoint getTransformed(Point p) {
        double x = p.preciseX();
        double y = p.preciseY();
        double temp;
        x *= scaleX;
        y *= scaleY;

        temp = x * cos - y * sin;
        y = x * sin + y * cos;
        x = temp;

        return new PrecisionPoint(x + dx, y + dy);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PrecisionTransform)) return false;

        PrecisionTransform t = (PrecisionTransform)obj;

        return (scaleX == t.scaleX) && (scaleY == t.scaleY)
                && (dx == t.dx) && (dy == t.dy)
                && (cos == t.cos) && (sin == t.sin);
    }
}
