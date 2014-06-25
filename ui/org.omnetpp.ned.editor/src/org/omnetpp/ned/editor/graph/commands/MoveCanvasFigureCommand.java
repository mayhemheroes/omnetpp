package org.omnetpp.ned.editor.graph.commands;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PrecisionPoint;
import org.eclipse.gef.commands.Command;
import org.omnetpp.ned.editor.graph.misc.CanvasFigureUtils;
import org.omnetpp.ned.editor.graph.parts.canvas.AbstractCanvasFigureEditPart;

public class MoveCanvasFigureCommand extends Command {
    private AbstractCanvasFigureEditPart target;

    private Point location, oldLocation;
    private List<AbstractCanvasFigureEditPart> descendants = new ArrayList<>();
    private List<Point> descendantOldLocations = new ArrayList<>();

    public MoveCanvasFigureCommand(AbstractCanvasFigureEditPart target, Point location) {
        this.target = target;
        this.location = location;
        this.oldLocation = new PrecisionPoint(target.getLocation());

        collectDescendants(target);
        for (AbstractCanvasFigureEditPart desc : descendants)
            descendantOldLocations.add(new PrecisionPoint(desc.getLocation()));

        setLabel("Move canvas figure");
    }

    @SuppressWarnings("unchecked")
    private void collectDescendants(AbstractCanvasFigureEditPart part) {
        for (Object child : part.getChildren()) {
            if (child instanceof AbstractCanvasFigureEditPart) {
                AbstractCanvasFigureEditPart childPart = (AbstractCanvasFigureEditPart) child;
                descendants.add(childPart);
                collectDescendants(childPart);
            }
        }
    }

    public void execute() {
        double dx = location.preciseX() - oldLocation.preciseX();
        double dy = location.preciseY() - oldLocation.preciseY();

        target.setLocation(location);
        double scale = target.getScale();
        for (int i = 0; i < descendants.size(); i++) {
            Point oldLoc = descendantOldLocations.get(i);
            PrecisionPoint newLoc = new PrecisionPoint(oldLoc.preciseX() + dx, oldLoc.preciseY() + dy);
            CanvasFigureUtils.roundForZoom(newLoc, scale);
            descendants.get(i).setLocation(newLoc);
        }
    }

    public void undo() {
        target.setLocation(oldLocation);
        for (int i = 0; i < descendants.size(); i++)
            descendants.get(i).setLocation(descendantOldLocations.get(i));
    }
}
