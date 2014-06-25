package org.omnetpp.ned.editor.graph.parts.canvas;

import org.omnetpp.figures.canvas.CanvasPiesliceFigure;
import static org.omnetpp.common.canvas.CanvasFigureConstants.*;

public class PiesliceEditPart extends AbstractCanvasShapeEditPart {

    @Override
    protected boolean representsType(String type) {
        return type.equals(FTYPE_PIESLICE);
    }

    @Override // For convenience.
    public CanvasPiesliceFigure getFigure() {
        return (CanvasPiesliceFigure)super.getFigure();
    }

    @Override
    protected CanvasPiesliceFigure createFigure() {
        return new CanvasPiesliceFigure();
    }

    @Override
    protected void refreshVisuals() {
        super.refreshVisuals();

        CanvasPiesliceFigure figure = getFigure();

        getFigure().setDefiningRectangle(parseAnchoredRectangle(PKEY_BOUNDS, PKEY_POS, PKEY_SIZE, PKEY_ANCHOR));

        Double startAngle = parseDouble(PKEY_STARTANGLE);
        figure.setStartAngle( (startAngle == null) ? 0 : startAngle );

        Double endAngle = parseDouble(PKEY_ENDANGLE);
        figure.setEndAngle( (endAngle == null) ? 360 : endAngle );
    }
}
