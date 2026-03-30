package org.omnetpp.ned.editor.graph.commands;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PrecisionDimension;
import org.eclipse.draw2d.geometry.PrecisionPoint;
import org.eclipse.gef.commands.Command;
import org.omnetpp.ned.editor.graph.misc.CanvasFigureUtils;
import org.omnetpp.ned.editor.graph.parts.canvas.AbstractCanvasFigureEditPart;

public class ResizeCanvasFigureCommand extends Command {
    private AbstractCanvasFigureEditPart target;

    private Point location, oldLocation;
    private Dimension size, oldSize;
    private List<AbstractCanvasFigureEditPart> descendants = new ArrayList<>();
    private List<Point> descendantOldLocations = new ArrayList<>();

    public ResizeCanvasFigureCommand(AbstractCanvasFigureEditPart target, Point location, Dimension size) {
        this.target = target;
        this.location = location;
        this.size = size;
        this.oldLocation = new PrecisionPoint(target.getLocation());
        this.oldSize = new PrecisionDimension(target.getSize());

        collectDescendants(target);
        for (AbstractCanvasFigureEditPart desc : descendants)
            descendantOldLocations.add(new PrecisionPoint(desc.getLocation()));

        setLabel("Resize canvas figure");
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
        target.setSize(size);

        if (dx != 0 || dy != 0) {
            double scale = target.getScale();
            for (int i = 0; i < descendants.size(); i++) {
                Point oldLoc = descendantOldLocations.get(i);
                PrecisionPoint newLoc = new PrecisionPoint(oldLoc.preciseX() + dx, oldLoc.preciseY() + dy);
                CanvasFigureUtils.roundForZoom(newLoc, scale);
                descendants.get(i).setLocation(newLoc);
            }
        }
    }

    public void undo() {
        target.setLocation(oldLocation);
        target.setSize(oldSize);
        for (int i = 0; i < descendants.size(); i++)
            descendants.get(i).setLocation(descendantOldLocations.get(i));
    }
}
