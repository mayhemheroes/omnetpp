package org.omnetpp.ned.editor.graph.parts.canvas;

import static org.omnetpp.common.canvas.CanvasFigureConstants.FTYPE_IMAGE;

import org.eclipse.draw2d.IFigure;
import org.omnetpp.figures.canvas.CanvasImageFigure;

public class ImageEditPart extends AbstractCanvasImageEditPart {

    @Override
    protected boolean representsType(String type) {
        return type.equals(FTYPE_IMAGE);
    }

    @Override
    protected IFigure createFigure() {
        return new CanvasImageFigure();
    }

}
