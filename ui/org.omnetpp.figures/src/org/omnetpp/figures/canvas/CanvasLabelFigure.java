package org.omnetpp.figures.canvas;

import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.omnetpp.common.color.ColorFactory;
import org.omnetpp.figures.misc.Transform;

public class CanvasLabelFigure extends AbstractCanvasText {

    @Override
    protected boolean hitTest(Point point) {
        point.translate(anchoringRectangle.getAnchoringOffset().getNegated());

        String text = layout.getText();

        for (int i = 0; i < text.length(); ++i) {
            if (new Rectangle(layout.getBounds(i, i)).contains(point)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Rectangle getGraphicalBounds() {
        Rectangle bounds = anchoringRectangle.getRectangle();
        bounds.translate(-anchoringRectangle.getLocation().preciseX(), -anchoringRectangle.getLocation().preciseY());
        return bounds;
    }

    @Override
    public Transform getCascadedTransform() {
        Point offset = super.getCascadedTransform().applyTo(anchoringRectangle.getLocation());
        Transform t = new Transform();
        t.translate((float)offset.preciseX(), (float)offset.preciseY());
        return t;
    }

    @Override
    public void paintFigure(Graphics graphics) {
        Point offset = anchoringRectangle.getAnchoringOffset();
        // The position of the layout is not set, because it can only
        // handle integer coordinates, so it is left at (0; 0), and translated
        // only upon drawing.
        graphics.translate((float)offset.preciseX(), (float)offset.preciseY());

        graphics.setForegroundColor((getLocalForegroundColor() == null) ? ColorFactory.BLACK : getLocalForegroundColor());
        graphics.setAlpha((int)Math.round(opacity * 255.0));

        graphics.drawTextLayout(layout, 0, 0);
    }
}
