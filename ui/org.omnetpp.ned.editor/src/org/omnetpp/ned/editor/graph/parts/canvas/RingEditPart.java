package org.omnetpp.ned.editor.graph.parts.canvas;

import org.eclipse.draw2d.geometry.Dimension;
import org.omnetpp.figures.canvas.CanvasRingFigure;
import static org.omnetpp.common.canvas.CanvasFigureConstants.*;

public class RingEditPart extends AbstractCanvasShapeEditPart {

    @Override
    protected boolean representsType(String type) {
        return type.equals(FTYPE_RING);
    }

    @Override
    public boolean isResizable() {
        return true;
    }

    @Override // For convenience.
    public CanvasRingFigure getFigure() {
        return (CanvasRingFigure)super.getFigure();
    }

    @Override
    protected CanvasRingFigure createFigure() {
        return new CanvasRingFigure();
    }

    @Override
    protected void refreshVisuals() {
        super.refreshVisuals();

        getFigure().setDefiningRectangle(parseAnchoredRectangle(PKEY_BOUNDS, PKEY_POS, PKEY_SIZE, PKEY_ANCHOR));

        Dimension innerSize = parseDimension(PKEY_INNERSIZE);
        getFigure().setInnerSize((innerSize != null) ? innerSize : new Dimension());
    }
}
