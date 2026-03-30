package org.omnetpp.ned.editor.graph.parts.canvas;

import org.omnetpp.figures.canvas.CanvasArcFigure;
import static org.omnetpp.common.canvas.CanvasFigureConstants.*;

public class ArcEditPart extends AbstractCanvasLineEditPart {

    @Override
    protected boolean representsType(String type) {
        return type.equals(FTYPE_ARC);
    }

    @Override
    public boolean isResizable() {
        return true;
    }

    @Override // For convenience.
    public CanvasArcFigure getFigure() {
        return (CanvasArcFigure)super.getFigure();
    }

    @Override
    protected CanvasArcFigure createFigure() {
        return new CanvasArcFigure();
    }

    @Override
    protected void refreshVisuals() {
        super.refreshVisuals();
        CanvasArcFigure figure = getFigure();

        getFigure().setDefiningRectangle(parseAnchoredRectangle(PKEY_BOUNDS, PKEY_POS, PKEY_SIZE, PKEY_ANCHOR));

        Double startAngle = parseDouble(PKEY_STARTANGLE);
        figure.setStartAngle( (startAngle == null) ? 0 : startAngle );

        Double endAngle = parseDouble(PKEY_ENDANGLE);
        figure.setEndAngle( (endAngle == null) ? 360 : endAngle );
    }
}
