package org.omnetpp.ned.editor.graph.parts.canvas;

import org.omnetpp.figures.canvas.CanvasTextFigure;
import static org.omnetpp.common.canvas.CanvasFigureConstants.*;

public class TextEditPart extends AbstractCanvasTextEditPart {
    @Override
    protected boolean representsType(String type) {
        return type.equals(FTYPE_TEXT);
    }

    @Override
    protected CanvasTextFigure createFigure() {
        return new CanvasTextFigure();
    }
}
