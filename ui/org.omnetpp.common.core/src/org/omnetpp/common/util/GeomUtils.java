/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.common.util;

import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.draw2d.geometry.Transform;

/**
 * Various 2D geometry routines.
 *
 * @author tomi
 */
public class GeomUtils {

    public static Insets subtract(Rectangle outer, Rectangle inner) {
        return new Insets(Math.max(inner.y - outer.y, 0),
                  Math.max(inner.x - outer.x, 0),
                  Math.max(outer.bottom() - inner.bottom(), 0),
                  Math.max(outer.right() - inner.right(), 0));
    }

    public static Rectangle add(Rectangle rect, Insets insets) {
        return rect.getExpanded(insets);
    }

    public static Rectangle subtract(Rectangle rect, Insets insets) {
        return rect.getShrinked(insets);
    }

    /**
     * Rounds a coordinate value based on the specified zoom level.
     * The rounding ensures coordinates have no more digits than makes sense
     * considering the granularity allowed by the zoom level.
     *
     * E.g. for a zoom of 1x..9x, round to integers; for a zoom of 10x..99x, round to 1 decimal place, etc.
     */
    public static double roundForZoom(double value, double scale) {
        double pow10 = 1.0;
        while (pow10 < scale)
            pow10 *= 10.0;
        return Math.round(value * pow10) / pow10;
    }

    /**
     * Calculates bounding box of a rotated rectangle. Rotation is in *degrees*.
     */
    public static Dimension rotatedSize(Dimension size, double rotation) {
        if (rotation == 0 || rotation == 180)  // avoid rounding errors in spec cases
            return size.getCopy();
        else if (rotation == 90 || rotation == 270)
            return new Dimension(size.height, size.width);
        else {
            Transform transform = new Transform();
            transform.setRotation(Math.toRadians(rotation));
            Point p1 = transform.getTransformed(new Point((size.width+1), (size.height+1)));
            Point p2 = transform.getTransformed(new Point((size.width+1), - (size.height+1)));
            return new Dimension(
                    Math.max(Math.abs(p1.x), Math.abs(p2.x)),
                    Math.max(Math.abs(p1.y), Math.abs(p2.y)));
        }
    }
}
