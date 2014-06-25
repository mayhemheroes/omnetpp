package org.omnetpp.ned.editor.graph.parts.canvas;

import org.eclipse.draw2d.geometry.PrecisionPoint;
import org.omnetpp.figures.canvas.CanvasRectangleFigure;
import static org.omnetpp.common.canvas.CanvasFigureConstants.*;

public class RectangleEditPart extends AbstractCanvasShapeEditPart {

    @Override
    protected boolean representsType(String type) {
        return type.equals(FTYPE_RECTANGLE);
    }

    @Override // For convenience.
    public CanvasRectangleFigure getFigure() {
        return (CanvasRectangleFigure)super.getFigure();
    }

    @Override
    protected CanvasRectangleFigure createFigure() {
        return new CanvasRectangleFigure();
    }

    @Override
    protected void refreshVisuals() {
        super.refreshVisuals();

        CanvasRectangleFigure figure = getFigure();

        figure.setDefiningRectangle(parseAnchoredRectangle(PKEY_BOUNDS, PKEY_POS, PKEY_SIZE, PKEY_ANCHOR));

        PrecisionPoint cornerRadius2 = parsePoint(PKEY_CORNERRADIUS);
        if (cornerRadius2 != null) {
            figure.setCornerRadius(cornerRadius2.preciseX(), cornerRadius2.preciseY());
        } else {
            Double cornerRadius1 = parseDouble(PKEY_CORNERRADIUS);
            figure.setCornerRadius((cornerRadius1 == null) ? 0 : cornerRadius1);
        }
    }
}
