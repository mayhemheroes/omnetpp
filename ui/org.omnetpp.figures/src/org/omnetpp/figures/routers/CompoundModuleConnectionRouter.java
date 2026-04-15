/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.figures.routers;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.draw2d.AbstractRouter;
import org.eclipse.draw2d.Connection;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.draw2d.geometry.Rectangle;
import org.omnetpp.figures.CompoundModuleFigure;
import org.omnetpp.figures.anchors.CompoundModuleGateAnchor;
import org.omnetpp.figures.anchors.IAnchorBounds;
import org.omnetpp.layout.engine.LayoutEngine;
import org.omnetpp.layout.engine.Pt;
import org.omnetpp.layout.engine.PtVector;
import org.omnetpp.layout.engine.Rc;
import org.omnetpp.layout.engine.RoutingConstraint;

/**
 * Connection router that uses the native arrowcoords() algorithm from the layout
 * library for submodule-to-submodule connections. This produces the same visual
 * result as Qtenv's runtime rendering. Self-connections are drawn as circular arcs.
 * Connections involving CompoundModuleGateAnchors fall back to the anchor-based
 * routing for correct compound module border placement.
 *
 * @author rhornig
 */
public class CompoundModuleConnectionRouter extends AbstractRouter
{
    private Map<Connection, Object> constraints = new HashMap<>();

    public CompoundModuleConnectionRouter() { }

    @Override
    public void setConstraint(Connection connection, Object constraint) {
        constraints.put(connection, constraint);
    }

    @Override
    public void remove(Connection connection) {
        constraints.remove(connection);
    }

    /**
     * Routes the given Connection. For non-self connections between submodule
     * figures, uses the native arrowcoords() algorithm. For self-connections,
     * draws a circular arc. For connections involving compound module anchors,
     * falls back to anchor-based routing.
     * @param conn the connection to be routed
     */
    public void route(Connection conn) {
        PointList points = conn.getPoints();
        points.removeAllPoints();

        if (isSelfConnection(conn)) {
            routeSelfConnection(conn, points);
        }
        else if (isCompoundModuleAnchorConnection(conn)) {
            // connections to/from compound module border: use anchor-based routing
            routeWithAnchors(conn, points);
        }
        else {
            // submodule-to-submodule: use native arrowcoords
            routeWithArrowCoords(conn, points);
        }

        conn.setPoints(points);
    }

    protected void routeWithArrowCoords(Connection conn, PointList points) {
        IFigure srcFigure = conn.getSourceAnchor().getOwner();
        IFigure destFigure = conn.getTargetAnchor().getOwner();

        if (!(srcFigure instanceof IAnchorBounds) || !(destFigure instanceof IAnchorBounds)) {
            // fallback if figures don't provide anchor bounds
            routeWithAnchors(conn, points);
            return;
        }

        Rectangle srcBounds = ((IAnchorBounds)srcFigure).getAnchorBounds().getCopy();
        Rectangle destBounds = ((IAnchorBounds)destFigure).getAnchorBounds().getCopy();

        // translate bounds to absolute coordinates
        srcFigure.translateToAbsolute(srcBounds);
        destFigure.translateToAbsolute(destBounds);

        // read routing constraint from the router's constraint map
        RoutingConstraint rc = new RoutingConstraint();
        int bundleIndex = 0, bundleSize = 1;
        Object constraint = constraints.get(conn);
        if (constraint instanceof ConnectionRoutingConstraint) {
            ConnectionRoutingConstraint crc = (ConnectionRoutingConstraint)constraint;
            rc.setMode(crc.mode);
            rc.setSrcAnchX(crc.srcAnchX);
            rc.setSrcAnchY(crc.srcAnchY);
            rc.setDestAnchX(crc.destAnchX);
            rc.setDestAnchY(crc.destAnchY);
            bundleIndex = crc.bundleIndex;
            bundleSize = crc.bundleSize;
        }

        Rc srcRc = new Rc(srcBounds.x, srcBounds.y, 0, srcBounds.width, srcBounds.height);
        Rc destRc = new Rc(destBounds.x, destBounds.y, 0, destBounds.width, destBounds.height);

        PtVector polyline = LayoutEngine.arrowcoords(srcRc, destRc, bundleIndex, bundleSize, rc);

        for (int i = 0; i < polyline.size(); i++) {
            Pt pt = polyline.get(i);
            Point p = new Point((int)Math.round(pt.getX()), (int)Math.round(pt.getY()));
            conn.translateToRelative(p);
            points.addPoint(p);
        }
    }

    protected void routeWithAnchors(Connection conn, PointList points) {
        Point start, end;
        conn.translateToRelative(start = getStartPoint(conn));
        conn.translateToRelative(end = getEndPoint(conn));
        points.addPoint(start);
        points.addPoint(end);
    }

    protected void routeSelfConnection(Connection conn, PointList points) {
        // self connections (both src and target is the same figure.
        // draw a 3/4 circle in upper right corner for submodules
        // and a 1/4 circle for compound modules
        IAnchorBounds owner = (IAnchorBounds)conn.getSourceAnchor().getOwner(); // can be only a submodule or compound module figure
        Point center = owner.getAnchorBounds().getTopRight();
        conn.getSourceAnchor().getOwner().translateFromParent(center);

        double radius = 14;
        double delta = Math.PI*2/20;
        double angle = Math.PI/2;
        int steps = 16;
        if (owner instanceof CompoundModuleFigure) {
            // compound modules have only a 1/4 circle
            delta = -delta;
            steps = 6;
            radius += 10;
        }
        for (int i=0; i<steps; ++i, angle += delta) {
            points.addPoint((int)(0.5+center.x-Math.sin(angle)*radius),
                            (int)(0.5+center.y+Math.cos(angle)*radius));
        }
    }

    protected boolean isSelfConnection(Connection conn) {
        return conn.getSourceAnchor().getOwner() == conn.getTargetAnchor().getOwner();
    }

    protected boolean isCompoundModuleAnchorConnection(Connection conn) {
        return conn.getSourceAnchor() instanceof CompoundModuleGateAnchor
            || conn.getTargetAnchor() instanceof CompoundModuleGateAnchor;
    }

    protected Point getEndPoint(Connection conn) {
        Point ref = conn.getSourceAnchor().getReferencePoint();
        // if this is a self connection (src == target) then the connection should end at the right side of the owner figure
        // this require that we use a point to the right from the figure as a reference point
        if (isSelfConnection(conn))
            ref.x += 1000000;
        // if the source anchor is a compound module anchor use the anchor location as reference point
        if (conn.getSourceAnchor() instanceof CompoundModuleGateAnchor)
            ref = conn.getSourceAnchor().getLocation(conn.getTargetAnchor().getReferencePoint());
        return new Point((conn.getTargetAnchor().getLocation(ref)));
    }

    protected Point getStartPoint(Connection conn) {
        Point ref = conn.getTargetAnchor().getReferencePoint();
        // if this is a self connection (src == target) then the connection should start at the top side of the owner figure
        // this require that we use a point above the figure as a reference point
        if (isSelfConnection(conn))
            ref.y -= 1000000;
        // if the target anchor is a compound module anchor use the anchor location as reference point
        if (conn.getTargetAnchor() instanceof CompoundModuleGateAnchor)
            ref = conn.getTargetAnchor().getLocation(conn.getSourceAnchor().getReferencePoint());

        return new Point(conn.getSourceAnchor().getLocation(ref));
    }
}


