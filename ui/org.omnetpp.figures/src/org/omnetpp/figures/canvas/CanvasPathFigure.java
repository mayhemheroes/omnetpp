package org.omnetpp.figures.canvas;

import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PrecisionPoint;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.widgets.Display;
import org.omnetpp.common.util.Converter;
import org.omnetpp.figures.misc.Transform;

public class CanvasPathFigure extends AbstractCanvasShape {
    String pathSpec = "";
    PrecisionPoint offset = new PrecisionPoint();
    FillRule fillRule = FillRule.FILL_NONZERO;

    public CanvasPathFigure() {
        updatePath();
    }

    public String getPath() {
        return pathSpec;
    }

    public void setPath(String path) {
        if ((path != null) && !pathSpec.equals(path)) {
            pathSpec = path;
            updatePath();
            repaint();
        }
    }

    public PrecisionPoint getOffset() {
        return offset;
    }

    public void setOffset(Point offset) {
        if ((offset != null) && !(this.offset.equals(offset))) {
            erase();
            Transform tf = getTransform();
            this.offset.setLocation(offset);
            setTransform(tf);
            repaint();
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

    @Override
    protected boolean hitTest(Point point) {
        return super.hitTest(point.getTranslated(offset.getNegated()));
    }

    @Override
    public void paintFigure(Graphics graphics) {
        graphics.translate(offset);
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

    @Override
    public Rectangle getGraphicalBounds() {
        return super.getGraphicalBounds().getTranslated(offset);
    }


    private Boolean parseBoolean(String[] strings, int index) {
        return (strings.length <= index) ? null :
            (strings[index].equals("1") ? true :
                (strings[index].equals("0") ? false : null));
    }

    private Double parseDouble(String[] strings, int index) {
        return (strings.length <= index) ? null : Converter.stringToOptionalDouble(strings[index]);
    }

    private PrecisionPoint parsePoint(String[] strings, int firstIndex) {
        if (strings.length < firstIndex + 2) return null;

        Double x = Converter.stringToOptionalDouble(strings[firstIndex]);
        Double y = Converter.stringToOptionalDouble(strings[firstIndex + 1]);

        return ((x == null) || (y == null)) ? null : new PrecisionPoint(x, y);
    }

    /* from mozilla via TkPath */
    private static double vectorAngle(double ux, double uy, double vx, double vy)
    {
        double ta = Math.atan2(uy, ux);
        double tb = Math.atan2(vy, vx);
        if (tb >= ta) {
            return tb-ta;
        } else {
            return 2.0*Math.PI - (ta-tb);
        }
    }

    // source: TkPath 0.3.1 (tkpath/generic/tkPath.c:925), not checked
    // beware, here be dragons
    private void arcToUsingBezier(Path path,
            PrecisionPoint penPosition,
            double rx, double ry,
            double phiDegrees,  /* The rotation angle in degrees! */
            boolean largeArcFlag, boolean sweepFlag,
            double x2, double y2)
    {
        int i, segments;
        double x1, y1;
        double cx, cy;
        double theta1, dtheta, phi;
        double sinPhi, cosPhi;
        double delta, t;

        x1 = penPosition.preciseX();
        y1 = penPosition.preciseY();

        /* All angles except phi is in radians! */
        phi = Math.toRadians(phiDegrees);


        { // inlined EndpointToCentralArcParameters from tkPath
            double dx, dy;
            double x1dash, y1dash;
            double cxdash, cydash;
            double numerator, root;

            /* 1. Treat out-of-range parameters as described in
             * http://www.w3.org/TR/SVG/implnote.html#ArcImplementationNotes
             *
             * If the endpoints (x1, y1) and (x2, y2) are identical, then this
             * is equivalent to omitting the elliptical arc segment entirely
             */
            if ((Math.abs(x1 - x2) < 0.01) && (Math.abs(y1 - y2) < 0.01)) {
                return;
            }

            /* If rx = 0 or ry = 0 then this arc is treated as a straight line
             * segment (a "lineto") joining the endpoints.
             */
            if ((rx == 0.0f) || (ry == 0.0f)) {
                path.lineTo((float)x2, (float)y2);
                return;
            }

            /* If rx or ry have negative signs, these are dropped; the absolute
             * value is used instead.
             */
            if (rx < 0.0) rx = -rx;
            if (ry < 0.0) ry = -ry;

            /* 2. convert to center parameterization as shown in
             * http://www.w3.org/TR/SVG/implnote.html
             */
            sinPhi = Math.sin(phi);
            cosPhi = Math.cos(phi);
            dx = (x1-x2)/2.0;
            dy = (y1-y2)/2.0;
            x1dash =  cosPhi * dx + sinPhi * dy;
            y1dash = -sinPhi * dx + cosPhi * dy;

            /* Compute cx' and cy'. */
            numerator = rx*rx*ry*ry - rx*rx*y1dash*y1dash - ry*ry*x1dash*x1dash;
            if (numerator < 0.0) {

                /* If rx , ry and are such that there is no solution (basically,
                 * the ellipse is not big enough to reach from (x1, y1) to (x2,
                 * y2)) then the ellipse is scaled up uniformly until there is
                 * exactly one solution (until the ellipse is just big enough).
                 *  -> find factor s, such that numerator' with rx'=s*rx and
                 *    ry'=s*ry becomes 0 :
                 */
                float s = (float) Math.sqrt(1.0 - numerator/(rx*rx*ry*ry));

                rx *= s;
                ry *= s;
                root = 0.0;
            } else {
                root = (largeArcFlag == sweepFlag ? -1.0 : 1.0) *
                        Math.sqrt( numerator/(rx*rx*y1dash*y1dash + ry*ry*x1dash*x1dash) );
            }

            cxdash =  root*rx*y1dash/ry;
            cydash = -root*ry*x1dash/rx;

            /* Compute cx and cy from cx' and cy'. */
            cx = cosPhi * cxdash - sinPhi * cydash + (x1+x2)/2.0;
            cy = sinPhi * cxdash + cosPhi * cydash + (y1+y2)/2.0;

            /* Compute start angle and extent. */
            theta1 = vectorAngle(1.0, 0.0, (x1dash-cxdash)/rx, (y1dash-cydash)/ry);
            dtheta = vectorAngle(
                    (x1dash-cxdash)/rx,  (y1dash-cydash)/ry,
                    (-x1dash-cxdash)/rx, (-y1dash-cydash)/ry);
            if (!sweepFlag && (dtheta > 0.0)) {
                dtheta -= 2.0*Math.PI;
            } else if (sweepFlag && (dtheta < 0.0)) {
                dtheta += 2.0*Math.PI;
            }
        }


        sinPhi = Math.sin(phi);
        cosPhi = Math.cos(phi);

        /* Convert into cubic bezier segments <= 90deg (from mozilla/svg; not checked) */
        segments = (int) Math.ceil(Math.abs(dtheta/(Math.PI/2.0)));
        delta = dtheta/segments;
        t = 8.0/3.0 * Math.sin(delta/4.0) * Math.sin(delta/4.0) / Math.sin(delta/2.0);

        for (i = 0; i < segments; ++i) {
            double cosTheta1 = Math.cos(theta1);
            double sinTheta1 = Math.sin(theta1);
            double theta2 = theta1 + delta;
            double cosTheta2 = Math.cos(theta2);
            double sinTheta2 = Math.sin(theta2);

            /* a) calculate endpoint of the segment: */
            double xe = cosPhi * rx*cosTheta2 - sinPhi * ry*sinTheta2 + cx;
            double ye = sinPhi * rx*cosTheta2 + cosPhi * ry*sinTheta2 + cy;

            /* b) calculate gradients at start/end points of segment: */
            double dx1 = t * ( - cosPhi * rx*sinTheta1 - sinPhi * ry*cosTheta1);
            double dy1 = t * ( - sinPhi * rx*sinTheta1 + cosPhi * ry*cosTheta1);

            double dxe = t * ( cosPhi * rx*sinTheta2 + sinPhi * ry*cosTheta2);
            double dye = t * ( sinPhi * rx*sinTheta2 - cosPhi * ry*cosTheta2);

            /* c) draw the cubic bezier: */
            path.cubicTo((float)(x1+dx1), (float)(y1+dy1), (float)(xe+dxe), (float)(ye+dye), (float)xe, (float)ye);
            /* do next segment */
            theta1 = theta2;
            x1 = (float) xe;
            y1 = (float) ye;
        }
    }


    private void updatePath() {
        path = new Path(Display.getDefault());

        PrecisionPoint penPosition = new PrecisionPoint();
        PrecisionPoint lastControlPoint = new PrecisionPoint();
        PrecisionPoint lastMovePosition = new PrecisionPoint();
        char currentElement = 0;

        String[] parts = pathSpec.split("\\s+");

        if ((parts.length == 0) || ((parts.length == 1) && (parts[0].isEmpty()))
                || (Character.toUpperCase(parts[0].charAt(0)) != 'M')) {
            return;
        }

        for (int index = 0; index < parts.length; ++index) {
            if ("MLHVAQTCSZ".contains(Character.toUpperCase(parts[index].charAt(0)) + "")) {
                currentElement = parts[index].charAt(0);
                ++index;
            }

            switch (currentElement) {
                case 'm': {
                    PrecisionPoint delta = parsePoint(parts, index);
                    if (delta == null) break;
                    ++index;
                    penPosition.translate(delta);
                    lastMovePosition.setLocation(penPosition);
                    path.moveTo((float)penPosition.preciseX(), (float)penPosition.preciseY());
                } break;
                case 'M': {
                    PrecisionPoint pos = parsePoint(parts, index);
                    if (pos == null) break;
                    ++index;
                    penPosition = pos;
                    lastMovePosition.setLocation(penPosition);
                    path.moveTo((float)penPosition.preciseX(), (float)penPosition.preciseY());
                } break;
                case 'l': {
                    PrecisionPoint delta = parsePoint(parts, index);
                    if (delta == null) break;
                    ++index;
                    penPosition.translate(delta);
                    path.lineTo((float)penPosition.preciseX(), (float)penPosition.preciseY());
                } break;
                case 'L': {
                    PrecisionPoint pos = parsePoint(parts, index);
                    if (pos == null) break;
                    ++index;
                    penPosition = pos;
                    path.lineTo((float)penPosition.preciseX(), (float)penPosition.preciseY());
                } break;
                case 'h': {
                    Double delta = parseDouble(parts, index);
                    if (delta == null) break;
                    penPosition.translate(delta, 0);
                    path.lineTo((float)penPosition.preciseX(), (float)penPosition.preciseY());
                } break;
                case 'H': {
                    Double pos = parseDouble(parts, index);
                    if (pos == null) break;
                    penPosition.setPreciseX(pos);
                    path.lineTo((float)penPosition.preciseX(), (float)penPosition.preciseY());
                } break;
                case 'v': {
                    Double delta = parseDouble(parts, index);
                    if (delta == null) break;
                    penPosition.translate(0, delta);
                    path.lineTo((float)penPosition.preciseX(), (float)penPosition.preciseY());
                } break;
                case 'V': {
                    Double pos = parseDouble(parts, index);
                    if (pos == null) break;
                    penPosition.setPreciseY(pos);
                    path.lineTo((float)penPosition.preciseX(), (float)penPosition.preciseY());
                } break;
                case 'a': {
                    PrecisionPoint radii = parsePoint(parts, index);
                    Double phi = parseDouble(parts, index + 2);
                    Boolean largeArc = parseBoolean(parts, index + 3);
                    Boolean sweep = parseBoolean(parts, index + 4);
                    PrecisionPoint to = parsePoint(parts, index + 5);
                    if ((radii == null) || (phi == null) || (largeArc == null) || (sweep == null) || (to == null)) break;
                    index += 6;
                    to.translate(penPosition);
                    arcToUsingBezier(path, penPosition,
                            radii.preciseX(), radii.preciseY(),
                            phi, largeArc, sweep,
                            to.preciseX(), to.preciseY());
                    penPosition = to;
                } break;
                case 'A': {
                    PrecisionPoint radii = parsePoint(parts, index);
                    Double phi = parseDouble(parts, index + 2);
                    Boolean largeArc = parseBoolean(parts, index + 3);
                    Boolean sweep = parseBoolean(parts, index + 4);
                    PrecisionPoint to = parsePoint(parts, index + 5);
                    if ((radii == null) || (phi == null) || (largeArc == null) || (sweep == null) || (to == null)) break;
                    index += 6;
                    arcToUsingBezier(path, penPosition,
                            radii.preciseX(), radii.preciseY(),
                            phi, largeArc, sweep,
                            to.preciseX(), to.preciseY());
                    penPosition = to;
                } break;
                case 'q': {
                    PrecisionPoint control = parsePoint(parts, index);
                    PrecisionPoint to = parsePoint(parts, index + 2);
                    if ((control == null) || (to == null)) break;
                    index += 3;
                    control.translate(penPosition); // to make it absolute
                    lastControlPoint = control;
                    penPosition.translate(to);
                    path.quadTo((float)control.preciseX(), (float)control.preciseY(),
                            (float)penPosition.preciseX(), (float)penPosition.preciseY());
                } break;
                case 'Q': {
                    PrecisionPoint control = parsePoint(parts, index);
                    PrecisionPoint to = parsePoint(parts, index + 2);
                    if ((control == null) || (to == null)) break;
                    index += 3;
                    lastControlPoint = control;
                    penPosition = to;
                    path.quadTo((float)control.preciseX(), (float)control.preciseY(),
                            (float)penPosition.preciseX(), (float)penPosition.preciseY());
                } break;
                case 't': {
                    PrecisionPoint to = parsePoint(parts, index);
                    if (to == null) break;
                    ++index;
                    lastControlPoint.translate(penPosition.getDifference(lastControlPoint).getScaled(2.0)); // mirroring
                    penPosition.translate(to);
                    path.quadTo((float)lastControlPoint.preciseX(), (float)lastControlPoint.preciseY(),
                            (float)penPosition.preciseX(), (float)penPosition.preciseY());
                } break;
                case 'T': {
                    PrecisionPoint to = parsePoint(parts, index);
                    if (to == null) break;
                    ++index;
                    lastControlPoint.translate(penPosition.getDifference(lastControlPoint).getScaled(2.0)); // mirroring
                    penPosition = to;
                    path.quadTo((float)lastControlPoint.preciseX(), (float)lastControlPoint.preciseY(),
                            (float)penPosition.preciseX(), (float)penPosition.preciseY());
                } break;
                case 'c': {
                    PrecisionPoint control1 = parsePoint(parts, index);
                    PrecisionPoint control2 = parsePoint(parts, index + 2);
                    PrecisionPoint to = parsePoint(parts, index + 4);
                    if ((control1 == null) || (control2 == null) || (to == null)) break;
                    index += 5;
                    control1.translate(penPosition); // to make it absolute
                    control2.translate(penPosition); // to make it absolute
                    lastControlPoint = control2;
                    penPosition.translate(to);
                    path.cubicTo((float)control1.preciseX(), (float)control1.preciseY(),
                            (float)control2.preciseX(), (float)control2.preciseY(),
                            (float)penPosition.preciseX(), (float)penPosition.preciseY());
                } break;
                case 'C': {
                    PrecisionPoint control1 = parsePoint(parts, index);
                    PrecisionPoint control2 = parsePoint(parts, index + 2);
                    PrecisionPoint to = parsePoint(parts, index + 4);
                    if ((control1 == null) || (control2 == null) || (to == null)) break;
                    index += 5;
                    lastControlPoint = control2;
                    penPosition = to;
                    path.cubicTo((float)control1.preciseX(), (float)control1.preciseY(),
                            (float)control2.preciseX(), (float)control2.preciseY(),
                            (float)penPosition.preciseX(), (float)penPosition.preciseY());
                } break;
                case 's': {
                    PrecisionPoint control1 = lastControlPoint;
                    control1.translate(penPosition.getDifference(control1).getScaled(2.0));
                    PrecisionPoint control2 = parsePoint(parts, index);
                    PrecisionPoint to = parsePoint(parts, index + 2);
                    if ((control2 == null) || (to == null)) break;
                    index += 3;
                    control2.translate(penPosition); // to make it absolute
                    lastControlPoint = control2;
                    penPosition.translate(to);
                    path.cubicTo((float)control1.preciseX(), (float)control1.preciseY(),
                            (float)control2.preciseX(), (float)control2.preciseY(),
                            (float)penPosition.preciseX(), (float)penPosition.preciseY());
                } break;
                case 'S': {
                    PrecisionPoint control1 = lastControlPoint;
                    control1.translate(penPosition.getDifference(control1).getScaled(2.0));
                    PrecisionPoint control2 = parsePoint(parts, index);
                    PrecisionPoint to = parsePoint(parts, index + 2);
                    if ((control2 == null) || (to == null)) break;
                    index += 3;
                    lastControlPoint = control2;
                    penPosition = to;
                    path.cubicTo((float)control1.preciseX(), (float)control1.preciseY(),
                            (float)control2.preciseX(), (float)control2.preciseY(),
                            (float)penPosition.preciseX(), (float)penPosition.preciseY());
                } break;
                case 'z':
                case 'Z': {
                    path.close();
                    --index;
                } break;
            }
        }
    }
}
