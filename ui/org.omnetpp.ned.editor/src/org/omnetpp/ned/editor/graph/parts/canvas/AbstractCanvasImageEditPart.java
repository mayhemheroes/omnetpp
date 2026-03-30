package org.omnetpp.ned.editor.graph.parts.canvas;

import static org.omnetpp.common.canvas.CanvasFigureConstants.*;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PrecisionPoint;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.omnetpp.common.image.ImageFactory;
import org.omnetpp.common.util.Converter;
import org.omnetpp.figures.canvas.AbstractCanvasImage;
import org.omnetpp.figures.canvas.AbstractCanvasImage.Interpolation;
import org.omnetpp.figures.misc.AnchoredRectangle;
import org.omnetpp.figures.misc.AnchoredRectangle.Anchor;
import org.omnetpp.ned.model.ex.PropertyElementEx;

public abstract class AbstractCanvasImageEditPart extends AbstractCanvasFigureEditPart {
    @Override
    public boolean isResizable() {
        return true;
    }

    @Override // For convenience.
    public AbstractCanvasImage getFigure() {
        return (AbstractCanvasImage)super.getFigure();
    }

    @Override
    public Point getLocation() {
        Point pos = parsePoint(PKEY_POS);
        if (pos != null) {
            return pos;
        } else {
            AnchoredRectangle rect = parseAnchoredRectangle(PKEY_BOUNDS, PKEY_POS, PKEY_SIZE, PKEY_ANCHOR);
            return rect == null ? new PrecisionPoint() : rect.getLocation();
        }
    }

    @Override
    public void setLocation(Point loc) {
        Point pos = parsePoint(PKEY_POS);
        if (pos != null) {
            setPoint(PKEY_POS, loc);
        } else {
            AnchoredRectangle rect = parseAnchoredRectangle(PKEY_BOUNDS, PKEY_POS, PKEY_SIZE, PKEY_ANCHOR);
            if (rect == null)
                rect = new AnchoredRectangle();
            rect.setLocation(loc);
            setAnchoredRectangle(PKEY_BOUNDS, PKEY_POS, PKEY_SIZE, PKEY_ANCHOR, rect);
        }
    }

    @Override
    protected void refreshVisuals() {
        super.refreshVisuals();

        AbstractCanvasImage figure = getFigure();
        PropertyElementEx model = getModel();

        if (model == null) {
            return;
        }

        IProject project = model.getSelfOrEnclosingTypeElement().getNedTypeInfo().getProject();
        Image image = ImageFactory.of(project).getImage(getModel().getValue(PKEY_IMAGE));
        if (image == null) {
            image = ImageFactory.of(project).getImage(ImageFactory.UNKNOWN);
        }
        figure.setImage(image);

        List<String> boundsValues = model.getValueAsList(PKEY_BOUNDS);
        if (boundsValues != null && !boundsValues.isEmpty()) {
            AnchoredRectangle rect = parseAnchoredRectangle(PKEY_BOUNDS, PKEY_POS, PKEY_SIZE, PKEY_ANCHOR);
            figure.setAnchor(Anchor.ANCHOR_NW);
            figure.setLocation(rect.getLocation());
            figure.setDimension(rect.getSize());
        } else {
            figure.setLocation(parsePoint(PKEY_POS));
            Dimension size = new Rectangle(image.getBounds()).getSize();

            List<String> sizeValues = model.getValueAsList(PKEY_SIZE);

            Double width = (sizeValues != null && sizeValues.size() >= 1) ? Converter.stringToOptionalDouble(sizeValues.get(0)) : null;
            if (width != null) {
                size.setWidth((int)Math.round(width));
            }

            Double height = (sizeValues != null && sizeValues.size() >= 2) ? Converter.stringToOptionalDouble(sizeValues.get(1)) : null;
            if (height != null) {
                size.setHeight((int)Math.round(height));
            }

            Anchor anchor = parseAnchor(PKEY_ANCHOR);
            figure.setAnchor((anchor == null) ? Anchor.ANCHOR_C : anchor);

            figure.setDimension(size);
        }

        Double opacity = parseDouble(PKEY_OPACITY);
        figure.setOpacity((opacity == null) ? 1 : opacity);

        Color tintColor = parseColor(PKEY_TINT);
        if (tintColor != null) {
            figure.setTintColor(tintColor);
            figure.setTintAmount(0.5);
        } else {
            figure.setTintAmount(0);
        }

        Double tintAmount = parseDouble(PKEY_TINT, 1);
        if (tintAmount != null)
            figure.setTintAmount(tintAmount);

        String interp = parseString(PKEY_INTERPOLATION);
        figure.setImage(image);
        figure.setInterpolation(Interpolation.INTERPOLATION_FAST);
        if (interp != null) {
            if (interp.equals("none")) {
                figure.setInterpolation(Interpolation.INTERPOLATION_NONE);
            } else if (interp.equals("fast")) {
                figure.setInterpolation(Interpolation.INTERPOLATION_FAST);
            } else if (interp.equals("best")) {
                figure.setInterpolation(Interpolation.INTERPOLATION_BEST);
            }
        }
    }
}
