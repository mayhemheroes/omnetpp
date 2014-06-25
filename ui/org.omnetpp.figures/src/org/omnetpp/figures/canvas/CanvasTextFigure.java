package org.omnetpp.figures.canvas;

import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.omnetpp.common.color.ColorFactory;

public class CanvasTextFigure extends AbstractCanvasText {
    @Override
    protected boolean hitTest(Point point) {
        point.translate(anchoringRectangle.getRectangle().getLocation().getNegated());

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
        return bounds;
    }

    @Override
    public void paintFigure(Graphics graphics) {
        Point position = anchoringRectangle.getRectangle().getLocation();
        // The position of the layout is not set, because it can only
        // handle integer coordinates, so it is left at (0; 0), and translated
        // only upon drawing.
        graphics.translate((float)position.preciseX(), (float)position.preciseY());

        graphics.setForegroundColor((getLocalForegroundColor() == null) ? ColorFactory.BLACK : getLocalForegroundColor());
        graphics.setAlpha((int)Math.round(opacity * 255.0));

        graphics.drawTextLayout(layout, 0, 0);
    }
}
