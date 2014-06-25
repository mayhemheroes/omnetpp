package org.omnetpp.ned.editor.graph.parts.canvas;

import org.omnetpp.figures.canvas.AbstractCanvasLine;
import org.omnetpp.figures.canvas.AbstractCanvasLine.ArrowHead;
import org.omnetpp.figures.canvas.AbstractCanvasLine.CapStyle;
import static org.omnetpp.common.canvas.CanvasFigureConstants.*;
import org.omnetpp.ned.model.ex.PropertyElementEx;

public abstract class AbstractCanvasLineEditPart extends AbstractCanvasShapeEditPart {

    @Override // For convenience.
    public AbstractCanvasLine getFigure() {
        return (AbstractCanvasLine)super.getFigure();
    }

    @Override
    protected void refreshVisuals() {
        super.refreshVisuals();

        AbstractCanvasLine figure = getFigure();

        ArrowHead startArrowHead = parseArrowHead(PKEY_STARTARROWHEAD);
        figure.setStartArrowHead((startArrowHead == null) ? ArrowHead.ARROW_NONE : startArrowHead);

        ArrowHead endArrowHead = parseArrowHead(PKEY_ENDARROWHEAD);
        figure.setEndArrowHead((endArrowHead == null) ? ArrowHead.ARROW_NONE : endArrowHead);

        CapStyle capStyle = parseCapStyle(PKEY_CAPSTYLE);
        figure.setCapStyle((capStyle == null) ? CapStyle.CAP_BUTT : capStyle);
    };

    // Utility functions for parameter parsing.

    protected CapStyle parseCapStyle(String key) {
        PropertyElementEx model = getModel();
        if (model == null) {
            return null;
        }

        String style = model.getValue(key);

        if (style == null) {
            return null;
        } else {
            if (style.equals("butt")) {
                return CapStyle.CAP_BUTT;
            } else if (style.equals("round")) {
                return CapStyle.CAP_ROUND;
            } else if (style.equals("square")) {
                return CapStyle.CAP_SQUARE;
            } else {
                return null;
            }
        }
    }

    protected ArrowHead parseArrowHead(String key) {
        String arrowHead = parseString(key);

        if (arrowHead == null) {
            return null;
        } else {
            if (arrowHead.equals("simple")) {
                return ArrowHead.ARROW_SIMPLE;
            } else if (arrowHead.equals("triangle")) {
                return ArrowHead.ARROW_TRIANGLE;
            } else if (arrowHead.equals("barbed")) {
                return ArrowHead.ARROW_BARBED;
            } else {
                return null;
            }
        }
    }
}
