package org.omnetpp.ned.editor.graph.parts.canvas;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PrecisionPoint;
import org.eclipse.swt.graphics.Color;
import org.omnetpp.common.color.ColorFactory;
import org.omnetpp.figures.canvas.AbstractCanvasText;
import org.omnetpp.figures.canvas.AbstractCanvasText.Alignment;
import org.omnetpp.figures.misc.AnchoredRectangle.Anchor;
import org.omnetpp.ned.editor.graph.misc.CanvasFigureUtils;

import static org.omnetpp.common.canvas.CanvasFigureConstants.*;

public abstract class AbstractCanvasTextEditPart extends AbstractCanvasFigureEditPart {

    @Override // For convenience.
    public AbstractCanvasText getFigure() {
        return (AbstractCanvasText)super.getFigure();
    }

    @Override
    protected void refreshVisuals() {
        super.refreshVisuals();

        AbstractCanvasText figure = getFigure();

        PrecisionPoint location = parsePoint(PKEY_POS);
        figure.setPosition((location != null) ? location : new PrecisionPoint(10, 10));

        String text = parseString(PKEY_TEXT);
        figure.setText((text != null) ? text : "Hello World!");

        Anchor anchor = parseAnchor(PKEY_ANCHOR);
        figure.setAnchor((anchor != null) ? anchor : Anchor.ANCHOR_NW);

        //Alignment alignment = parseAlignment(PKEY_ALIGNMENT);
        //figure.setAlignment((alignment == null) ? Alignment.ALIGN_LEFT : alignment);

        Color color = parseColor(PKEY_COLOR);
        figure.setForegroundColor((color == null) ? ColorFactory.BLACK : color);

        Double opacity = parseDouble(PKEY_OPACITY);
        figure.setOpacity((opacity == null) ? 1 : opacity);

        figure.setFontData(CanvasFigureUtils.parseFont(getModel(), PKEY_FONT));
    }

    @Override
    public void setLocation(Point loc) {
        setPoint(PKEY_POS, loc);
    }

    @Override
    public Point getLocation() {
        return parsePoint(PKEY_POS);
    }

    // Utility function for parameter parsing.

    protected Alignment parseAlignment(String key) {
        if (getModel() == null) {
            return null;
        }

        String align = getModel().getValue(key);
        if (align == null) {
            return null;
        } else {
            if (align.equals("left")) {
                return Alignment.ALIGN_LEFT;
            } else if (align.equals("center")) {
                return Alignment.ALIGN_CENTER;
            } else if (align.equals("right")) {
                return Alignment.ALIGN_RIGHT;
            } else {
                return null;
            }
        }
    }
}
