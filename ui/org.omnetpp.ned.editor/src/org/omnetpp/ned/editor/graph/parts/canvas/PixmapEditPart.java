package org.omnetpp.ned.editor.graph.parts.canvas;

import static org.omnetpp.common.canvas.CanvasFigureConstants.*;

import java.util.List;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.swt.graphics.Color;
import org.omnetpp.common.color.ColorFactory;
import org.omnetpp.figures.canvas.CanvasPixmapFigure;
import org.omnetpp.ned.model.ex.PropertyElementEx;

public class PixmapEditPart extends AbstractCanvasImageEditPart {

    @Override
    protected boolean representsType(String type) {
        return type.equals(FTYPE_PIXMAP);
    }

    @Override
    public CanvasPixmapFigure getFigure() {
        return (CanvasPixmapFigure) super.getFigure();
    }

    @Override
    protected void refreshVisuals() {
        super.refreshVisuals();

        PropertyElementEx model = getModel();
        if (model == null)
            return;

        CanvasPixmapFigure figure = getFigure();

        int width = 32, height = 32;
        List<String> resValues = model.getValueAsList(PKEY_RESOLUTION);
        if (resValues != null) {
            try {
                if (resValues.size() >= 1) width = Math.max(1, (int) Math.round(Double.parseDouble(resValues.get(0))));
                if (resValues.size() >= 2) height = Math.max(1, (int) Math.round(Double.parseDouble(resValues.get(1))));
            }
            catch (NumberFormatException e) {
            }
        }

        Color fillColor = parseColor(PKEY_FILLCOLOR);
        if (fillColor == null)
            fillColor = ColorFactory.GREY80;

        figure.setPixmapData(width, height, fillColor);

        List<String> boundsValues = model.getValueAsList(PKEY_BOUNDS);
        List<String> sizeValues = model.getValueAsList(PKEY_SIZE);
        if ((boundsValues == null || boundsValues.isEmpty()) && (sizeValues == null || sizeValues.isEmpty())) {
            figure.setDimension(new Dimension(width, height));
        }
    }

    @Override
    protected IFigure createFigure() {
        return new CanvasPixmapFigure();
    }

}
