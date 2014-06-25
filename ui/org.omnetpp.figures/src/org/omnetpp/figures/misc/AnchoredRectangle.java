package org.omnetpp.figures.misc;

import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PrecisionDimension;
import org.eclipse.draw2d.geometry.PrecisionPoint;
import org.eclipse.draw2d.geometry.PrecisionRectangle;
import org.eclipse.draw2d.geometry.Rectangle;

public class AnchoredRectangle {

    // the ANCHOR_NONE option is mostly used when the rectangle was specified with the "bounds" property key
    // or when the anchor key is missing in general
    public enum Anchor { ANCHOR_NONE, ANCHOR_C, ANCHOR_N, ANCHOR_E, ANCHOR_S, ANCHOR_W, ANCHOR_NW, ANCHOR_NE, ANCHOR_SE, ANCHOR_SW;
    public String toString() {
        return (this == ANCHOR_NONE) ? "" : name().substring(7).toLowerCase();
        };
    }

    private Anchor anchor = Anchor.ANCHOR_NONE;

    private double x, y;
    private double width, height;

    public AnchoredRectangle() {

    }

    public AnchoredRectangle(AnchoredRectangle other) {
        anchor = other.anchor;
        x = other.x;
        y = other.y;
        width = other.width;
        height = other.height;
    }

    public AnchoredRectangle(Rectangle bounds) {
        this(bounds.preciseX(), bounds.preciseY(), bounds.preciseWidth(), bounds.preciseHeight());
    }

    public AnchoredRectangle(double x, double y, double width, double height) {
        this(x, y, width, height, Anchor.ANCHOR_NONE);
    }

    public AnchoredRectangle(double x, double y, double width, double height, Anchor anchor) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.anchor = anchor;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof AnchoredRectangle)) {
            return false;
        }

        AnchoredRectangle other = (AnchoredRectangle)obj;

        return (other.anchor == anchor)
                && (other.x == x) && (other.y == y)
                && (other.width == width) && (other.height == height);
    }

    /**
     * Returns the correctly anchored PrecisionRectangle.
     */
    public PrecisionRectangle getRectangle() {
        return (PrecisionRectangle)(new PrecisionRectangle(x, y, width, height).translate(getAnchoringOffset()));
    }

    public Anchor getAnchor() {
        return anchor;
    }

    public void setAnchor(Anchor anchor) {
        if (anchor != null) {
            this.anchor = anchor;
        }
    }

    public PrecisionPoint getLocation() {
        return new PrecisionPoint(x, y);
    }

    public void setLocation(Point location) {
        if (location != null) {
            x = location.preciseX();
            y = location.preciseY();
        }
    }

    public PrecisionDimension getSize() {
        return new PrecisionDimension(width, height);
    }

    public void setSize(Dimension size) {
        if (size != null) {
            width = size.preciseWidth();
            height = size.preciseHeight();
        }
    }

    public PrecisionPoint getAnchoringOffset() {
        if (anchor == null) {
            return new PrecisionPoint(0, 0);
        }

        switch (anchor) {
        case ANCHOR_C:
            return new PrecisionPoint(-width / 2.0, -height / 2.0);
        case ANCHOR_E:
            return new PrecisionPoint(-width, -height / 2.0);
        case ANCHOR_N:
            return new PrecisionPoint(-width / 2.0, 0);
        case ANCHOR_NE:
            return new PrecisionPoint(-width, 0);
        case ANCHOR_NW:
            return new PrecisionPoint(0, 0);
        case ANCHOR_S:
            return new PrecisionPoint(-width / 2.0, -height);
        case ANCHOR_SE:
            return new PrecisionPoint(-width, -height);
        case ANCHOR_SW:
            return new PrecisionPoint(0, -height);
        case ANCHOR_W:
            return new PrecisionPoint(0, -height / 2.0);
        default:
            return new PrecisionPoint(0, 0);
        }
    }
}
