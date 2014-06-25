package org.omnetpp.ned.editor.graph.parts.canvas;

import static org.omnetpp.common.canvas.CanvasFigureConstants.FTYPE_ICON;

import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.omnetpp.figures.canvas.AbstractCanvasFigure;
import org.omnetpp.figures.canvas.CanvasIconFigure;
import org.omnetpp.figures.misc.Transform;

public class IconEditPart extends ImageEditPart {

    @Override
    protected boolean representsType(String type) {
        return type.equals(FTYPE_ICON);
    }

    @Override
    protected Command getTransformedMoveCommand(Request request, Transform t) {
        IFigure parent = getFigure().getParent();
        Transform transf;
        if (parent instanceof AbstractCanvasFigure) {
            transf = ((AbstractCanvasFigure)parent).getCascadedTransform();
        } else {
            transf = new Transform();
            transf.scale((float)getScale(), (float)getScale());
        }

        return super.getTransformedMoveCommand(request, transf);
    }

    @Override
    protected IFigure createFigure() {
        return new CanvasIconFigure();
    }

}
