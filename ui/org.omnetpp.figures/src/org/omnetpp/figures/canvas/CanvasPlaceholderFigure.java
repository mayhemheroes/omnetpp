package org.omnetpp.figures.canvas;

import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.geometry.PrecisionRectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.widgets.Display;
import org.omnetpp.figures.misc.AnchoredRectangle;

/**
 * A placeholder figure for unknown/custom @figure types that are only
 * rendered at simulation runtime. Draws a semi-transparent grey rectangle
 * with a black dotted outline and a thin black diagonal cross.
 */
public class CanvasPlaceholderFigure extends AbstractCanvasShape {
    private AnchoredRectangle definingRectangle;
    private String type;

    public CanvasPlaceholderFigure() {
        definingRectangle = new AnchoredRectangle();
        updatePath();
    }

    public AnchoredRectangle getDefiningRectangle() {
        return definingRectangle;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        if (type != null && !type.equals(this.type)) {
            this.type = type;
            repaint();
        }
    }

    public void setDefiningRectangle(AnchoredRectangle rectangle) {
        if ((rectangle != null) && (!definingRectangle.equals(rectangle))) {
            erase();
            definingRectangle = new AnchoredRectangle(rectangle);
            updatePath();
            repaint();
        }
    }

    private void updatePath() {
        path = new Path(Display.getDefault());

        PrecisionRectangle r = definingRectangle.getRectangle();
        float x1 = (float)r.preciseX();
        float y1 = (float)r.preciseY();
        float w = (float)r.preciseWidth();
        float h = (float)r.preciseHeight();

        path.addRectangle(x1, y1, w, h);
    }

    @Override
    public void paintFigure(Graphics graphics) {
        PrecisionRectangle r = definingRectangle.getRectangle();
        float x1 = (float)r.preciseX();
        float y1 = (float)r.preciseY();
        float w = (float)r.preciseWidth();
        float h = (float)r.preciseHeight();
        float x2 = x1 + w;
        float y2 = y1 + h;

        float zoomedLw = 1.0f / (float)getScale();

        // semi-transparent grey fill
        graphics.setBackgroundColor(org.omnetpp.common.color.ColorFactory.GREY80);
        graphics.setAlpha(128);
        graphics.fillRectangle((int)x1, (int)y1, (int)w, (int)h);

        // black dotted outline
        graphics.setForegroundColor(org.omnetpp.common.color.ColorFactory.BLACK);
        graphics.setAlpha(255);
        graphics.setLineWidthFloat(zoomedLw);
        graphics.setLineStyle(SWT.LINE_CUSTOM);
        graphics.setLineDash(new float[] { 3 * zoomedLw, 3 * zoomedLw });
        graphics.setLineDashOffset(0);
        graphics.drawRectangle((int)x1, (int)y1, (int)w, (int)h);

        // thin black diagonal cross from corners
        graphics.setLineStyle(SWT.LINE_SOLID);
        graphics.setLineWidthFloat(zoomedLw);
        graphics.drawLine((int)x1, (int)y1, (int)x2, (int)y2);
        graphics.drawLine((int)x2, (int)y1, (int)x1, (int)y2);

        // type label at the top center
        if (type != null && !type.isEmpty()) {
            Font font = new Font(Display.getDefault(), new FontData("Arial", 9, SWT.NORMAL));
            try {
                graphics.setFont(font);
                graphics.setForegroundColor(org.omnetpp.common.color.ColorFactory.BLACK);
                graphics.setAlpha(200);
                int padding = (int)(2 / getScale());
                int textWidth = org.eclipse.draw2d.FigureUtilities.getTextWidth(type, font);
                int textX = (int)(x1 + w / 2) - textWidth / 2;
                graphics.drawText(type, textX, (int)y1 + padding);
            } finally {
                font.dispose();
            }
        }
    }
}
