package org.omnetpp.ned.editor.graph.parts.canvas;

import org.omnetpp.figures.canvas.CanvasOvalFigure;
import static org.omnetpp.common.canvas.CanvasFigureConstants.*;

public class OvalEditPart extends AbstractCanvasShapeEditPart {

    @Override
    protected boolean representsType(String type) {
        return type.equals(FTYPE_OVAL);
    }

    @Override // For convenience.
    public CanvasOvalFigure getFigure() {
        return (CanvasOvalFigure)super.getFigure();
    }

    @Override
    protected CanvasOvalFigure createFigure() {
        return new CanvasOvalFigure();
    }

    @Override
    protected void refreshVisuals() {
        super.refreshVisuals();

        getFigure().setDefiningRectangle(parseAnchoredRectangle(PKEY_BOUNDS, PKEY_POS, PKEY_SIZE, PKEY_ANCHOR));
    }
}
